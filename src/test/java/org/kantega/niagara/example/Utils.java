package org.kantega.niagara.example;

import fj.Unit;
import org.kantega.niagara.Task;

import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    static Task<Unit> set(AtomicLong atomicLong, long value) {
        return Task.runnableTask(() -> atomicLong.set(value));
    }

    static Task<Long> read(AtomicLong atomicLong) {
        return Task.call(atomicLong::get);
    }

    static Task<String> println(String line) {
        return Task.call(() -> {
            System.out.println(line);
            return line;
        });
    }

}
