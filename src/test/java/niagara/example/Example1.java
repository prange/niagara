package niagara.example;

import fj.data.List;
import niagara.Stream;
import niagara.Streams;

import java.time.Duration;
import java.time.Instant;

public class Example1 {


    public static void main(String[] args) throws InterruptedException {
        Stream<String> s = Streams.repeat( "jalla", 5 );


        s.run( System.out::println );

        Instant end = Instant.now().plusSeconds( 3 );

        Stream<String> repeatEvery =
                Streams.repeatEvery(
                        Instant.now().plusMillis( 500 ),
                        Duration.ofMillis( 1000 ),
                        instant -> instant.isAfter( end ) )
                        .map( Instant::toString );

        repeatEvery.run( System.out::println );
        Thread.sleep( 4000 );
        Stream.defaultScheduler.shutdown();


        Stream<String> appended =
                Stream.emitAll( List.list( "a","b" ) ).append( Stream.emitAll( List.list("c","d") ) );
        appended.run( System.out::println );

        Stream<String> flatMapped =
                Stream.emitAll( List.list("a","bcd") ).flatMap( str -> Stream.emitAll( List.list(str+" "+1,str+" "+2) ) );

        flatMapped.run(System.out::println);
    }

}
