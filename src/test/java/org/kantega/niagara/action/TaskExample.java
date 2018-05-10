package org.kantega.niagara.action;

import org.kantega.niagara.task.Console;
import org.kantega.niagara.task.TaskRuntime;

import java.time.Duration;

import static org.kantega.niagara.task.Task.*;

public class TaskExample {

    public static void main(String[] args) {
        var unitAction =
          Console.outputln("One");

        var stringAction =
          value("string")
            .delay(Duration.ofSeconds(2));

        var integerAction =
          value(1234);

        var mapped =
          integerAction.map(String::valueOf);

        var joined =
          join(stringAction, mapped, (s1, s2) -> s1 + " " + s2);

        var printResult =
          joined.flatMap(Console::outputln);

        var rts = new TaskRuntime();
        rts.eval(fork(printResult, unitAction));
    }
}
