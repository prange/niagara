package org.kantega.niagara.state;

import fj.Unit;
import org.kantega.niagara.Emitter;
import org.kantega.niagara.Interrupt;
import org.kantega.niagara.Source;
import org.kantega.niagara.Try;
import org.kantega.niagara.op.StageOp;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Instruction<O, R> {

    Step<R> eval(Scope<O> scope);


    static <O, R> Instruction<O, R> pure(R value) {
        return new Pure<>(value);
    }

    static <O, R> Instruction<O, R> aquire(Supplier<R> resource) {
        return new Aquire<>(resource);
    }

    static <O, R> Instruction<O, Unit> release(R r, Consumer<R> cleanup) {
        return new Release<>(r, cleanup);
    }


    static <O, R> Instruction<O, R> fail(Throwable t) {
        return new Fail<>(t);
    }

    static <O, R> Instruction<O, Unit> emit(Source<O> source) {
        return new Emit<>(source);
    }

    private <O2> Instruction<O2, R> get() {
        return (Instruction<O2, R>) this;
    }

    default <O2> Instruction<O2, Unit> transform(StageOp<O, O2> op) {
        return scope.map(sb -> sb.append(op)).get();
    }

    default <R2> Instruction<O, R2> map(Function<R, R2> function) {
        return bind(v -> v.fold(Instruction::fail, s -> pure(function.apply(s))));
    }

    default <R2> Instruction<O, R2> bind(Function<Try<R>, Instruction<O, R2>> function) {
        return new BindScope<>(this, function);
    }

    default Instruction<O, R> append(Supplier<Instruction<O, R>> other) {
        return bind(__ -> other.get());
    }

    default Instruction<O, R> repeat() {
        return append(this::repeat);
    }

    default Instruction<O, R> handle(Function<Throwable, Instruction<O, R>> handler) {
        return bind(rTry -> rTry.fold(handler::apply, Instruction::<O, R>pure));
    }

    static <O> Instruction<O, Source<O>> join(Instruction<O, ?> left, Instruction<O, ?> right) {
        return new Join<>(left, right);
    }


    class Pure<O, R> implements Instruction<O, R> {
        final R value;

        public Pure(R value) {
            this.value = value;
        }

        @Override
        public Step<R> eval(Scope<O> scope) {
            return Step.done(value);
        }
    }

    class BindScope<O, R, R2> implements Instruction<O, R2> {

        final Instruction<O, R> instr;
        final Function<Try<R>, Instruction<O, R2>> function;

        public BindScope(Instruction<O, R> instr, Function<Try<R>, Instruction<O, R2>> function) {
            this.instr = instr;
            this.function = function;
        }


        public Step<R2> eval(Scope<O> ctx) {
            return function.apply(instr.eval(ctx).get()).eval(ctx);
        }

        @Override
        public <R21> Instruction<O, R21> bind(Function<Try<R2>, Instruction<O, R21>> f) {
            return instr.bind(rTry -> function.apply(rTry).bind(f));
        }
    }


    class Join<O> implements Instruction<O, Source<O>> {
        final Instruction<O, ?> first;
        final Instruction<O, ?> second;

        public Join(Instruction<O, ?> left, Instruction<O, ?> right) {
            this.first = left;
            this.second = right;
        }


        @Override
        public Step<Source<O>> eval(Scope<O> scope) {
            return null;
        }
    }


    class Fail<O, R> implements Instruction<O, R> {

        final Throwable t;

        public Fail(Throwable t) {
            this.t = t;
        }


        @Override
        public Step<R> eval(Scope<O> scope) {
            return Step.fail(t);
        }
    }

    class Compile<O, O2> implements Instruction<O2, Unit> {
        final Source<O> source;
        final StageOp<O, O2> stages;

        public Compile(Source<O> source, StageOp<O, O2> stages) {
            this.source = source;
            this.stages = stages;
        }

        @Override
        public Step<Unit> eval(Scope<O2> scope) {
            return Step.cont(() -> new Emit<>(stages.apply(source)).eval(scope));
        }

        @Override
        public <O21> Instruction<O21, Unit> transform(StageOp<O2, O21> op) {
            return new Compile<>(source,stages.fuse(op));
        }
    }

    class EncloseCompile<O,O2> implements Instruction<O2,Unit>{

        final Instruction<O,?> inner;
        final StageOp<O,O2> stages;

        public EncloseCompile(Instruction<O, ?> inner, StageOp<O, O2> stages) {
            this.inner = inner;
            this.stages = stages;
        }

        @Override
        public Step<Unit> eval(Scope<O2> scope) {
            return inner.eval();
        }

        @Override
        public <O21> Instruction<O21, Unit> transform(StageOp<O2, O21> op) {
            return new EncloseCompile<>(inner,stages.fuse(op));
        }
    }

    class Emit<O> implements Instruction<O, Unit> {
        final Source<O> source;

        public Emit(Source<O> source) {
            this.source = source;
        }

        public Step<Unit> eval(Scope<O> loop) {
            return Step.cont(() -> {
                var cf = new CompletableFuture<Boolean>();
                Emitter emitter = source.build(loop.sink(), r -> {
                    if (r.isNil())
                        cf.complete(true);
                });
                return new EmittingStep<>(emitter, new Interrupt(cf), loop);
            });
        }

        static class EmittingStep<O> implements Step<Unit> {

            final Interrupt interrupt;
            final Emitter emitter;
            final Scope<O> loop;
            boolean isWaiting = false;

            EmittingStep(Emitter emitter, Interrupt interrupt, Scope<O> loop) {
                this.interrupt = interrupt;
                this.emitter = emitter;
                this.loop = loop;
            }

            //This is the hotspot, here 99% of all work will be done.
            @Override
            public Step<Unit> step() {
                if (!complete())
                    try {
                        if (emitter.emit()) {
                            if (isWaiting)
                                return loop.resetWait(() -> this);
                            else
                                return this;
                        } else {
                            isWaiting = true;
                            return loop.wait(() -> this);
                        }
                    } catch (Throwable t) {
                        return Step.fail(t);
                    }
                return Step.done(Unit.unit());
            }

            @Override
            public boolean complete() {
                return interrupt.isInterrupted();
            }

            @Override
            public Try<Unit> get() {
                return Step.trampoline(this);
            }
        }
    }


    class Aquire<O, R> implements Instruction<O, R> {

        final Supplier<R> resource;

        public Aquire(Supplier<R> resource) {
            this.resource = resource;
        }


        @Override
        public Step<R> eval(Scope<O> scope) {
            return Step.trycatch(resource);
        }
    }

    class Release<O, R> implements Instruction<O, Unit> {

        final R resource;
        final Consumer<R> cleanup;

        public Release(R resource, Consumer<R> cleanup) {
            this.resource = resource;
            this.cleanup = cleanup;
        }

        @Override
        public Step<Unit> eval(Scope<O> scope) {
            return Step.cont(() -> {
                cleanup.accept(resource);
                return Step.done(Unit.unit());
            });
        }
    }


}
