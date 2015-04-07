package niagara;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static niagara.Stream.*;

public class Streams {


    public static <A> Stream<A> repeat(A a, int times) {
        return (times == 0) ? halt( Cause.End ) : emit( a, repeat( a, times - 1 ) );
    }

    public static Stream<Instant> repeatEvery(Instant start, Duration interval, Predicate<Instant> end) {
        return (end.test( start )) ?
               halt( Cause.End ) :
               Stream.<Instant>await( sink -> {
                   Instant now = Instant.now();
                   Duration delay = Duration.between( now, start );
                   defaultScheduler.schedule(
                           () -> sink.handle( emit( start, repeatEvery( start.plus( interval ), interval, end ) ) ),
                           delay.toMillis(),
                           TimeUnit.MILLISECONDS );
               } );

    }

}
