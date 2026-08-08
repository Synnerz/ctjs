package com.chattriggers.ctjs.internal.launch.generation

import com.chattriggers.ctjs.internal.launch.*
import com.chattriggers.ctjs.internal.utils.descriptorString
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import java.lang.reflect.Method
import org.spongepowered.asm.mixin.injection.At as SPAt
import org.spongepowered.asm.mixin.injection.Constant as SPConstant
import org.spongepowered.asm.mixin.injection.Slice as SPSlice

internal object Utils {
    fun createAtAnnotation(at: At): AnnotationNode {
        return AnnotationNode(SPAt::class.descriptorString()).apply {
            if (at.id != null)
                visit("id", at.id)
            visit("value", at.value)
            if (at.slice != null)
                visit("slice", at.slice)
            if (at.shift != null)
                visit("shift", arrayOf(SPAt.Shift::class.java.descriptorString(), at.shift.name))
            if (at.by != null)
                visit("by", at.by)
            if (at.args != null)
                visit("args", at.args)
            if (at.target != null)
                visit("target", at.atTarget.descriptor.mappedDescriptor())
            if (at.ordinal != null)
                visit("ordinal", at.ordinal)
            if (at.opcode != null)
                visit("opcode", at.opcode)
            if (at.remap != null)
                visit("remap", at.remap)

            visitEnd()
        }
    }

    fun createSliceAnnotation(slice: Slice): AnnotationNode {
        return AnnotationNode(SPSlice::class.descriptorString()).apply {
            if (slice.id != null)
                visit("id", slice.id)
            if (slice.from != null)
                visit("from", createAtAnnotation(slice.from))
            if (slice.to != null)
                visit("to", createAtAnnotation(slice.to))
            visitEnd()
        }
    }

    fun createConstantAnnotation(constant: Constant): AnnotationNode {
        return AnnotationNode(SPConstant::class.descriptorString()).apply {
            if (constant.nullValue != null)
                visit("nullValue", constant.nullValue)
            if (constant.intValue != null)
                visit("intValue", constant.intValue)
            if (constant.floatValue != null)
                visit("floatValue", constant.floatValue)
            if (constant.longValue != null)
                visit("longValue", constant.longValue)
            if (constant.doubleValue != null)
                visit("doubleValue", constant.doubleValue)
            if (constant.stringValue != null)
                visit("stringValue", constant.stringValue)
            if (constant.classValue != null) {
                visit("classValue", Type.getObjectType(constant.classValue))
            }
            if (constant.ordinal != null)
                visit("ordinal", constant.ordinal)
            if (constant.slice != null)
                visit("slice", constant.slice)
            if (constant.expandZeroConditions != null)
                visit("expandZeroConditions", constant.expandZeroConditions)
            if (constant.log != null)
                visit("log", constant.log)
        }
    }

    // FIXME: fix access wideners not working probably
    fun widenField(
//        mappedClass: Mappings.MappedClass, fieldName: String, isMutable: Boolean
    ) {
//        val field = mappedClass.fields[fieldName]
//            ?: error("Unable to find field $fieldName in class ${mappedClass.name.original}")

//        FabricLoaderImpl.INSTANCE.accessWidener.visitField(
//            mappedClass.name.value,
//            field.name.value,
//            field.type.value,
//            AccessWidenerReader.AccessType.ACCESSIBLE,
//            false,
//        )
//
//        if (isMutable) {
//            FabricLoaderImpl.INSTANCE.accessWidener.visitField(
//                mappedClass.name.value,
//                field.name.value,
//                field.type.value,
//                AccessWidenerReader.AccessType.MUTABLE,
//                false,
//            )
//        }
    }

    fun widenMethod(
//        mappedClass: Mappings.MappedClass,
//        methodName: String,
//        isMutable: Boolean,
    ) {
//        val descriptor = Descriptor.Parser(methodName).parseMethod(full = false)
//        val mappedMethod = findMethod(mappedClass, descriptor).first

//        FabricLoaderImpl.INSTANCE.accessWidener.visitMethod(
//            mappedClass.name.value,
//            mappedMethod.name.value,
//            mappedMethod.toDescriptor(),
//            AccessWidenerReader.AccessType.ACCESSIBLE,
//            false,
//        )
//
//        if (isMutable) {
//            FabricLoaderImpl.INSTANCE.accessWidener.visitMethod(
//                mappedClass.name.value,
//                mappedMethod.name.value,
//                mappedMethod.toDescriptor(),
//                AccessWidenerReader.AccessType.MUTABLE,
//                false,
//            )
//        }
    }

    fun findMethod(
        className: String,
        descriptor: Descriptor.Method,
    ): Method {
        val clazz = Class.forName(className)
        val parameters = descriptor.parameters

        val methods = sequence {
            yieldAll(clazz.hierarchy())
        }
            .flatMap { it.declaredMethods.asSequence() }
            .filter { it.name == descriptor.name }
            .filter {
                parameters == null ||
                        (
                                it.parameterCount == parameters.size &&
                                        it.parameterTypes.zip(parameters).all { (type, parameter) ->
                                            type.jvmDescriptor() == parameter.originalDescriptor()
                                        }
                                )
            }
            .toList()

        return when (methods.size) {
            0 -> error(
                "Unable to match method $descriptor in class $className"
            )

            1 -> methods.single()

            else -> error(
                "Multiple methods match name ${descriptor.name} in class $className, " +
                        "please provide a method descriptor"
            )
        }
    }

    private fun Class<*>.hierarchy(): Sequence<Class<*>> = sequence {
        yield(this@hierarchy)

        superclass?.let {
            yieldAll(it.hierarchy())
        }

        for (interfaceClass in interfaces) {
            yieldAll(interfaceClass.hierarchy())
        }
    }

    fun Method.toJvmDescriptor(): String =
        buildString {
            append('(')
            parameterTypes.forEach {
                append(it.jvmDescriptor())
            }
            append(')')
            append(returnType.jvmDescriptor())
        }

    fun Class<*>.jvmDescriptor(): String = when {
        isPrimitive -> when (this) {
            java.lang.Void.TYPE -> "V"
            java.lang.Boolean.TYPE -> "Z"
            java.lang.Byte.TYPE -> "B"
            java.lang.Character.TYPE -> "C"
            java.lang.Short.TYPE -> "S"
            java.lang.Integer.TYPE -> "I"
            java.lang.Long.TYPE -> "J"
            java.lang.Float.TYPE -> "F"
            java.lang.Double.TYPE -> "D"
            else -> error("Unknown primitive type: $this")
        }

        isArray -> name.replace('.', '/')

        else -> "L${name.replace('.', '/')};"
    }

    fun getParameterFromLocal(local: Local, name: String = "Local"): InjectorGenerator.Parameter {
        val descriptor = when {
            local.print == true -> {
                // The type doesn't matter, it won't actually be applied
                Descriptor.Primitive.INT
            }
            local.type != null -> {
                if (local.index != null) {
                    require(local.ordinal == null) {
                        "$name that specifies a type and index cannot specify an ordinal"
                    }
                } else {
                    require(local.ordinal != null) {
                        "$name that specifies a type must also specify an index or ordinal"
                    }
                }
                Descriptor.Parser(local.type).parseType(full = true)
            }
            else -> error("$name must specify \"print\", or \"type\" and either \"ordinal\" or \"index\"")
        }

        require(descriptor.isType)

        return InjectorGenerator.Parameter(descriptor, local)
    }
}
