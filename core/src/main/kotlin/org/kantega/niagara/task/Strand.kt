package org.kantega.niagara.task



/**
 * A Strans is connected to a scheduled task. You can interrupt the task
 * thougth the Strand, and you can bind new Tasks to the task by attaching them.
 * @param <A>
</A> */
interface Strand<A> {

    /**
     * Interrupts the attached task if it is pending.
     * @return
     */
    fun interrupt(): Task<Unit>

    /**
     * A handle for the task this strand is attached to
     * @return
     */
    fun attach(): Task<A>
}
