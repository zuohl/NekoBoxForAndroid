import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder
import org.gradle.api.file.DirectoryProperty

plugins {
    id("com.android.library")
}

val generatedJniLibsDir = layout.buildDirectory.dir("generated/jniLibs")

android {
    namespace = "bpfmatcher"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    androidComponents {
        beforeVariants(selector().all()) { variant ->
            variant.enableAndroidTest = false
            (variant as? HasHostTestsBuilder)
                ?.hostTests
                ?.get(HostTestBuilder.UNIT_TEST_TYPE)
                ?.enable = false
        }

        onVariants { variant ->
            variant.sources.jniLibs?.addStaticSourceDirectory("build/generated/jniLibs")
        }
    }

    lint {
        disable += "ChromeOsAbiSupport"
    }
}

val buildHelper = tasks.register<BuildNativeHelperTask>("buildBpfMatcher") {
    sourceFile.set(layout.projectDirectory.file("src/main/native/bpf-matcher.c"))
    outputName.set("libbpf-matcher.so")
    outputDirectory.set(generatedJniLibsDir)
    rootProject.layout.projectDirectory.file("local.properties")
        .takeIf { it.asFile.exists() }
        ?.let(localPropertiesFile::set)
    minSdk.set(21)
    targetAbis.set(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
}

tasks.named("preBuild") {
    dependsOn(buildHelper)
}