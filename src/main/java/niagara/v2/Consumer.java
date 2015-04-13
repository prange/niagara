package niagara.v2;

import fj.F;
import fj.Unit;
import no.kantega.concurrent.Task;

public class Consumer<A> {

    final F<A,Task<Unit>> effect;

    public Consumer(F<A, Task<Unit>> effect) {
        this.effect = effect;
    }

    public Task<Unit> prepare(A a){
        return effect.f( a );

    }
}
