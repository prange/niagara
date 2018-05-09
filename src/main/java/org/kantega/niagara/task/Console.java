package org.kantega.niagara.task;

import fj.Unit;

public class Console {

    public static Task<Unit> prinln(String line){
        return Task.run(()-> System.out.println(line));
    }

}
