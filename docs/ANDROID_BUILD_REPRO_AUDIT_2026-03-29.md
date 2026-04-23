# Android Build Repro Audit 2026-03-29

This is a repo-local audit of `Ae.solator` build logic with focus on:

- configuration-cache-hostile inputs
- host drift
- deterministic CLI-first Android build closure

Scope:

- root Gradle entrypoint
- `app` module build logic
- repo-local bootstrap/build helpers that feed the Android build lane

## Executive Verdict

The structural audit findings are now closed in the repository build logic.

The authoritative root lane is cache-capable, `preBuild` is no longer a
network/source-mutation hook, the split wrapper/property drift has been
collapsed, and the NDK runtime lane no longer hard-codes one host prebuilt
path.

The current residual is narrower and more honest:

- bare root `./gradlew --no-daemon assembleDebug` now succeeds on this Termux
  host because the wrapper auto-wires the local Termux `aapt2` binary
- the documented Termux lane
  `. ./tools/env-android-local.sh && ./gradlew --no-daemon assembleDebug`
  still succeeds and remains useful for the broader host LLVM/ADB lane
- the remaining Gradle 9 deprecation comes from the Kotlin Android plugin, not
  from repo-local build logic

So the corrected verdict is:

- configuration cache: healthy on the authoritative lane
- reproducible build closure: structurally repaired in-repo
- current host residual: no structural build-logic blocker remains in this
  audit class

## Closed Findings

### 1. `preBuild` no longer mutates source assets or pulls network artifacts

Status: fixed

Proof:

- [app/build.gradle](/data/data/com.termux/files/home/aesolator/app/build.gradle)
- live dry run:
  `./gradlew :app:preBuild -m`

Current state:

- `downloadImageFS` is now an explicit helper task only
- donor rootfs downloads land in `build/downloads/imagefs`, not in
  `app/src/main/assets`
- `preBuild` depends on:
  - `verifyBundledImageFsAssets`
  - `prepareEmbeddedWineAssets`
  - `prepareRuntimeSharedJni`
- `:app:preBuild -m` no longer contains `:app:downloadImageFS`

Why this matters:

- the authoritative app build lane is no longer network-sensitive
- `src/main/assets` is no longer rewritten during normal builds
- deterministic operator discipline is now compatible with the repo layout

### 2. Eager host probing and hard failure were reduced out of the main build model

Status: fixed

Proof:

- [app/build.gradle](/data/data/com.termux/files/home/aesolator/app/build.gradle)
- live proof:
  `./gradlew :app:help --configuration-cache --configuration-cache-problems=warn`
  and
  `./gradlew :app:properties --configuration-cache --configuration-cache-problems=warn`

Current state:

- NDK runtime resolution moved into task execution through
  `prepareRuntimeSharedJni`
- signing config no longer throws eagerly just because the keystore path does
  not exist at configuration time
- root tasks like `help`, `properties`, and `preBuild -m` execute without the
  old configuration-time failure pattern

Why this matters:

- the lane is less brittle for non-package tasks
- host state is less aggressively baked into configuration
- configuration cache remains usable after the remediation

### 3. The NDK runtime lane no longer hard-codes `linux-x86_64`

Status: fixed

Proof:

- [app/build.gradle](/data/data/com.termux/files/home/aesolator/app/build.gradle)

Current state:

- host prebuilt root is derived from `toolchains/llvm/prebuilt/*`
- optional override is explicit through `AEO_ANDROID_NDK_HOST_TAG`
- the build script no longer treats one desktop host tag as permanent truth

Why this matters:

- the NDK lane now has one deliberate host-resolution policy
- host drift is reduced to bootstrap/runtime state, not hard-coded DSL logic

### 4. Split wrapper authority was collapsed

Status: fixed

Proof:

- root wrapper:
  [gradle-wrapper.properties](/data/data/com.termux/files/home/aesolator/gradle/wrapper/gradle-wrapper.properties)
