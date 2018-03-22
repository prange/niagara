
package org.kantega.niagara.exchange;


import org.kantega.niagara.op.ScopeFlag;
import org.kantega.niagara.concurrent.PullStrategy;
import org.kantega.niagara.concurrent.ThreadTools;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

abstract class BackoffPullPrePad
{
    @SuppressWarnings("unused")
    long p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15;
}

abstract class BackoffPullData extends BackoffPullPrePad
{
    protected static final int NOT_IDLE = 0;
    protected static final int SPINNING = 1;
    protected static final int YIELDING = 2;
    protected static final int PARKING = 3;

    protected final long maxSpins;
    protected final long maxYields;
    protected final long minParkPeriodNs;
    protected final long maxParkPeriodNs;

    protected int state = NOT_IDLE;
    protected long spins;
    protected long yields;
    protected long parkPeriodNs;

    BackoffPullData(
        final long maxSpins, final long maxYields, final long minParkPeriodNs, final long maxParkPeriodNs)
    {
        this.maxSpins = maxSpins;
        this.maxYields = maxYields;
        this.minParkPeriodNs = minParkPeriodNs;
        this.maxParkPeriodNs = maxParkPeriodNs;
    }
}


public final class BackoffPull<A> extends BackoffPullData implements PullStrategy<A>
{
    @SuppressWarnings("unused")
    long p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15;


    public BackoffPull(
        final long maxSpins, final long maxYields, final long minParkPeriodNs, final long maxParkPeriodNs)
    {
        super(maxSpins, maxYields, minParkPeriodNs, maxParkPeriodNs);
    }


    public A pull(ScopeFlag s, Supplier<A> supplier){
        A value;
        while((value=supplier.get())==null && s.keepRunning()){
            idle();
        }
        reset();
        return value;
    }

    private void idle()
    {
        switch (state)
        {
            case NOT_IDLE:
                state = SPINNING;
                spins++;
                break;

            case SPINNING:
                ThreadTools.onSpinWait();
                if (++spins > maxSpins)
                {
                    state = YIELDING;
                    yields = 0;
                }
                break;

            case YIELDING:
                if (++yields > maxYields)
                {
                    state = PARKING;
                    parkPeriodNs = minParkPeriodNs;
                }
                else
                {
                    Thread.yield();
                }
                break;

            case PARKING:
                LockSupport.parkNanos(parkPeriodNs);
                parkPeriodNs = Math.min(parkPeriodNs << 1, maxParkPeriodNs);
                break;
        }
    }

    private void reset()
    {
        spins = 0;
        yields = 0;
        parkPeriodNs = minParkPeriodNs;
        state = NOT_IDLE;
    }

    public String toString()
    {
        return "BackoffIdleStrategy{" +
            "maxSpins=" + maxSpins +
            ", maxYields=" + maxYields +
            ", minParkPeriodNs=" + minParkPeriodNs +
            ", maxParkPeriodNs=" + maxParkPeriodNs +
            '}';
    }
}

