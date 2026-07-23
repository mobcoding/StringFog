# Compatibility And Verification

## Version Matrix

| Consumer AGP | StringFog version | Additional requirement |
|---|---:|---|
| 8.x | `5.2.2` | Enable `android.buildFeatures.buildConfig = true`. |
| 9.x | `5.3.3` | No StringFog-specific BuildConfig requirement. |

Use the same version for:

- `com.github.mobcoding.StringFog:gradle-plugin`
- `com.github.mobcoding.StringFog:xor`

Do not add the `v` prefix to StringFog versions.

## Kotlin DSL Pattern

Resolve the plugin marker to the JitPack implementation in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "stringfog") {
                useModule(
                    "com.github.mobcoding.StringFog:gradle-plugin:${requested.version}"
                )
            }
        }
    }
}
```

In the final application module:

```kotlin
import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.stringfog)
}

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    enable = true
    fogPackages = arrayOf(
        "com.example.app",
        "com.example.secure"
    )
    kg = RandomKeyGenerator()
    mode = StringFogMode.bytes
}

dependencies {
    implementation(libs.stringfog.xor)
}
```

Use the existing project convention when it already resolves `stringfog`; do not add a second plugin resolution path.

`fogPackages` is one allowlist for both project and dependency classes. When encrypting an AAR/JAR, include the confirmed application-source roots and the confirmed AAR/JAR roots. A nonempty list containing only the AAR root skips application-source classes.

## Inspection Commands

Use commands adapted to the supplied paths. On Windows, extract only the data required for inspection.

```powershell
jar tf <input-aar> | Select-String 'classes.jar'
jar tf <extracted-classes.jar> | Select-String '^com/example/secure/'
javap -classpath <extracted-classes.jar> -c -p com.example.secure.EntryPoint
```

After producing an AAB, inspect its DEX entries and scan for a plaintext sampled from the input AAR:

```powershell
jar tf <release-aab> | Select-String '^base/dex/classes[0-9]*\.dex$'
```

Extract DEX files to a temporary directory, then perform a binary-safe scan. An absent plaintext is evidence only when the corresponding AAR class is retained or otherwise known to execute; R8 may remove dead code.

## Failure Diagnosis

- `NoClassDefFoundError` for `<application package>.StringFog`: verify the final app owns the plugin configuration and `xor` runtime dependency; do not add a hand-written replacement class.
- AAR or application plaintext remains: verify the real AAR package, every confirmed application-source and AAR root in `fogPackages`, final app module placement, and AGP/StringFog compatibility.
- KSP/Room variant failures after migration: remove old custom plugin code that places AGP API on a runtime classpath. Do not add `extendsFrom` processor-classpath patches as a substitute.
