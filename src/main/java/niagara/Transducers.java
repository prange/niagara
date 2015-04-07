package niagara;

import fj.data.Option;

public class Transducers {

    public static <A> Transducer<Option<A>,A> stripOption(){
        return new Transducer<>( option -> option.toList() );
    }

}
