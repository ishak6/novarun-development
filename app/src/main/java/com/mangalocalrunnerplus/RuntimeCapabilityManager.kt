
package com.mangalocalrunnerplus

enum class RuntimeType {
    WEB_VIEW,
    INTERPRETER,
    COMPILER,
    JVM,
    DATABASE,
    DOTNET,
    EXTERNAL_RUNTIME,
    NOT_SUPPORTED
}

enum class RuntimeStatus {
    AVAILABLE_FOR_PREVIEW,
    ADAPTER_PLANNED,
    NOT_CONFIGURED,
    NOT_SUPPORTED
}

data class RuntimeCapability(
    val language: String,
    val runtimeType: RuntimeType,
    val status: RuntimeStatus,
    val description: String,
    val canPreviewInWebView: Boolean = false
)

object RuntimeCapabilityManager {

    private val capabilities = mapOf(
        "HTML" to RuntimeCapability(
            "HTML",
            RuntimeType.WEB_VIEW,
            RuntimeStatus.AVAILABLE_FOR_PREVIEW,
            "HTML pages can be displayed in an Android WebView.",
            true
        ),

        "CSS" to RuntimeCapability(
            "CSS",
            RuntimeType.WEB_VIEW,
            RuntimeStatus.AVAILABLE_FOR_PREVIEW,
            "CSS can be previewed with an HTML page.",
            true
        ),

        "JavaScript" to RuntimeCapability(
            "JavaScript",
            RuntimeType.WEB_VIEW,
            RuntimeStatus.AVAILABLE_FOR_PREVIEW,
            "JavaScript files can run with the embedded interpreter; browser scripts can also be previewed in WebView.",
            true
        ),

        "JavaScript JSX" to RuntimeCapability(
            "JavaScript JSX",
            RuntimeType.EXTERNAL_RUNTIME,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a JavaScript build tool to transform JSX."
        ),

        "Node.js" to RuntimeCapability(
            "Node.js",
            RuntimeType.EXTERNAL_RUNTIME,
            RuntimeStatus.ADAPTER_PLANNED,
            "Not executable yet: no maintained Android Node.js runtime is bundled. The legacy Node 18 mobile line is end-of-life; Node APIs and built-in modules are not provided by the Rhino JavaScript interpreter."
        ),

        "npm" to RuntimeCapability(
            "npm",
            RuntimeType.EXTERNAL_RUNTIME,
            RuntimeStatus.ADAPTER_PLANNED,
            "Not executable yet: npm requires a supported Node.js runtime, package lifecycle scripts, registry/network handling, storage quotas, and an Android-compatible native-addon policy."
        ),

        "TypeScript" to RuntimeCapability(
            "TypeScript",
            RuntimeType.EXTERNAL_RUNTIME,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a TypeScript compiler or build tool."
        ),

        "TypeScript JSX" to RuntimeCapability(
            "TypeScript JSX",
            RuntimeType.EXTERNAL_RUNTIME,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a TypeScript and JSX build pipeline."
        ),

        "PHP" to RuntimeCapability(
            "PHP",
            RuntimeType.INTERPRETER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Not executable yet: no PHP interpreter is bundled. PHP CLI must be cross-compiled for Android ABI and integrated with a maintained runtime/JNI bridge; a static library alone cannot run scripts."
        ),

        "Python" to RuntimeCapability(
            "Python",
            RuntimeType.INTERPRETER,
            RuntimeStatus.AVAILABLE_FOR_PREVIEW,
            "Embedded CPython 3.11 runtime (Chaquopy); standard library support with Android platform limitations."
        ),

        "Ruby" to RuntimeCapability(
            "Ruby",
            RuntimeType.INTERPRETER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a Ruby interpreter."
        ),

        "Java" to RuntimeCapability(
            "Java",
            RuntimeType.JVM,
            RuntimeStatus.ADAPTER_PLANNED,
            "Not executable yet: Android does not provide a general-purpose JVM for arbitrary Java source. A Java compiler plus JVM-bytecode-to-Dex conversion and isolated class loading are required."
        ),

        "Kotlin" to RuntimeCapability(
            "Kotlin",
            RuntimeType.JVM,
            RuntimeStatus.ADAPTER_PLANNED,
            "Not executable yet: Kotlin source compilation requires the Kotlin compiler, dependency resolution, JVM-bytecode-to-Dex conversion, and controlled Android class loading."
        ),

        "Kotlin Script" to RuntimeCapability(
            "Kotlin Script",
            RuntimeType.JVM,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a Kotlin scripting environment."
        ),

        "Go" to RuntimeCapability(
            "Go",
            RuntimeType.COMPILER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a compatible Go compiler."
        ),

        "C#" to RuntimeCapability(
            "C#",
            RuntimeType.DOTNET,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a compatible .NET runtime or compiler."
        ),

        "Dart" to RuntimeCapability(
            "Dart",
            RuntimeType.COMPILER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a Dart SDK or compatible runtime."
        ),

        "C" to RuntimeCapability(
            "C",
            RuntimeType.COMPILER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Not executable yet: no Android-hosted C compiler is bundled. A target-compatible compiler, sysroot, linker, and execution policy are required."
        ),

        "C Header" to RuntimeCapability(
            "C Header",
            RuntimeType.COMPILER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Header files require a compatible C compiler."
        ),

        "C++" to RuntimeCapability(
            "C++",
            RuntimeType.COMPILER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Not executable yet: no Android-hosted C++ compiler is bundled. A target-compatible compiler, C++ standard library, sysroot, linker, and execution policy are required."
        ),

        "C++ Header" to RuntimeCapability(
            "C++ Header",
            RuntimeType.COMPILER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Header files require a compatible C++ compiler."
        ),

        "Rust" to RuntimeCapability(
            "Rust",
            RuntimeType.COMPILER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires the Rust compiler and target toolchain."
        ),

        "Swift" to RuntimeCapability(
            "Swift",
            RuntimeType.COMPILER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a compatible Swift compiler and target toolchain."
        ),

        "Lua" to RuntimeCapability(
            "Lua",
            RuntimeType.INTERPRETER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a Lua interpreter."
        ),

        "Perl" to RuntimeCapability(
            "Perl",
            RuntimeType.INTERPRETER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a Perl interpreter."
        ),

        "Haskell" to RuntimeCapability(
            "Haskell",
            RuntimeType.COMPILER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a compatible Haskell compiler."
        ),

        "R" to RuntimeCapability(
            "R",
            RuntimeType.INTERPRETER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires an R runtime."
        ),

        "Julia" to RuntimeCapability(
            "Julia",
            RuntimeType.INTERPRETER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a Julia runtime."
        ),

        "SQL" to RuntimeCapability(
            "SQL",
            RuntimeType.DATABASE,
            RuntimeStatus.AVAILABLE_FOR_PREVIEW,
            "A registered SQLite adapter runs project SQL scripts against an isolated, persistent database on this device."
        ),

        "Bash" to RuntimeCapability(
            "Bash",
            RuntimeType.INTERPRETER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a compatible shell environment."
        )
    )

    fun getCapability(language: String): RuntimeCapability {
        return capabilities[language]
            ?: RuntimeCapability(
                language = language,
                runtimeType = RuntimeType.NOT_SUPPORTED,
                status = RuntimeStatus.NOT_SUPPORTED,
                description = "No runtime adapter is registered for this language."
            )
    }

    fun getAllCapabilities(): List<RuntimeCapability> {
        return capabilities.values.toList()
    }

    fun getPreviewableLanguages(): List<RuntimeCapability> {
        return capabilities.values.filter {
            it.canPreviewInWebView
        }
    }

    fun getPlannedAdapters(): List<RuntimeCapability> {
        return capabilities.values.filter {
            it.status == RuntimeStatus.ADAPTER_PLANNED
        }
    }
}
