package org.kantega.niagara;

public interface Emitter {

    /**
     * Emits one value. Lets the caller know iw work has actually been done. This is useful when the runner wants to pause the thread
     * in case the supplier currently has no data available.
     *
     * @return true if work has been done, false if the Emitter remained idle (because no data was available).
     */
    boolean emit();

}
