package niagara;

import fj.F;
import fj.function.Effect1;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class Stream<A> {

    public static ScheduledExecutorService defaultScheduler = Executors.newSingleThreadScheduledExecutor();

    //Algebra

    public <B> Stream<B> map(F<A, B> f) {
        return fold(
                await -> Stream.<B>await( sink -> await.driver.f( inner -> sink.handle( inner.map( f ) ) ) ),
                emit -> emit( f.f( emit.value ), emit.next.map( f ) ),
                halt -> new Halt<B>( halt.cause )
        );
    }


    //Catamorphism

    public abstract <B> B fold(F<Await<A>, B> onAwait, F<Emit<A>, B> onEmit, F<Halt<A>, B> onHalt);

    //Interpreters

    public void run(Effect1<A> collector) {
        step( collector );
    }

    private Stream<A> step(Effect1<A> sink) {
        Stream<A> next = goStep( this, sink );
        while (next instanceof Emit) {
            next = goStep( next, sink );
        }
        return next;
    }


    private static <A> Stream<A> goStep(Stream<A> stream, Effect1<A> sink) {
        return stream.<Stream<A>>fold(
                await -> {
                    await.driver.f( s -> s.step( sink ) );
                    return await;
                },
                emit -> {
                    sink.f( emit.value );
                    if (emit.next instanceof Await)
                        return goStep( emit.next, sink );
                    else
                        return emit.next;

                },
                halt -> halt );
    }


    //Constructors

    public static <A> Stream<A> await(Effect1<Sink<A>> driver) {
        return new Await<>( driver );
    }

    public static <A> Stream<A> emit(A a, Stream<A> next) {
        return new Emit<>( a, next );
    }

    public static <A> Halt<A> halt(Cause cause) {
        return new Halt<>( cause );
    }


    //States

    public static class Await<A> extends Stream<A> {

        final Effect1<Sink<A>> driver;

        public Await(Effect1<Sink<A>> driver) {
            this.driver = driver;
        }


        @Override public <B> B fold(F<Await<A>, B> onAwait, F<Emit<A>, B> onEmit, F<Halt<A>, B> onHalt) {
            return onAwait.f( this );
        }
    }

    public static class Emit<A> extends Stream<A> {

        final A value;


        final Stream<A> next;

        public Emit(A value, Stream<A> next) {
            this.value = value;
            this.next = next;
        }

        @Override public <B> B fold(F<Await<A>, B> onAwait, F<Emit<A>, B> onEmit, F<Halt<A>, B> onHalt) {
            return onEmit.f( this );
        }
    }

    public static class Halt<A> extends Stream<A> {

        public final Cause cause;

        public Halt(Cause cause) {
            this.cause = cause;
        }

        @Override public <B> B fold(F<Await<A>, B> onAwait, F<Emit<A>, B> onEmit, F<Halt<A>, B> onHalt) {
            return onHalt.f( this );
        }
    }

}
