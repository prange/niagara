package niagara;

import fj.data.Either;
import fj.data.Option;
import no.kantega.concurrent.Task;

public interface Handler<A> {

    Task<Option<Cause>> handle(Either<Cause, A> value);

}
