package org.kantega.niagara.task;

import fj.Unit;

public class Console {

    public static Task<Unit> outputln(String line){
        return Task.run(()-> System.out.println(line));
    }

}
