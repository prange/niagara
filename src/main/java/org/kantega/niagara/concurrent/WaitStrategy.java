package org.kantega.niagara.concurrent;

import java.util.concurrent.locks.LockSupport;

public interface WaitStrategy {

    void idle();

    WaitStrategy hog = ()->{};
    WaitStrategy yieldStrategy = Thread::yield;
    WaitStrategy busySpinStrategy = ThreadTools::onSpinWait;
    WaitStrategy lockSupportStrategy = () -> LockSupport.parkNanos(1);
}
