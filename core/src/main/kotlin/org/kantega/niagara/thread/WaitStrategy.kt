package org.kantega.niagara.thread

import java.util.concurrent.locks.LockSupport

/**
 * The waitstrategy pauses the thread using different strategies.
 */
interface WaitStrategy {

    /**
     * Invokes the waitstrategy
     */
    fun idle()

    /**
     * Resets the state of the waitstrategy if applicable
     */
    fun reset() {

    }

    companion object {

        /**
         * Does not pause the thread at all
         */
        val nowait = { }

        /**
         * Yields the thread
         */
        val yieldStrategy =  { Thread.yield() }

        /**
         * Keeps the thread in a busy spin loop.
         */
        val busySpinStrategy =  { ThreadTools.onSpinWait() }

        /**
         * Parks the thread for as short amount of time as possible
         */
        val lockSupportStrategy = { LockSupport.parkNanos(1) }
    }
}
