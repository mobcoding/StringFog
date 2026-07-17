package com.github.megatronking.stringfog.plugin

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import groovy.xml.XmlParser
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.FileInputStream
import java.io.InputStreamReader

class StringFogPlugin : Plugin<Project> {

    companion object {
        private const val PLUGIN_NAME = "stringfog"
    }

    private fun String.asTaskSuffix(): String {
        return replaceFirstChar { ch ->
            if (ch.isLowerCase()) {
                ch.titlecase()
            } else {
                ch.toString()
            }
        }
    }

    override fun apply(project: Project) {
        project.extensions.create(PLUGIN_NAME, StringFogExtension::class.java)
        var configured = false

        fun configureWhenAndroidPluginIsApplied() {
            if (configured) {
                return
            }
            val androidExtension = project.extensions.findByName("android")
            val extension = androidExtension as? CommonExtension
                ?: throw GradleException("StringFog plugin must be used with android plugin")
            configured = true
            configure(project, extension)
        }

        project.pluginManager.withPlugin("com.android.application") {
            configureWhenAndroidPluginIsApplied()
        }
        project.pluginManager.withPlugin("com.android.library") {
            configureWhenAndroidPluginIsApplied()
        }
        project.pluginManager.withPlugin("com.android.dynamic-feature") {
            configureWhenAndroidPluginIsApplied()
        }
        project.afterEvaluate {
            if (!configured) {
                throw GradleException("StringFog plugin must be used with android plugin")
            }
        }
    }

    private fun configure(project: Project, extension: CommonExtension) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            val stringfog = project.extensions.getByType(StringFogExtension::class.java)
            if (stringfog.implementation.isNullOrEmpty()) {
                throw IllegalArgumentException("Missing stringfog implementation config")
            }
            if (!stringfog.enable) {
                return@onVariants
            }
            val fogPackages = stringfog.fogPackages.map(String::trim)
            require(fogPackages.none(String::isEmpty)) {
                "stringfog.fogPackages must not contain blank package names"
            }

            var applicationId: String? = null
            // Priority: AndroidManifest -> namespace -> stringfog.packageName.
            val manifestFile = project.file("src/main/AndroidManifest.xml")
            if (manifestFile.exists()) {
                val parsedManifest = XmlParser().parse(
                    InputStreamReader(FileInputStream(manifestFile), "utf-8")
                )
                applicationId = parsedManifest.attribute("package")?.toString()
            }
            if (applicationId.isNullOrEmpty()) {
                applicationId = extension.namespace
            }
            if (applicationId.isNullOrEmpty()) {
                applicationId = stringfog.packageName
            }
            if (applicationId.isNullOrEmpty()) {
                throw IllegalArgumentException("Unable to resolve applicationId")
            }

            val logs = mutableListOf<String>()
            // ALL includes dependency classes and is only available to application modules.
            val instrumentationScope = if (
                extension is ApplicationExtension && fogPackages.isNotEmpty()
            ) {
                InstrumentationScope.ALL
            } else {
                InstrumentationScope.PROJECT
            }
            variant.instrumentation.transformClassesWith(
                StringFogTransform::class.java,
                instrumentationScope
            ) { params ->
                params.setParameters(
                    applicationId,
                    stringfog,
                    logs,
                    "$applicationId.${SourceGeneratingTask.FOG_CLASS_NAME}",
                    fogPackages
                )
            }
            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            )

            val generateTaskName = "generateStringFog${variant.name.asTaskSuffix()}"
            val stringfogDir = project.layout.buildDirectory.dir(
                "generated/source/stringFog/${variant.name}"
            )
            val provider = project.tasks.register(
                generateTaskName,
                SourceGeneratingTask::class.java
            ) { task ->
                task.genDir.set(stringfogDir)
                task.applicationId.set(applicationId)
                task.implementation.set(stringfog.implementation)
                task.mode.set(stringfog.mode)
            }
            variant.sources.java?.addGeneratedSourceDirectory(
                provider,
                SourceGeneratingTask::genDir
            )
        }
    }
}
