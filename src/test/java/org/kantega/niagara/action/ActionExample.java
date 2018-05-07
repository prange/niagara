package org.kantega.niagara.action;

import fj.Unit;
import org.kantega.niagara.task.Action;
import org.kantega.niagara.task.RTS;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ActionExample {


    public static void main(String[] args) {
        var unitAction = Action.run(new Println(Duration.ofSeconds(3), "One"));
        var integerAction = Action.value(1234);
        var mapped = integerAction.map(String::valueOf);
        var printResult =
          mapped.bind(new PrintlnCont(Duration.ofSeconds(1)));

        var rts = new RTS();
        rts.runAction(unitAction);
        rts.runAction(printResult);

    }

    static class PrintlnCont implements Function<String, Action<Unit>> {

        final Duration delay;

        PrintlnCont(Duration delay) {
            this.delay = delay;
        }

        @Override
        public Action<Unit> apply(String s) {

            return Action.run(new Println(delay, s));
        }
    }

    static class Println implements Runnable {

        final Duration delay;
        public final String value;

        Println(Duration delay, String value) {
            this.delay = delay;
            this.value = value;
        }

        @Override
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(delay.toSeconds());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(value);
        }
    }
}
