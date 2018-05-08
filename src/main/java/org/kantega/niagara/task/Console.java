package org.kantega.niagara.task;

import fj.Unit;

public class Console {

    public static Action<Unit> prinln(String line){
        return Action.run(()-> System.out.println(line));
    }

}
