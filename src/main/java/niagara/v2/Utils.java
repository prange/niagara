package niagara.v2;

import fj.function.Effect1;

public class Utils {

    public static <A> Effect1<A> joinE(Effect1<A> one, Effect1<A> other){
        return a ->{
            one.f(a);
            other.f( a );
        };
    }

}
