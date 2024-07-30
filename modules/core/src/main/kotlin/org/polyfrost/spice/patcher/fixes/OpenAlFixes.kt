package org.polyfrost.spice.patcher.fixes

import org.lwjgl.LWJGLException
import org.polyfrost.spice.patcher.util.AudioHelper

@Suppress("unused")
object OpenAlFixes {
    @JvmStatic
    fun create() {
        try {
            AudioHelper.createContext(null, -1, 60, false)
        } catch (ex: Throwable) {
            throw LWJGLException(ex)
        }
    }

    @JvmStatic
    fun destroyContext() = AudioHelper.destroyContext()
}