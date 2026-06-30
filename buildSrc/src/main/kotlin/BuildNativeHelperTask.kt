import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import java.util.Properties
import javax.inject.Inject

/**
 * Compiles a single C source into a PIE executable shipped as a jniLibs `.so`
 * (Android treats any `lib*.so` under jniLibs as an extractable native library,
 * which `useLegacyPackaging` then places on disk so it can be chmod +x'd at
 * runtime). Ported from AsteriskNG's BuildSetuidgidTask/BuildBpfMatcherTask and
 * generalized for all native root helpers.
 */
abstract class BuildNativeHelperTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:Input
    abstract val outputName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Optional
    @get:InputFile
    abstract val localPropertiesFile: RegularFileProperty

    @get:Input
    abstract val minSdk: Property<Int>

    @get:Input
    abstract val targetAbis: ListProperty<String>

    @TaskAction
    fun build() {
        val source = sourceFile.get().asFile
        val ndkDir = findNdkDir()
        val outputDir = outputDirectory.get().asFile
        outputDir.mkdirs()
        val outName = outputName.get()
        targetAbis.get().map { abi -> abi.toNativeHelperAbiTarget() }.forEach { target ->
            val output = outputDir.resolve("${target.androidAbi}/$outName")
            output.parentFile.mkdirs()
            execOperations.exec {
                commandLine(
                    findNdkClang(ndkDir, target).absolutePath,
                    "-O2",
                    "-Wall",
                    "-Wextra",
                    "-fPIE",
                    "-pie",
                    source.absolutePath,
                    "-o",
                    output.absolutePath,
                )
            }
            if (!output.exists() || output.length() <= 0) {
                throw GradleException("Failed to build $outName: ${output.absolutePath}")
            }
        }
    }

    private fun findNdkClang(ndkDir: File, target: NativeHelperAbiTarget): File {
        val hostTag = when {
            System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> "windows-x86_64"
            System.getProperty("os.name").contains("Mac", ignoreCase = true) -> "darwin-x86_64"
            else -> "linux-x86_64"
        }
        val executableName = if (hostTag.startsWith("windows")) {
            "${target.clangTarget}${minSdk.get()}-clang.cmd"
        } else {
            "${target.clangTarget}${minSdk.get()}-clang"
        }
        val clang = ndkDir.resolve("toolchains/llvm/prebuilt/$hostTag/bin/$executableName")
        if (!clang.exists()) {
            throw GradleException("Android NDK clang not found: ${clang.absolutePath}")
        }
        return clang
    }

    private fun findNdkDir(): File {
        listOf("ANDROID_NDK_HOME", "ANDROID_NDK_ROOT").forEach { name ->
            System.getenv(name)?.takeIf { it.isNotBlank() }?.let { path -> return File(path) }
        }
        val localProperties = localPropertiesFile.orNull?.asFile
        if (localProperties != null && localProperties.exists()) {
            val properties = Properties()
            localProperties.inputStream().use(properties::load)
            properties.getProperty("ndk.dir")?.takeIf { it.isNotBlank() }?.let { path -> return File(path) }
            properties.getProperty("sdk.dir")?.takeIf { it.isNotBlank() }?.let { path ->
                File(path, "ndk").latestChildDirectory()?.let { return it }
            }
        }
        listOf("ANDROID_HOME", "ANDROID_SDK_ROOT").forEach { name ->
            System.getenv(name)?.takeIf { it.isNotBlank() }?.let { path ->
                File(path, "ndk").latestChildDirectory()?.let { return it }
            }
        }
        throw GradleException("Android NDK not found. Set ndk.dir, ANDROID_NDK_HOME, or install an NDK under the Android SDK.")
    }

    private fun File.latestChildDirectory(): File? {
        return listFiles()?.filter(File::isDirectory)?.maxByOrNull { directory -> directory.name }
    }

    enum class NativeHelperAbiTarget(
        val androidAbi: String,
        val clangTarget: String,
    ) {
        Arm64("arm64-v8a", "aarch64-linux-android"),
        Arm32("armeabi-v7a", "armv7a-linux-androideabi"),
        X86("x86", "i686-linux-android"),
        X64("x86_64", "x86_64-linux-android");
    }

    private fun String.toNativeHelperAbiTarget(): NativeHelperAbiTarget {
        return NativeHelperAbiTarget.values().firstOrNull { target -> target.androidAbi == this }
            ?: throw GradleException("Unsupported native helper ABI: $this")
    }
}