- nested app wrapper:
  [gradle-wrapper.properties](/data/data/com.termux/files/home/aesolator/app/gradle/wrapper/gradle-wrapper.properties)

Current state:

- root wrapper: `Gradle 8.10.2`
- nested `app/` wrapper: `Gradle 8.10.2`
- repo docs now treat root `./gradlew` as the only authoritative lane

Why this matters:

- there is no longer a silent `8.10.2` vs `8.6` split
- operator discipline is now aligned with one root lane

### 5. Nested `app/gradle.properties` no longer contaminates the root lane

Status: fixed

Proof:

- root project file:
  [gradle.properties](/data/data/com.termux/files/home/aesolator/gradle.properties)
- nested file:
  [gradle.properties](/data/data/com.termux/files/home/aesolator/app/gradle.properties)
- live proof from `:app:properties`:
  effective `org.gradle.jvmargs` is now
  `-Xmx8192m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8`

Current state:

- module-local `app/gradle.properties` is intentionally neutralized
- obsolete `-XX:MaxPermSize=512m` no longer leaks into the live lane

Why this matters:

- the root lane is once again centralized and inspectable
- obsolete JVM args are gone from effective build properties

## Remaining Findings

### 6. `local.properties` remains host-bound bootstrap input

Status: accepted residual

Proof:

- [local.properties](/data/data/com.termux/files/home/aesolator/local.properties)
- [bootstrap-termux-host.sh](/data/data/com.termux/files/home/aesolator/tools/bootstrap-termux-host.sh)

Current state:

- `sdk.dir` is still local-host state
- this is acceptable as bootstrap input
- the important correction is that repo-local build logic no longer treats it
  as justification for broad eager configuration failure

### 7. Gradle 9 deprecation is real, but it is not repo-local build logic

Status: external/plugin residual

Proof:

- `./gradlew :app:help --no-configuration-cache --warning-mode all -Dorg.gradle.deprecation.trace=true`

Trace owner:

- `org.jetbrains.kotlin.gradle.plugin.internal.CompatibilityConventionRegistrarG81`
- `org.jetbrains.kotlin.gradle.plugin.sources.android.configurator.GradleConventionAddKotlinSourcesToAndroidSourceSetConfigurator`

Current state:

- the warning is real
- the source is the Kotlin Android plugin compatibility layer
- it is not caused by a direct repo-local `Convention` API use in our build
  script

### 8. Bare Termux shells no longer need a manual `aapt2` export

Status: fixed

Proof:

- `./gradlew --no-daemon assembleDebug`
  succeeds on this host
- `. ./tools/env-android-local.sh && ./gradlew --no-daemon assembleDebug`
  succeeds

Current state:

- root `gradlew` now auto-wires
  `-Dorg.gradle.project.android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
  when the local Termux `aapt2` binary is present
- the documented env script still exports the same override, but the wrapper
  no longer requires users to do that manually just to keep the build alive

## Verification Evidence

Verified after remediation:

- `./gradlew :app:help --configuration-cache --configuration-cache-problems=warn`
  succeeded and stored cache
- `./gradlew :app:properties --configuration-cache --configuration-cache-problems=warn`
  succeeded and stored cache
- `./gradlew :app:preBuild -m`
  succeeded and showed only the deterministic local tasks:
  `verifyBundledImageFsAssets`, `prepareEmbeddedWineAssets`,
  `prepareRuntimeSharedJni`
- `./gradlew --no-daemon assembleDebug`
  succeeded on this host after the wrapper remediation
- `. ./tools/env-android-local.sh && ./gradlew --no-daemon assembleDebug`
  succeeded on this host

## Product-Level Statement

For `Ae.solator` on this Termux/Android host:

- the repository build logic no longer carries the original reproducibility
  defects from this audit
- the authoritative Android lane is now the root `./gradlew`; the documented
  Termux host bootstrap environment remains the richer operator lane, not a
  hard requirement just to survive `aapt2`
- the next frontier is no longer this audit class; it is broader toolchain and
  product verification work above the repaired build entrypoint
