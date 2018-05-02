package org.kantega.niagara.state;

import fj.Unit;
import org.kantega.niagara.Emitter;
import org.kantega.niagara.Interrupt;
import org.kantega.niagara.Source;
import org.kantega.niagara.Try;
import org.kantega.niagara.op.NoOp;
import org.kantega.niagara.op.StageOp;
import org.kantega.niagara.sink.Sink;

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

    static <O> Instruction<O, Unit> emit(Source<O> source) {
        return new Compile<>(source, new NoOp<>());
    }


    default <O2> Instruction<O2, Unit> transform(StageOp<O, O2> op) {
        return new EncloseCompile<>(this.map(__ -> Unit.unit()), op);
    }

    default <R2> Instruction<O, R2> map(Function<R, R2> function) {
        return bind(v -> v.fold(Instruction::fail, s -> pure(function.apply(s))));
    }

    default <R2> Instruction<O, R2> bind(Function<Try<R>, Instruction<O, R2>> function) {
        return new BindInstr<>(this, function);
    }

    default Instruction<O, Unit> append(Supplier<Instruction<O, Unit>> other) {
        return new Append<>(this.map(__->Unit.unit()), other);
    }

    default Instruction<O, Unit> repeat() {
        return new Interruptable<>(append(this::repeat));
    }

    default Instruction<O, R> handle(Function<Throwable, Instruction<O, R>> handler) {
        return bind(rTry -> rTry.fold(handler::apply, Instruction::<O, R>pure));
    }

    static <O> Instruction<O, Unit> join(Instruction<O, Unit> left, Instruction<O, Unit> right) {
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

    class BindInstr<O, R, R2> implements Instruction<O, R2> {

        final Instruction<O, R> instr;
        final Function<Try<R>, Instruction<O, R2>> function;

        public BindInstr(Instruction<O, R> instr, Function<Try<R>, Instruction<O, R2>> function) {
            this.instr = instr;
            this.function = function;
        }


        public Step<R2> eval(Scope<O> ctx) {
            return instr.eval(ctx).bindTry(rTry -> function.apply(rTry).eval(ctx));
        }

        @Override
        public <R21> Instruction<O, R21> bind(Function<Try<R2>, Instruction<O, R21>> f) {
            return instr.bind(rTry ->
              function.apply(rTry).bind(f));
        }

        @Override
        public String toString() {
            return "BindInstr{" +
              "instr=" + instr +
              ", function=" + function +
              '}';
        }
    }

    class Append<O> implements Instruction<O, Unit> {
        final Instruction<O, Unit> first;
        final Supplier<Instruction<O, Unit>> next;

        public Append(Instruction<O, Unit> first, Supplier<Instruction<O, Unit>> next) {
            this.first = first;
            this.next = next;
        }

        @Override
        public Step<Unit> eval(Scope<O> scope) {
            return
              first
                .eval(scope)
                .bind(__ ->
                  next.get().eval(scope.reset()));
        }
    }

    class Interruptable<O> implements Instruction<O, Unit> {
        final Instruction<O, Unit> instruction;

        public Interruptable(Instruction<O, Unit> instruction) {
            this.instruction = instruction;
        }

        @Override
        public Step<Unit> eval(Scope<O> scope) {
            return new Step.InterruptingStep<>(scope,instruction.eval(InnerScope.wrap(scope)));
        }
    }

    class Join<O> implements Instruction<O, Unit> {
        final Instruction<O, Unit> first;
        final Instruction<O, Unit> second;

        public Join(Instruction<O, Unit> left, Instruction<O, Unit> right) {
            this.first = left;
            this.second = right;
        }


        @Override
        public Step<Unit> eval(Scope<O> scope) {
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
            return new Compile<>(source, stages.fuse(op));
        }

        @Override
        public String toString() {
            return "Compile{" +
              "source=" + source +
              ", stages=" + stages +
              '}';
        }
    }

    class EncloseCompile<O, O2> implements Instruction<O2, Unit> {

        final Instruction<O, Unit> inner;
        final StageOp<O, O2> stages;

        public EncloseCompile(Instruction<O, Unit> inner, StageOp<O, O2> stages) {
            this.inner = inner;
            this.stages = stages;
        }

        @Override
        public Step<Unit> eval(Scope<O2> outerScope) {
            return new Step.InterruptingStep<>(outerScope, inner.eval(new InnerScope<>(stages, outerScope)));
        }

        @Override
        public <O21> Instruction<O21, Unit> transform(StageOp<O2, O21> op) {
            return new EncloseCompile<>(inner, stages.fuse(op));
        }

        @Override
        public String toString() {
            return "EncloseCompile{" +
              "inner=" + inner +
              ", stages=" + stages +
              '}';
        }
    }

    class Emit<O> implements Instruction<O, Unit> {
        final Source<O> source;

        public Emit(Source<O> source) {
            this.source = source;
        }

        public Step<Unit> eval(Scope<O> loop) {
            return Step.done(source.build(loop.sink()))
              .bind(emitter -> new Step.EmittingStep<>(emitter, loop));

        }


        @Override
        public String toString() {
            return "Emit{" +
              "source=" + source +
              '}';
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
