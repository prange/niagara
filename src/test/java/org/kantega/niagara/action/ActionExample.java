package org.kantega.niagara.action;

import org.kantega.niagara.task.Action;
import org.kantega.niagara.task.Console;
import org.kantega.niagara.task.RTS;

public class ActionExample {
    public static void main(String[] args) {
        var unitAction = Console.prinln("One");
        var integerAction = Action.value(1234);
        var mapped = integerAction.map(String::valueOf);
        var printResult =
          mapped.flatMap(Console::prinln);

        var rts = new RTS();
        rts.runAction(unitAction);
        rts.runAction(printResult);
    }
}
