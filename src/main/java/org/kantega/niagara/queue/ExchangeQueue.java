package org.kantega.niagara.queue;

import java.util.concurrent.atomic.LongAdder;

@SuppressWarnings({"WeakerAccess", "unchecked"})
public class ExchangeQueue<E> {
    final int size;

    final long mask;

    final LongAdder tail = new LongAdder();

    //   pad to prevent false sharing
    long p1, p2, p3, p4, p5, p6, p7;

    //Caching tail to prevent unessecary access to tail longadder.
    @jdk.internal.vm.annotation.Contended
    long tailCache = 0L;
    long a1, a2, a3, a4, a5, a6, a7, a8;

    final E[] buffer;

    long r1, r2, r3, r4, r5, r6, r7;

    //Caching head to prevent unessecary acces to head longadder.
    @jdk.internal.vm.annotation.Contended
    long headCache = 0L;

    long c1, c2, c3, c4, c5, c6, c7, c8;

    final LongAdder head = new LongAdder();

    public ExchangeQueue(final int size) {
        //Round up to neares pow of 2
        int rs = 1;
        while (rs < size) rs <<= 1;
        this.size = rs;
        this.mask = rs - 1;

        buffer = (E[]) new Object[this.size];
    }


    public boolean offer(final E e) {
        if (e != null) {
            final long tail = this.tail.sum();
            final long queueStart = tail - size;
            if ((headCache > queueStart) || ((headCache = head.sum()) > queueStart)) {
                final int dx = (int) (tail & mask);
                buffer[dx] = e;
                this.tail.increment();
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException("Provided value cannot be null!");
        }
    }

    public E poll() {
        final long head = this.head.sum();
        if ((head < tailCache) || (head < (tailCache = tail.sum()))) {
            final int dx = (int) (head & mask);
            final E e = buffer[dx];
            buffer[dx] = null;
            this.head.increment();
            return e;
        } else {
            return null;
        }
    }

    public final int size() {
        return (int) Math.max(tail.sum() - head.sum(), 0);
    }

    public int capacity() {
        return size;
    }

    public final boolean isEmpty() {
        return tail.sum() == head.sum();
    }

    long preventOptimization() {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8 + r1 + r2 + r3 + r4 + r5 + r6 + r7 + c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + headCache + tailCache;
    }

}
