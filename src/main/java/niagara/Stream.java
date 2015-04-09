package niagara;

import fj.F;
import fj.F2;
import fj.P;
import fj.P2;
import fj.data.List;
import fj.function.Effect1;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class Stream<A> {

    public static ScheduledExecutorService defaultScheduler = Executors.newSingleThreadScheduledExecutor();

    //Algebra

    public <B> Stream<B> map(F<A, B> f) {
        return fold(
                await -> Stream.<A, B>wrapAwait( await, inner -> inner.map( f ) ),
                emit -> emit( f.f( emit.value ), emit.next.map( f ) ),
                halt -> new Halt<B>( halt.cause )
        );
    }

    public <B> Stream<B> flatMap(F<A, Stream<B>> f) {
        return fold(
                await -> Stream.<A, B>wrapAwait( await, inner -> inner.flatMap( f ) ),
                emit -> f.f( emit.value ).append( emit.next.flatMap( f ) ),
                halt -> new Halt<B>( halt.cause )
        );
    }


    public Stream<A> append(Stream<A> other) {
        return onHalt( cause -> cause.isEnd() ? other : halt( cause ) );
    }

    public Stream<A> onHalt(F<Cause, Stream<A>> next) {
        return fold(
                await -> Stream.<A, A>wrapAwait( await, inner -> inner.onHalt( next ) ),
                emit -> emit( emit.value, emit.next.onHalt( next ) ),
                halt -> next.f( halt.cause )
        );
    }

    /**
     * Joins this stream with another stream. The resulting stream emits pairs. The value from the current emitting input
     * is paired with the last emitted input from the other. tee() is inderministic and does not zip to stream with one from each
     * input stream. The stream halts when one of the input streams halt.
     * @param other
     * @param <B>
     * @return
     */

    public <B> Stream<P2<A, B>> tee(Stream<B> other) {
        return Stream.<A,B,Stream<P2<A,B>>>fold( this, other ).fold(
                (haltA, haltB) -> halt( haltA.cause.onException( e -> true, () -> false ) ? haltA.cause: haltB.cause ),
                (haltA, emitB) -> halt(haltA.cause),
                (haltA, awaitB) -> halt(haltA.cause),
                (emitA, haltB) -> halt(haltB.cause),
                (emitA, emitB) -> emit( P.p( emitA.value, emitB.value ), emitA.next.tee( emitB.next ) ),
                (awaitA,emitA)->null,
                (emitA,awaitB)->null,
                (awaitA,awaitB)->null,
                (await,halt)->halt(halt.cause)
                );
    }

    //Catamorphism

    public abstract <B> B fold(F<Await<A>, B> onAwait, F<Emit<A>, B> onEmit, F<Halt<A>, B> onHalt);

    public static <A, B, C> FoldBoth<A, B, C> fold(Stream<A> a, Stream<B> b) {
        return (haltHalt, haltEmit, haltAwait, emitHalt, emitEmit, awaitEmit, emitAwait, awaitAwait,awaitHalt) -> a.fold(
                awaitA -> b.fold( awaitB -> awaitAwait.f( awaitA, awaitB ), emitB -> awaitEmit.f( awaitA, emitB ), haltB -> awaitHalt.f( awaitA, haltB ) ),
                emitA -> b.fold( awaitB -> emitAwait.f( emitA, awaitB ), emitB -> emitEmit.f( emitA, emitB ), haltB -> emitHalt.f( emitA, haltB ) ),
                haltA -> b.fold( awaitB -> haltAwait.f( haltA, awaitB ), emitB -> haltEmit.f( haltA, emitB ), haltB -> haltHalt.f( haltA, haltB ) )
        );
    }

    public interface FoldBoth<A, B, C> {

        C fold(
                F2<Halt<A>, Halt<B>, C> haltHalt,
                F2<Halt<A>, Emit<B>, C> haltEmit,
                F2<Halt<A>, Await<B>, C> haltAwait,
                F2<Emit<A>, Halt<B>, C> emitHalt,
                F2<Emit<A>, Emit<B>, C> emitEmit,
                F2<Await<A>, Emit<B>, C> awaitEmit,
                F2<Emit<A>, Await<B>, C> emitAwait,
                F2<Await<A>, Await<B>, C> awaitAwait,
                F2<Await<A>,Halt<B>,C> awaitHalt
        );
    }

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

    public static <A, B> Stream<B> wrapAwait(Await<A> await, F<Stream<A>, Stream<B>> f) {
        return Stream.<B>await( sink -> await.driver.f( inner -> sink.handle( f.f( inner ) ) ) );
    }

    public static <A> Stream<A> emit(A a, Stream<A> next) {
        return new Emit<>( a, next );
    }

    public static <A> Stream<A> emitOne(A a) {
        return emit( a, halt( Cause.End ) );
    }

    public static <A> Stream<A> emitAll(List<A> as) {
        return (as.isEmpty()) ? halt( Cause.End ) : emit( as.head(), emitAll( as.tail() ) );
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
