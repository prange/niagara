package org.kantega.niagara.concurrent;

import java.util.concurrent.locks.LockSupport;

/**
 * The waitstrategy pauses the thread using different strategies.
 */
public interface WaitStrategy {

    void idle();

    /**
     * Resets the state of the waitstrategy if applicable
     */
    default void reset(){

    }

    /**
     * Does not pause the thread at all
     */
    WaitStrategy nowait = ()->{};

    /**
     * Yields the thread
     */
    WaitStrategy yieldStrategy = Thread::yield;

    /**
     * Keeps the thread in a busy spin loop.
     */
    WaitStrategy busySpinStrategy = ThreadTools::onSpinWait;

    /**
     * Parks the thread for as short amount of time as possible
     */
    WaitStrategy lockSupportStrategy = () -> LockSupport.parkNanos(1);
}
