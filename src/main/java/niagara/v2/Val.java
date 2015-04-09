package niagara.v2;

import fj.F;
import fj.data.Either;
import fj.function.Effect1;
import niagara.Cause;

public class Val<A> {

    final Either<Cause, A> content;

    public Val(Either<Cause, A> content) {
        this.content = content;
    }


    public static <A> Val<A> value(A a) {
        return new Val<>( Either.right( a ) );
    }

    public static <A> Val<A> halt(Cause c) {
        return new Val<>( Either.left( c ) );
    }

    public void onHaltElse(Effect1<Cause> effect, Effect1<Val<A>> normal) {
        content.left().foreachDoEffect( effect );
        content.right().map( v -> this ).right().foreachDoEffect( normal );
    }

    public <B> B fold(F<Cause, B> onHalt, F<A, B> onValue) {
        return content.either( onHalt, onValue );
    }

    public <B> Val<B> map(F<A, B> f) {
        return new Val<>( content.right().map( f ) );
    }
}
