package niagara;

import fj.P2;
import fj.function.Effect1;

public class Tee<A,B> {

    final Effect1<Sink<P2<A,B>>> sink;

    public Tee(Effect1<Sink<P2<A, B>>> sink) {
        this.sink = sink;
    }

    public Effect1<Sink<A>> left(){
        return sink -> {};
    }

    public Effect1<Sink<B>> right(){
        return sink -> {};
    }



}
