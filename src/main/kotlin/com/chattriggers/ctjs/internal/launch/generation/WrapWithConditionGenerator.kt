package com.chattriggers.ctjs.internal.launch.generation

import codes.som.koffee.MethodAssembly
import codes.som.koffee.insns.jvm.ldc
import com.chattriggers.ctjs.internal.launch.At
import com.chattriggers.ctjs.internal.launch.Descriptor
import com.chattriggers.ctjs.internal.launch.WrapWithCondition
import com.chattriggers.ctjs.internal.launch.generation.Utils.toJvmDescriptor
import com.chattriggers.ctjs.internal.utils.descriptorString
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition as SPWrapWithCondition

internal class WrapWithConditionGenerator(
    ctx: GenerationContext,
    id: Int,
    private val wrapWithCondition: WrapWithCondition,
) : InjectorGenerator(ctx, id) {
    override val type = "wrapWithCondition"

    override fun getInjectionSignature(): InjectionSignature {
        val method = ctx.findMethod(wrapWithCondition.method)

        val parameters = mutableListOf<Parameter>()

        when (val atTarget = wrapWithCondition.at.atTarget) {
            is At.InvokeTarget -> {
                val descriptor = atTarget.descriptor
                val owner = descriptor.owner ?: error("owner not found $descriptor")
                val targetMethodIsStatic = Modifier.isStatic(Utils.findMethod(owner.originalDescriptor(), descriptor).modifiers)

                if (!targetMethodIsStatic)
                    parameters.add(Parameter(owner))

                descriptor.parameters!!.forEach {
                    parameters.add(Parameter(it))
                }
            }
            is At.FieldTarget -> {
                require(atTarget.isStatic != null && atTarget.isGet != null) {
                    "WrapWithCondition targeting FIELD expects an opcode value"
                }
                require(!atTarget.isGet) {
                    "WrapWithCondition targeting FIELD expects opcode to be PUTFIELD or PUTSTATIC"
                }

                val descriptor = atTarget.descriptor
                if (!atTarget.isStatic)
                    parameters.add(Parameter(descriptor.owner!!))

                parameters.add(Parameter(descriptor.type!!))
            }
            else -> error("Unexpected At.target for WrapWithCondition: ${atTarget.targetName}")
        }

        return InjectionSignature(
            method,
            parameters,
            Descriptor.Primitive.BOOLEAN,
            Modifier.isStatic(method.modifiers),
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPWrapWithCondition::class.descriptorString(), true).apply {
            visit("method", signature.targetMethod.toJvmDescriptor())
            visit("at", Utils.createAtAnnotation(wrapWithCondition.at))
            if (wrapWithCondition.slice != null)
                visit("slice", wrapWithCondition.slice.map(Utils::createSliceAnnotation))
            if (wrapWithCondition.remap != null)
                visit("remap", wrapWithCondition.remap)
            if (wrapWithCondition.require != null)
                visit("require", wrapWithCondition.require)
            if (wrapWithCondition.expect != null)
                visit("expect", wrapWithCondition.expect)
            if (wrapWithCondition.allow != null)
                visit("allow", wrapWithCondition.allow)
            visitEnd()
        }
    }

    context(methodAssembly: MethodAssembly)
    override fun generateNotAttachedBehavior() {
        methodAssembly.ldc(1)
    }
}
