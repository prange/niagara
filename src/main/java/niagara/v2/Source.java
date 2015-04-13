package niagara.v2;

import fj.F;
import fj.Unit;
import fj.data.Either;
import fj.function.Effect1;
import niagara.Cause;
import no.kantega.concurrent.Task;

import static niagara.v2.Utils.joinE;

public class Source<A> {


    final Effect1<F<Val<A>, Task<Unit>>> initializer;

    final Effect1<Cause> finalizer;

    public Source(Effect1<F<Val<A>, Task<Unit>>> initializer, Effect1<Cause> finalizer) {
        this.initializer = initializer;
        this.finalizer = finalizer;
    }

    //Starter
    void open(F<Val<A>, Task<Unit>> effect) {
        initializer.f( effect );
    }

    //Run effects
    public Runnable to(Sink<A> sink) {
        return () -> {
            Effect1<Cause> cleanup = cause -> {
                finalizer.f( cause );
                sink.finalizer.f( cause );
            };

            F<A, Task<Unit>> s = sink.open( cleanup );
            initializer.f( val -> val.fold( cause -> Task.call( () -> {
                cleanup.f( cause );
                return Unit.unit();
            } ), s ) );
        };
    }

    //Algebra

    public <B> Source<B> pipe(Transducer<A, B> t) {
        return Source.<B>source( sink -> {
            F<Val<A>, Task<Unit>> aSink =
                    valA -> t.lift().effect.f( valA ).flatMap( sink );
            open( aSink );
        }, finalizer );
    }

    public Source<A> append(Source<A> other) {
        return onEnd( cause -> other );
    }

    public Source<A> and(Source<A> other) {
        return Source.source( sink -> {
            open( sink );
            other.open( sink );
        }, joinE( finalizer, other.finalizer ) );
    }

    public <B> Source<Either<A, B>> or(Source<B> other) {
        return Source.source( sink -> {
            open( lefts -> sink.f( lefts.map( Either.left_() ) ) );
            other.open( rights -> sink.f( rights.map( Either.right_() ) ) );
        }, joinE( finalizer, other.finalizer ) );
    }


    public Source<A> onEnd(F<Cause, Source<A>> handler) {
        return source( sink ->
                open( val ->
                        val.fold(
                                cause -> cause.isEnd() ?
                                         Task.call( () -> {
                                             handler.f( cause ).open( sink );
                                             return Unit.unit();
                                         } ) :
                                         sink.f( val ),
                                a -> sink.f( val ) ) ), finalizer );
    }

    public Source<A> withFinalizer(Effect1<Cause> finalizer) {
        return new Source<>( initializer, joinE( finalizer, this.finalizer ) );
    }

    //Constructors
    public static <A> Source<A> source(Effect1<F<Val<A>, Task<Unit>>> initializer, Effect1<Cause> finalizer) {
        return new Source<>( initializer, finalizer );
    }

    public static <A, B> Source<B> wrapSource(Source<A> wrapped, F<Val<A>, Val<B>> f, Effect1<Cause> finalizer) {
        return source( sink -> wrapped.initializer.f( valA -> sink.f( f.f( valA ) ) ), finalizer );
    }
}
