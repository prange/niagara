package niagara.v2;

import fj.F;
import fj.function.Effect1;
import fj.function.Effect2;
import niagara.Cause;

public class Source<A> {


    final Effect1<Effect1<Val<A>>> initializer;

    final Effect1<Cause> finalizer;

    public Source(Effect1<Effect1<Val<A>>> initializer, Effect1<Cause> finalizer) {
        this.initializer = initializer;
        this.finalizer = finalizer;
    }

    //Starter
    void open(Effect1<Val<A>> sink) {
        initializer.f( sink );
    }


    //Algebra
    public Source<A> append(Source<A> other) {
        return onHalt((sink,cause)->other.open(sink));
    }

    public <B> Source<B> pipe(Transducer<A,B> t){
        return source( sink -> open( t.transform( sink ) ) );
    }

    public Runnable to(Sink<A> sink){
        return () -> {
            Effect1<Cause> cleanup = cause -> {
                finalizer.f(cause);
                sink.finalizer.f(cause);
            };
            
            Effect1<Val<A>> s = sink.open( cleanup );
            initializer.f( val -> val.onHaltElse( cleanup,s ));
        };
    }

    public Source<A> onHalt(Effect2<Effect1<Val<A>>,Cause> handler){
        return source( sink ->
                open(
                        val -> val.onHaltElse(
                                cause -> handler.f(sink,cause),
                                sink ) ) );
    }

    public Source<A> withFinalizer(Effect1<Cause> finalizer){
        return new Source<>( initializer,cause -> {
            this.finalizer.f( cause );
            finalizer.f(cause);
        } );
    }

    //Constructors
    public static <A> Source<A> source(Effect1<Effect1<Val<A>>> initializer) {
        return new Source<>( initializer, cause -> {} );
    }

    public static <A, B> Source<B> wrapSource(Source<A> wrapped, F<Val<A>, Val<B>> f) {
        return source( sink -> wrapped.initializer.f( valA -> sink.f( f.f( valA ) ) ) );
    }
}
