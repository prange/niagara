package org.kantega.niagara.queue;


import jdk.internal.vm.annotation.Contended;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;


public class ConcurrentQueue<E> {

    private final int      size;

    // compute the bucket position with x&(size-1)
    // aka x&mask
    final long     mask;

    // the sequence number of the end of the queue
    final LongAdder tail = new LongAdder();

    final AtomicLong tailCursor = new AtomicLong(0L);

    // use the value in the L1 cache rather than reading from memory when possible
    long p1, p2, p3, p4, p5, p6, p7;
    @Contended
    long tailCache = 0L;
    long a1, a2, a3, a4, a5, a6, a7, a8;

    // a ring buffer representing the queue
    final E[] buffer;

    long r1, r2, r3, r4, r5, r6, r7;
    @Contended
    long headCache = 0L;
    long c1, c2, c3, c4, c5, c6, c7, c8;

    // the sequence number of the start of the queue
    final LongAdder head =  new LongAdder();

    final AtomicLong headCursor = new AtomicLong(0L);

    /**
     *
     * Note: actual capacity will be the next power of two
     * larger than capacity.
     *
     * @param capacity maximum capacity of this queue
     */

    public ConcurrentQueue(final int capacity) {
        int c = 1;
        while(c < capacity) c <<=1;
        size = c;
        mask = size - 1L;
        buffer = (E[])new Object[size];
    }


    public boolean offer(E e) {


        for(;;) {
            final long tailSeq = tail.sum();
            // never offer onto the slot that is currently being polled off
            final long queueStart = tailSeq - size;

            // will this sequence exceed the capacity
            if((headCache > queueStart) || ((headCache = head.sum()) > queueStart)) {
                // does the sequence still have the expected
                // value
                if(tailCursor.compareAndSet(tailSeq, tailSeq + 1L)) {

                    try {
                        // tailSeq is valid
                        // and we got access without contention

                        // convert sequence number to slot id
                        final int tailSlot = (int)(tailSeq&mask);
                        buffer[tailSlot] = e;

                        return true;
                    } finally {
                        tail.increment();
                    }
                } // try again
            } else {
                // queue full
                return false;
            }

        }
    }


    public E poll() {
        for(;;) {
            final long head = this.head.sum();
            // is there data for us to poll
            if((tailCache > head) || (tailCache = tail.sum()) > head) {
                // check if we can update the sequence
                if(headCursor.compareAndSet(head, head+1L)) {
                    try {
                        // copy the data out of slot
                        final int pollSlot = (int)(head&mask);
                        final E   pollObj  =  buffer[pollSlot];

                        // got it, safe to read and free
                        buffer[pollSlot] = null;

                        return pollObj;
                    } finally {
                        this.head.increment();
                    }
                } // retry
            } else {
                return null;
                //queue empty
            }
        }
    }


    public final int size() {
        return (int)Math.max((tail.sum() - head.sum()), 0);
    }

    public int capacity() {
        return size;
    }

    public final boolean isEmpty() {
        return tail.sum() == head.sum();
    }


    long sumToAvoidOptimization() {
        return p1+p2+p3+p4+p5+p6+p7+a1+a2+a3+a4+a5+a6+a7+a8+r1+r2+r3+r4+r5+r6+r7+c1+c2+c3+c4+c5+c6+c7+c8+headCache+tailCache;
    }
}
