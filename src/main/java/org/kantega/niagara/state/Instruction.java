package org.kantega.niagara.state;

import fj.Unit;
import org.kantega.niagara.Emitter;
import org.kantega.niagara.Source;
import org.kantega.niagara.Try;
import org.kantega.niagara.op.StageOp;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Instruction<O, R> {

    Stream<O,R> build(Scope<O> scope);


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

    static <O> Instruction<O, ?> source(Source<O> source) {
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
        public Stream<O, R> build(Scope<O> loop) {
            return next -> () -> next.accept(Try.value(value));
        }
    }

    class BindScope<O, R, R2> implements Instruction<O, R2> {

        final Instruction<O, R> scope;
        final Function<Try<R>, Instruction<O, R2>> function;

        public BindScope(Instruction<O, R> scope, Function<Try<R>, Instruction<O, R2>> function) {
            this.scope = scope;
            this.function = function;
        }


        @Override
        public Stream<O, R2> build(Scope<O> loop) {
            return next -> scope.build(loop).stepper(r->loop.setNext(function.apply(r)));
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
        public Stream<O, Source<O>> build(Scope<O> scope) {
            return null;
        }
    }


    class Fail<O, R> implements Instruction<O, R> {

        final Throwable t;

        public Fail(Throwable t) {
            this.t = t;
        }


        @Override
        public Stream<O, R> build(Scope<O> loop) {
            return next -> () -> next.accept(Try.fail(t));
        }
    }

    class Emit<O> implements Instruction<O, Optional<Emit<O>>> {
        final Source<O> source;

        public Emit(Source<O> source) {
            this.source = source;
        }

        public Stream<O, Optional<Emit<O>>> build(Scope<O> loop) {
            return next -> {
                Emitter emitter = source.build(loop.sink, r -> {
                    if (r.isNil())
                        next.accept(Try.value(Optional.empty()));
                    else
                        next.accept(Try.value(Optional.of(new Emit<O>(r))));
                });
                return emitter::emit;
            };
        }

    }




    class Aquire<O, R> implements Instruction<O, R> {

        final Supplier<R> resource;

        public Aquire(Supplier<R> resource) {
            this.resource = resource;
        }


        @Override
        public Stream<O, R> build(Scope<O> loop) {
            return next -> () -> next.accept(Try.call(resource));
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
        public Stream<O, Unit> build(Scope<O> loop) {
            return next -> () -> next.accept(Try.call(()->cleanup.accept(resource)));
        }
    }

    class Pause<O, R> implements Instruction<O, Instruction<O,R>> {

        final Instruction<O, R> cont;

        public Pause(Instruction<O, R> cont) {
            this.cont = cont;
        }

        @Override
        public Stream<O, Instruction<O,R>> build(Scope<O> loop) {
            return next -> () ->{
                loop.waitStrategy.idle();
                next.accept(Try.value(cont));
            };
        }
    }


}
