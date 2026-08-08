package com.chattriggers.ctjs.internal.launch.generation

import codes.som.koffee.MethodAssembly
import com.chattriggers.ctjs.internal.launch.Descriptor
import com.chattriggers.ctjs.internal.launch.ModifyReturnValue
import com.chattriggers.ctjs.internal.launch.generation.Utils.jvmDescriptor
import com.chattriggers.ctjs.internal.launch.generation.Utils.toJvmDescriptor
import com.chattriggers.ctjs.internal.utils.descriptorString
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier
import com.llamalad7.mixinextras.injector.ModifyReturnValue as SPModifyReturnValue

internal class ModifyReturnValueInjector(
    ctx: GenerationContext,
    id: Int,
    private val modifyReturnValue: ModifyReturnValue
) : InjectorGenerator(ctx, id) {
    override val type = "modifyReturnValue"

    override fun getInjectionSignature(): InjectionSignature {
        val method = ctx.findMethod(modifyReturnValue.method)
        val returnType = Descriptor.Parser(method.returnType.jvmDescriptor()).parseType(full = true)
        check(returnType != Descriptor.Primitive.VOID) {
            "ModifyReturnValue mixin cannot target a void method"
        }

        val parameters = listOf(Parameter(returnType)) + modifyReturnValue.locals
            ?.map(Utils::getParameterFromLocal)
            .orEmpty()

        return InjectionSignature(
            method,
            parameters,
            returnType,
            Modifier.isStatic(method.modifiers),
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPModifyReturnValue::class.descriptorString(), true).apply {
            visit("method", listOf(signature.targetMethod.toJvmDescriptor()))
            visit("at", Utils.createAtAnnotation(modifyReturnValue.at))
            if (modifyReturnValue.slice != null)
                visit("slice", listOf(modifyReturnValue.slice.map(Utils::createSliceAnnotation)))
            if (modifyReturnValue.remap != null)
                visit("remap", modifyReturnValue.remap)
            if (modifyReturnValue.require != null)
                visit("require", modifyReturnValue.require)
            if (modifyReturnValue.expect != null)
                visit("expect", modifyReturnValue.expect)
            if (modifyReturnValue.allow != null)
                visit("allow", modifyReturnValue.allow)
            visitEnd()
        }
    }

    context(methodAssembly: MethodAssembly)
    override fun generateNotAttachedBehavior() {
        methodAssembly.generateParameterLoad(0)
    }
}
