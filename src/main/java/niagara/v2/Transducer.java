package niagara.v2;

import fj.F;
import fj.P2;
import fj.Unit;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import fj.data.State;
import no.kantega.concurrent.Task;

import java.util.concurrent.atomic.AtomicReference;

import static fj.Function.andThen;
import static fj.P.p;

public class Transducer<A, B> {

    final F<A, Task<B>> effect;

    public Transducer(F<A, Task<B>> effect) {
        this.effect = effect;
    }


    public Sink<A> pipe(Sink<B> s) {
        return Sink.sink( closer -> {
            F<B, Task<Unit>> effectB = s.open( closer );
            return andThen( effect, (Task<B> taskB) -> taskB.flatMap( effectB ) );
        } ,s.finalizer);
    }



    public static <A,B> Transducer<A,B> transducer(F<A,B> f){
        return channel( (A a) -> Task.call( () -> f.f( a ) ) );
    }

    public static <A, B> Transducer<A, B> channel(F<A, Task<B>> effect) {
        return new Transducer<>( effect );
    }

    public Transducer<Val<A>,Val<B>> lift(){
        F<Val<A>, Task<Val<B>>> f2taskB =
                valA -> valA.fold( 
                        cause -> Task.now( Val.<B>halt( cause ) ), 
                        (A a) -> effect.f( a ).map( Val::value ) );
        return new Transducer<>( f2taskB );
    }

    public static <S, A, B> Transducer<A, B> mapAccum(S s, F<A, State<S, B>> state) {
        AtomicReference<P2<S, B>> ref = new AtomicReference<>( p( s, null ) );
        return transducer( a -> ref.updateAndGet( memo -> state.f( a ).run( memo._1() ) )._2() );
    }

    public static <A> Transducer<Option<A>,A> stripOption(){
        return transducer( option -> option.option( ()->Task.call ) )
    }
    
    public static <A,B> Transducer<Either<A,B>,P2<List<A>,B>> accumLeft(){
        AtomicReference<List<A>> list = new AtomicReference<>( List.nil() );
        return transducer( a ->  )
    }

}
