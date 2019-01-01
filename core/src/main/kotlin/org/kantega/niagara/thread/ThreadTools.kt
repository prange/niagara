package org.kantega.niagara.thread

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

import java.lang.invoke.MethodType.methodType

object ThreadTools {
    private val onSpinWaitMethod: MethodHandle?

    init {
        val lookup = MethodHandles.lookup()

        var method: MethodHandle? = null
        try {
            method = lookup.findStatic(Thread::class.java, "onSpinWait", methodType(Void.TYPE))
        } catch (ignore: Exception) {
        }

        onSpinWaitMethod = method
    }


    fun onSpinWait() {
        if (null != onSpinWaitMethod) {
            try {
                onSpinWaitMethod.invokeExact()
            } catch (ignore: Throwable) {
            }

        }
    }
}
