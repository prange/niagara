package niagara.example;

import java.time.Instant;

public class StreamTest {


    public static void main(String[] args) {
        java.util.stream.Stream<Integer> x = null;
        java.util.stream.Stream<Integer> y = null;

        java.util.stream.Stream.generate( () -> Instant.now() ).parallel().forEach( instant -> {
            System.out.println( Thread.currentThread().getName() + " - " + instant.toString() );
        } );
    }
}
