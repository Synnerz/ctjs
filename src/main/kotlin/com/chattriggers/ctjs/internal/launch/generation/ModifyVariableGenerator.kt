package com.chattriggers.ctjs.internal.launch.generation

import codes.som.koffee.MethodAssembly
import com.chattriggers.ctjs.internal.launch.Local
import com.chattriggers.ctjs.internal.launch.ModifyVariable
import com.chattriggers.ctjs.internal.launch.generation.Utils.toJvmDescriptor
import com.chattriggers.ctjs.internal.utils.descriptorString
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier
import org.spongepowered.asm.mixin.injection.ModifyVariable as SPModifyVariable

internal class ModifyVariableGenerator(
    ctx: GenerationContext,
    id: Int,
    private var modifyVariable: ModifyVariable,
) : InjectorGenerator(ctx, id) {
    override val type = "modifyVariable"

    override fun getInjectionSignature(): InjectionSignature {
        val method = ctx.findMethod(modifyVariable.method)

        // Construct a temporary local so we can call Utils.getParameterFromLocal
        val tempLocal = Local(
            modifyVariable.print,
            modifyVariable.index,
            modifyVariable.ordinal,
            modifyVariable.type,
            mutable = false,
        )

        val parameter = Utils.getParameterFromLocal(tempLocal, name = "ModifyVariable")

        // Update our ModifyVariable annotation since we may have changed the index
        modifyVariable = modifyVariable.copy(
            index = parameter.local?.index ?: modifyVariable.index,
        )

        return InjectionSignature(
            method,
            listOf(parameter.copy(local = null)),
            parameter.descriptor,
            Modifier.isStatic(method.modifiers),
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPModifyVariable::class.descriptorString(), true).apply {
            visit("method", listOf(signature.targetMethod.toJvmDescriptor()))
            visit("at", Utils.createAtAnnotation(modifyVariable.at))
            if (modifyVariable.slice != null)
                visit("slice", Utils.createSliceAnnotation(modifyVariable.slice!!))
            if (modifyVariable.print != null)
                visit("print", modifyVariable.print)
            if (modifyVariable.ordinal != null)
                visit("ordinal", modifyVariable.ordinal)
            if (modifyVariable.index != null)
                visit("index", modifyVariable.index)
            if (modifyVariable.remap != null)
                visit("remap", modifyVariable.remap)
            if (modifyVariable.require != null)
                visit("require", modifyVariable.require)
            if (modifyVariable.expect != null)
                visit("expect", modifyVariable.expect)
            if (modifyVariable.allow != null)
                visit("allow", modifyVariable.allow)
            if (modifyVariable.constraints != null)
                visit("constraints", modifyVariable.constraints)
            visitEnd()
        }
    }

    context(methodAssembly: MethodAssembly)
    override fun generateNotAttachedBehavior() {
        methodAssembly.generateParameterLoad(0)
    }
}
