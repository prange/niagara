package org.kantega.niagara.state;

import fj.Unit;
import org.kantega.niagara.Emitter;
import org.kantega.niagara.Source;
import org.kantega.niagara.Try;
import org.kantega.niagara.op.StageOp;
import org.kantega.niagara.sink.Sink;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

    static <O, R> Instruction<O, Optional<Emit<O, R>>> emit(Source<O> source) {
        return new Emit<>(source);
    }

    static <O, O2> Instruction<O2, Source<O2>> transform(Instruction<O, Source<O>> scope, StageOp<O, O2> op) {
        return scope.map(sb -> sb.append(op)).get();
    }

    private <O2> Instruction<O2, R> get() {
        return (Instruction<O2, R>) this;
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
        return this;
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
            return function.apply(Try.value(instr.eval(ctx).get())).eval(ctx);
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
            return null;
        }
    }

    class Emit<O, R> implements Instruction<O, Optional<Emit<O, R>>> {
        final Source<O> source;

        public Emit(Source<O> source) {
            this.source = source;
        }

        public Step<Optional<Emit<O, R>>> eval(Scope<O> loop) {
            return Step.cont(() -> {
                var cf = new CompletableFuture<Optional<Emit<O, R>>>();
                Emitter emitter = source.build(loop.sink(), r -> {
                    if (r.isNil())
                        cf.complete(Optional.empty());
                    else
                        cf.complete(Optional.of(new Emit<>(r)));
                });
                return new EmittingStep<>(emitter, cf);
            });
        }

        static class EmittingStep<O, R> implements Step<Optional<Emit<O, R>>> {

            final CompletableFuture<Optional<Emit<O, R>>> cf;
            final Emitter emitter;

            EmittingStep(Emitter emitter, CompletableFuture<Optional<Emit<O, R>>> cf) {
                this.cf = cf;
                this.emitter = emitter;
            }

            @Override
            public Step<Optional<Emit<O, R>>> step() {
                if (!complete())
                    emitter.emit();//TODO Must check for wait
                return Step.done(get());
            }

            @Override
            public Optional<Emit<O, R>> get() {
                try {
                    return cf.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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
            return Step.cont(() -> Step.done(resource.get()));
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

    class Pause<O, R> implements Instruction<O, R> {

        final Instruction<O, R> cont;

        public Pause(Instruction<O, R> cont) {
            this.cont = cont;
        }


        @Override
        public Step<R> eval(Scope<O> scope) {
            return scope.wait(() -> cont.eval(scope));
        }
    }


}
