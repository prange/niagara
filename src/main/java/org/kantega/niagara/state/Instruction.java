package org.kantega.niagara.state;

import fj.Unit;
import fj.data.Either;
import org.kantega.niagara.PartialFunction;
import org.kantega.niagara.Source;
import org.kantega.niagara.Try;
import org.kantega.niagara.op.NoOp;
import org.kantega.niagara.op.StageOp;
import org.kantega.niagara.task.Task;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static fj.data.Either.*;

public interface Instruction<O, R> {

    Task<Either<R, Instruction<O, R>>> eval(Scope<O> outer);


    static <O, R> Instruction<O, R> pure(Task<R> value) {
        return new Pure<>(value);
    }

    static <O, R> Instruction<O, R> fail(Throwable t) {
        return pure(Task.fail(t));
    }

    static <O> Instruction<O, Unit> emit(Source<O> source) {
        return new Drain<>(source, new NoOp<>());
    }

    static <O> Instruction<O, Unit> merge(Instruction<O, ?> left, Instruction<O, ?> right) {
        return new Merge<>(left, right);
    }

    default <O2> Instruction<O2, R> transform(StageOp<O, O2> op) {
        return new Transform<>(this, op);
    }

    default <R2> Instruction<O, R2> map(Function<R, R2> function) {
        return bind(v -> v.fold(Instruction::fail, s -> pure(Task.value(function.apply(s)))));
    }

    default <R2> Instruction<O, R2> bind(Function<Try<R>, Instruction<O, R2>> function) {
        return new BindInstr<>(this, function);
    }

    default <RR> Instruction<O, RR> append(Supplier<Instruction<O, RR>> other) {
        return new Append<>(this, other);
    }

    default Instruction<O, R> repeat() {
        return append(this::repeat);
    }

    default Instruction<O, R> handle(Function<Throwable, Instruction<O, R>> handler) {
        return bind(rTry -> rTry.fold(handler::apply, r -> pure(Task.value(r))));
    }

    default <E extends Throwable> Instruction<O,R> handle(Class<E> exceptionType, Function<E,Instruction<O,R>> handler){
        return handle(PartialFunction.<Throwable,Instruction<O,R>,E>onType(exceptionType,handler).orElse(Instruction::fail));
    }

    default Instruction<O,R> delay(Duration delay){
        return new Timed<>(this,delay);
    }

    class Pure<O, R> implements Instruction<O, R> {
        final Task<R> value;

        public Pure(Task<R> value) {
            this.value = value;
        }

        @Override
        public Task<Either<R, Instruction<O, R>>> eval(Scope<O> c) {
            return value.map(Either::left);
        }
    }

    class BindInstr<O, R, R2> implements Instruction<O, R2> {

        final Instruction<O, R> instr;
        final Function<Try<R>, Instruction<O, R2>> function;

        public BindInstr(Instruction<O, R> instr, Function<Try<R>, Instruction<O, R2>> function) {
            this.instr = instr;
            this.function = function;
        }


        public Task<Either<R2, Instruction<O, R2>>> eval(Scope<O> c) {
            return instr.eval(c).map(either ->
              either.either(
                r -> right(function.apply(Try.value(r))),
                next -> right(next.bind(function))
              ));
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

    class Append<O, R> implements Instruction<O, R> {
        final Instruction<O, ?> first;
        final Supplier<Instruction<O, R>> next;

        public Append(Instruction<O, ?> first, Supplier<Instruction<O, R>> next) {
            this.first = first;
            this.next = next;
        }

        @Override
        public Task<Either<R, Instruction<O, R>>> eval(Scope<O> c) {
            return first.eval(c).map(either ->
              either.either(
                r -> right(next.get()),
                n -> right(n.append(next))
              )
            );
        }
    }


    class Merge<O> implements Instruction<O, Unit> {
        final Instruction<O, ?> first;
        final Instruction<O, ?> second;

        public Merge(Instruction<O, ?> left, Instruction<O, ?> right) {
            this.first = left;
            this.second = right;
        }


        @Override
        public Task<Either<Unit, Instruction<O, Unit>>> eval(Scope<O> c) {
            return first.eval(c).map(either ->
              either.either(
                r -> right(second.map(o -> Unit.unit())),
                cont -> right(merge(second, cont))
              )
            );
        }
    }


    class Drain<O, O2> implements Instruction<O2, Unit> {
        final Source<O> source;
        final StageOp<O, O2> stages;

        public Drain(Source<O> source, StageOp<O, O2> stages) {
            this.source = source;
            this.stages = stages;
        }

        public Task<Either<Unit, Instruction<O2, Unit>>> eval(Scope<O2> c) {
            return (rt, cont) -> {
                Source<O2> s2 = stages.apply(source);
                var running = new AtomicBoolean(true);
                var emitter = s2.build(Scope.scope(c.consumer, d -> {
                    running.set(false);
                    c.done.done(d);
                }));
                while (c.isRunning() && running.get()) {
                    emitter.emit();
                }
                cont.accept(Try.value(left(Unit.unit())));
            };

        }

        @Override
        public <O21> Instruction<O21, Unit> transform(StageOp<O2, O21> t) {
            return new Drain<>(source,stages.fuse(t));
        }

        @Override
        public String toString() {
            return "Emit{" +
              "source=" + source +
              '}';
        }
    }

    class Take<O> implements Instruction<O, Unit> {
        final Source<O> source;
        final int max;

        public Take(Source<O> source, int max) {
            this.source = source;
            this.max = max;
        }

        public Task<Either<Unit, Instruction<O, Unit>>> eval(Scope<O> c) {
            return (rt, cont) -> {
                try {
                    var done = new AtomicBoolean(false);
                    var empty = false;
                    var emitter = source.build(Scope.scope(c.consumer, d -> {
                        done.set(true);
                        c.done.done(d);
                    }));
                    var count = 0;
                    while (c.isRunning() && !done.get() && count++ < max && !empty) {
                        empty = emitter.emit();
                    }

                    if (done.get())
                        cont.accept(Try.value(left(Unit.unit())));
                    else
                        cont.accept(Try.value(right(new Take<>(source, max))));
                } catch (Throwable t) {
                    cont.accept(Try.fail(t));
                }
            };

        }


        @Override
        public String toString() {
            return "Emit{" +
              "source=" + source +
              '}';
        }
    }

    class Transform<O, O2, R> implements Instruction<O2, R> {

        final Instruction<O, R> wrapped;
        final StageOp<O, O2> op;

        public Transform(Instruction<O, R> wrapped, StageOp<O, O2> op) {
            this.wrapped = wrapped;
            this.op = op;
        }

        @Override
        public Task<Either<R, Instruction<O2, R>>> eval(Scope<O2> outer) {
            return (rt, cont) -> {
                var wrappedContext = rt.branch()._1();
                wrapped
                  .eval(op.build(outer))
                  .map(either -> either.right().map(next -> next.transform(op)))
                  .perform(wrappedContext, cont);
            };
        }

        @Override
        public <O21> Instruction<O21, R> transform(StageOp<O2, O21> t) {
            return new Transform<>(wrapped,op.fuse(t));
        }
    }

    class Timed<O,R> implements Instruction<O,R>{

        final Instruction<O,R> delayed;
        final Duration duration;

        public Timed(Instruction<O, R> delayed, Duration duration) {
            this.delayed = delayed;
            this.duration = duration;
        }

        @Override
        public Task<Either<R, Instruction<O, R>>> eval(Scope<O> outer) {
            return delayed.eval(outer).delay(duration);
        }
    }
}
