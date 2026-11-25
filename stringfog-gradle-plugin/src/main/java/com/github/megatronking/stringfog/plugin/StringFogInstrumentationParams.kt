package com.github.megatronking.stringfog.plugin

import com.android.build.api.instrumentation.InstrumentationParameters
import com.github.megatronking.stringfog.IKeyGenerator
import com.github.megatronking.stringfog.plugin.kg.HardCodeKeyGenerator
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.Serializable

abstract class StringFogInstrumentationParams : InstrumentationParameters {
    @get:Input
    abstract val className: Property<String>

    @get:Input
    abstract val fogPackages: ListProperty<String>

    @get:Input
    abstract val implementationClass: Property<String>

    @get:Input
    abstract val mode: Property<StringFogMode>

    @get:Input
    @get:Optional
    abstract val randomKeyLength: Property<Int>

    @get:Input
    @get:Optional
    abstract val hardCodedKey: Property<ByteArray>

    @get:Input
    @get:Optional
    abstract val customKeyGenerator: Property<Serializable>

    internal fun setKeyGenerator(generator: IKeyGenerator) {
        when (generator.javaClass) {
            RandomKeyGenerator::class.java -> randomKeyLength.set((generator as RandomKeyGenerator).length)
            HardCodeKeyGenerator::class.java -> hardCodedKey.set((generator as HardCodeKeyGenerator).key)
            else -> {
                require(generator is Serializable) {
                    "Custom StringFog key generator ${generator.javaClass.name} must implement java.io.Serializable"
                }
                customKeyGenerator.set(generator)
            }
        }
    }

    internal fun createKeyGenerator(): IKeyGenerator = when {
        randomKeyLength.isPresent -> RandomKeyGenerator(randomKeyLength.get())
        hardCodedKey.isPresent -> HardCodeKeyGenerator(hardCodedKey.get().clone())
        else -> customKeyGenerator.get() as IKeyGenerator
    }
}
