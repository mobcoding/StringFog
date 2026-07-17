package com.github.megatronking.stringfog.plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import org.objectweb.asm.ClassVisitor

abstract class StringFogTransform : AsmClassVisitorFactory<StringFogInstrumentationParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return with(parameters.get()) {
            ClassVisitorFactory.create(
                implementation, logs, fogPackages.get().toTypedArray(), extension.kg, className.get(),
                classContext.currentClassData.className, extension.mode, nextClassVisitor
            )
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean =
        ClassVisitorFactory.shouldInstrument(
            parameters.get().fogPackages.get().toTypedArray(),
            classData.className
        )

}
