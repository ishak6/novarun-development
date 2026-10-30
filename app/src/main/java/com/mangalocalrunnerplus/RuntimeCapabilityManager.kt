
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
            "Browser JavaScript can run inside a configured WebView.",
            true
        ),

        "JavaScript JSX" to RuntimeCapability(
            "JavaScript JSX",
            RuntimeType.EXTERNAL_RUNTIME,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a JavaScript build tool to transform JSX."
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
            "Requires a compatible PHP runtime."
        ),

        "Python" to RuntimeCapability(
            "Python",
            RuntimeType.INTERPRETER,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires an embedded or packaged Python runtime."
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
            "Requires a compatible JVM runtime and execution environment."
        ),

        "Kotlin" to RuntimeCapability(
            "Kotlin",
            RuntimeType.JVM,
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires Kotlin compilation and a compatible JVM."
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
            "Requires a compatible native C compiler."
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
            "Requires a compatible native C++ compiler."
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
            RuntimeStatus.ADAPTER_PLANNED,
            "Requires a configured database engine such as SQLite, MySQL or PostgreSQL."
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
