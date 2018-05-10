package org.kantega.niagara.action;

import org.kantega.niagara.Eval;
import org.kantega.niagara.task.Console;
import org.kantega.niagara.task.Task;
import org.kantega.niagara.task.TaskRuntime;

import java.time.Duration;

import static org.kantega.niagara.task.Task.*;

public class TaskExample2 {

    public static void main(String[] args) {

        var stringAction =
          value("string");

        var cleanup =
          stringAction
            .onFinish(Console.outputln("Cleaning up"));

        var failingAction =
          evaluate(failingEval)
            .delay(Duration.ofSeconds(2))
            .onFinish(Console.outputln("Cleanup 2"));

        var joined =
          join(cleanup, failingAction, (s1, s2) -> s1 + " " + s2);

        var caught =
          joined.handle(t ->Task.value(t.getMessage()));

        var printResult =
          caught.flatMap(Console::outputln);


        var rts = new TaskRuntime();
        rts.eval(printResult);
    }


    static Eval<String> failingEval =
      Eval.fail(new NullPointerException("Failed"));
}
