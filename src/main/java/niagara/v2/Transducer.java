package niagara.v2;

import fj.F;
import fj.P2;
import fj.data.State;
import fj.function.Effect1;
import niagara.Cause;
import no.kantega.concurrent.Task;

import java.util.concurrent.atomic.AtomicReference;

import static fj.P.p;

public class Transducer<A, B> {

    final F<Effect1<Val<B>>, Effect1<Val<A>>> sinkTransformer;

    public Transducer(F<Effect1<Val<B>>, Effect1<Val<A>>> sinkTransformer) {
        this.sinkTransformer = sinkTransformer;
    }

    Effect1<Val<A>> transform(Effect1<Val<B>> sink) {
        return sinkTransformer.f( sink );
    }


    public Sink<A> pipe(Sink<B> s) {
        return Sink.sink( closer -> sinkTransformer.f( s.open( closer ) ) );
    }


    public static <A, B> Transducer<A, B> channel(F<A, Task<B>> f) {
        return transducer( (Effect1<Val<B>> sinkB) -> ((Val<A> valA) -> {
            valA.<Task<Val<B>>>fold(
                    cause -> Task.now( Val.halt( cause ) ),
                    a -> f.f( a ).map( Val::value ) )
                    .execute( tried ->
                            sinkB.f( tried.fold(
                                    t -> Val.<B>halt( Cause.toError.f( t ) ),
                                    b -> b ) ) );
        }) );
    }

    public static <S, A, B> Transducer<A, B> mapAccum(S s, F<A, State<S, B>> state) {
        AtomicReference<P2<S, B>> ref = new AtomicReference<>( p( s, null ) );

        return transducer( sinkB -> (valA -> valA.map( a -> ref.updateAndGet( pair -> state.f( a ).run( pair._1() ) )._2() )) );
    }

    public static <A, B> Transducer<A, B> transducer(F<Effect1<Val<B>>, Effect1<Val<A>>> sinkTransformer) {
        return new Transducer<>( sinkTransformer );
    }
}
