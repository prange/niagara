package niagara;

import fj.F;

import java.util.function.Supplier;

public abstract class Cause {

    public static End End = new End();
    public static Kill Kill = new Kill();
    public static final F<Throwable,Cause> toError = Failure::new;


    public abstract boolean isEnd();

    public abstract boolean isKill();

    public abstract <A> A onException(F<Throwable,A> f, Supplier<A> defaultValue);



    public static class End extends Cause{
        @Override public boolean isEnd() {
            return true;
        }

        @Override public boolean isKill() {
            return false;
        }

        @Override public <A> A onException(F<Throwable, A> f, Supplier<A> defaultValue) {
            return defaultValue.get();
        }
    }

    public static class Kill extends Cause{
        @Override public boolean isEnd() {
            return false;
        }

        @Override public boolean isKill() {
            return true;
        }

        @Override public <A> A onException(F<Throwable, A> f, Supplier<A> defaultValue) {
            return defaultValue.get();
        }
    }

    public static class Failure extends Cause{

        public final Throwable t;

        public Failure(Throwable t) {
            this.t = t;
        }

        @Override public boolean isEnd() {
            return false;
        }

        @Override public boolean isKill() {
            return false;
        }

        @Override public <A> A onException(F<Throwable, A> f, Supplier<A> defaultValue) {
            return f.f(t);
        }
    }

}
