package niagara;

import fj.F;
import fj.data.List;

public class Transducer<I,O> {

    final F<I,List<O>> f;

    public Transducer(F<I, List<O>> f) {
        this.f = f;
    }

    public <C> Transducer<I,C> pipe(Transducer<O,C> t2){
        return new Transducer<>( i -> f.f(i).bind(t2.f) );
    }
}
