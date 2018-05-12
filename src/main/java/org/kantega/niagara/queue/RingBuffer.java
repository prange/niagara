package org.kantega.niagara.queue;

/**
 * Mutable ringbuffer based queue for singlethreaded access. Do not share between threads.
 * @param <E>
 */
@SuppressWarnings({"WeakerAccess", "unchecked"})
public class RingBuffer<E> {
    final int size;

    final long mask;

    long tail = 0L;

    final E[] buffer;

    long head = 0L;

    public RingBuffer(final int size) {
        //Round up to neares pow of 2 (for mask)
        int rs = 1;
        while (rs < size) rs <<= 1;
        this.size = rs;
        this.mask = rs - 1;

        buffer = (E[]) new Object[this.size];
    }


    public boolean offer(final E e) {
        if (e != null) {
            final long dropoff = tail - size;
            if ((head > dropoff)) {
                final int dx = (int) (tail & mask);
                buffer[dx] = e;
                tail++;
                return true;
            } else {
                return false; //queue was full
            }
        } else {
            throw new NullPointerException("Provided value cannot be null!");
        }
    }

    public E poll() {
        if ((head < tail)) {
            final int dx = (int) (head & mask);
            final E e = buffer[dx];
            buffer[dx] = null;
            head++;
            return e;
        } else {
            return null;
        }
    }

    public final int size() {
        return (int) Math.max(tail - head, 0);
    }

    public int capacity() {
        return size;
    }

    public final boolean isEmpty() {
        return tail == head;
    }
}
