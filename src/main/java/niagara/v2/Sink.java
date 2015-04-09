package niagara.v2;

import fj.F;
import fj.function.Effect1;
import niagara.Cause;

public class Sink<A> {

    final F<Effect1<Cause>,Effect1<Val<A>>> initializer;
    final Effect1<Cause> finalizer;

    public Sink(F<Effect1<Cause>, Effect1<Val<A>>> initializer, Effect1<Cause> finalizer) {
        this.initializer = initializer;
        this.finalizer = finalizer;
    }

    public Effect1<Val<A>> open(Effect1<Cause> closer){
        return initializer.f(closer);
    }

    public static <A> Sink<A> sink(F<Effect1<Cause>,Effect1<Val<A>>> initializer){
        return new Sink<>( initializer, cause -> {} );
    }

}
