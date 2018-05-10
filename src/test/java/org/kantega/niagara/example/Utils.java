package org.kantega.niagara.example;

import fj.Unit;
import org.kantega.niagara.task.Task;

import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    static Task<Unit> set(AtomicLong atomicLong, long value) {
        return Task.run(() -> atomicLong.set(value));
    }

    static Task<Long> read(AtomicLong atomicLong) {
        return Task.get(atomicLong::get);
    }

    static Task<String> println(String line) {
        return Task.get(() -> {
            System.out.println(line);
            return line;
        });
    }

}
