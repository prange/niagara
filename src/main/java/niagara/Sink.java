package niagara;

public interface Sink<A> {

    void handle(Stream<A> next);

}
