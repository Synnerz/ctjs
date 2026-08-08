package com.chattriggers.ctjs.internal.launch.generation

import com.chattriggers.ctjs.internal.launch.Descriptor
import com.chattriggers.ctjs.internal.launch.DynamicMixinManager
import com.chattriggers.ctjs.internal.launch.Mixin
import java.lang.reflect.Method

internal data class GenerationContext(val mixin: Mixin) {
    val generatedClassName = "CTMixin_\$${mixin.target.replace('.', '_')}\$_${mixinCounter++}"
    val generatedClassFullPath = "${DynamicMixinManager.GENERATED_PACKAGE}/$generatedClassName"

    fun findMethod(method: String): Method {
        val descriptor = Descriptor.Parser(method).parseMethod(full = false)
        return Utils.findMethod(mixin.target, descriptor)
    }

    companion object {
        private var mixinCounter = 0
    }
}
