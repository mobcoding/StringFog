---
name: android-stringfog-integration
description: Integrate, migrate, validate, or troubleshoot mobcoding StringFog encryption for an external Android AAR. Use when a user supplies an AAR path and needs its classes encrypted in an Android application, needs AGP 8/9 compatible StringFog configuration, wants to replace a legacy AAR StringFog plugin, or needs Release APK/AAB verification.
---

# Android StringFog Integration

Encrypt external AAR classes only after inspecting the supplied artifact and the consuming Android project. Keep changes scoped to the final application module.

## Required Input

Require an absolute path to the AAR to encrypt. Do not guess an AAR from `libs/` or configure StringFog until the user supplies the intended artifact.

Read [references/compatibility-and-verification.md](references/compatibility-and-verification.md) before changing Gradle files.

## Workflow

1. Inspect the AAR without modifying it.
   - List `classes.jar` entries and identify the actual package roots.
   - Inspect representative classes and string constants. Distinguish application-owned AAR code from embedded third-party packages.
   - Present the candidate package roots and obtain confirmation before using them in `fogPackages`.

2. Audit the consumer project.
   - Read `settings.gradle*`, the version catalog, root build script, the final `com.android.application` module, and any existing StringFog/build-logic/buildSrc configuration.
   - Determine the AGP major version before selecting StringFog.
   - Locate the final app module containing the supplied AAR. Apply external-AAR encryption there, not in a library or dynamic-feature module.

3. Configure the remote plugin conservatively.
   - Use the compatibility version from the reference; keep `gradle-plugin` and `xor` on the same version.
   - Resolve plugin ID `stringfog` through `pluginManagement.resolutionStrategy` when using a version-catalog alias.
   - Add `com.github.mobcoding.StringFog:xor` as an `implementation` dependency of the final app module.
   - Configure `StringFogExtension` with `StringFogImpl`, `enable = true`, `StringFogMode.bytes`, and only the confirmed AAR package roots in `fogPackages`.
   - For AGP 8, ensure `android.buildFeatures.buildConfig = true`.

4. Migrate legacy implementations only after the remote configuration resolves.
   - Remove obsolete local plugin JARs, custom AAR transform plugins, and buildSrc/build-logic code only when they are exclusively for the old StringFog path.
   - Never put AGP APIs in a plugin runtime dependency. Keep plugin-side AGP APIs `compileOnly`.
   - Do not create, copy, delete, or add manual keep rules for the generated `<application package>.StringFog` class.

5. Build and verify the Release artifact.
   - Build the requested APK or AAB after a successful Gradle configuration check.
   - Compare known plaintext from the input AAR with final DEX content. Confirm the selected AAR package survives and known sensitive literals are absent.
   - Confirm the final runtime classpath contains the matching `xor` artifact.
   - Treat R8-merged/decompiled final classes as output artifacts, not proof of an original AAR class boundary.

## Guardrails

- `fogPackages` matches package boundaries. `com.example.foo` matches `com.example.foo.*`, not `com.example.foobar.*`.
- A nonempty `fogPackages` in the final app module permits matching dependency AAR/JAR classes to be instrumented. Resources, assets, and native libraries are outside StringFog scope.
- Empty strings and third-party static constants can remain visible in decompiled code. Diagnose against input AAR bytecode and final DEX, not visual consistency alone.
- Preserve unrelated Gradle, KSP/Room, signing, and AabResGuard configuration.
