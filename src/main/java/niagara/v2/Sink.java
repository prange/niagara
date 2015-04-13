package niagara.v2;

import fj.F;
import fj.Unit;
import fj.data.Either;
import fj.function.Effect1;
import niagara.Cause;
import no.kantega.concurrent.Task;

import static niagara.v2.Utils.*;

public class Sink<A> {

    final F<Effect1<Cause>, F<A, Task<Unit>>> initializer;

    final Effect1<Cause> finalizer;

    public Sink(F<Effect1<Cause>, F<A, Task<Unit>>> initializer, Effect1<Cause> finalizer) {
        this.initializer = initializer;
        this.finalizer = finalizer;
    }

    //sinks
    public static <A> Sink<A> fromEffect(Effect1<A> effect1) {
        return sink( closer -> ((A a) -> Task.call( () -> {
            effect1.f( a );
            return Unit.unit();
        } )), cause -> {
        } );
    }

    public F<A, Task<Unit>> open(Effect1<Cause> closer) {
        return initializer.f( closer );
    }

    public static <A> Sink<A> sink(F<Effect1<Cause>, F<A, Task<Unit>>> initializer, Effect1<Cause> finalizer) {
        return new Sink<>( initializer, finalizer );
    }

    public Sink<A> withFinalizer(Effect1<Cause> finalizer) {
        return new Sink<>( initializer, joinE( finalizer, this.finalizer ) );
    }

    public <B> Sink<Either<A, B>> or(Sink<B> other) {
        return sink(
                closer -> either -> either.either( open( closer ), other.open( closer ) ),
                joinE( finalizer, other.finalizer ) );
    }

}
