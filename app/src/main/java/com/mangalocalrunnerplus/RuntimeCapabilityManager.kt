package com.mangalocalrunnerplus

enum class RuntimeType {
    WEB_VIEW, INTERPRETER, COMPILER, JVM, DATABASE, DOTNET, EXTERNAL_RUNTIME, NOT_SUPPORTED
}

enum class RuntimeStatus {
    AVAILABLE_FOR_EXECUTION,
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
    private val capabilities = listOf(
        RuntimeCapability("HTML", RuntimeType.WEB_VIEW, RuntimeStatus.AVAILABLE_FOR_PREVIEW, "HTML pages can be displayed in the native project preview.", true),
        RuntimeCapability("CSS", RuntimeType.WEB_VIEW, RuntimeStatus.AVAILABLE_FOR_PREVIEW, "CSS stylesheets can be previewed with an HTML project.", true),
        RuntimeCapability("SVG", RuntimeType.WEB_VIEW, RuntimeStatus.AVAILABLE_FOR_PREVIEW, "SVG vector assets can be served and previewed.", true),
        RuntimeCapability("JavaScript", RuntimeType.INTERPRETER, RuntimeStatus.AVAILABLE_FOR_EXECUTION, "Browser JavaScript runs in project preview; standalone .js uses the bounded Rhino ES6 interpreter. This is not Node.js.", true),
        RuntimeCapability("JavaScript JSX", RuntimeType.EXTERNAL_RUNTIME, RuntimeStatus.ADAPTER_PLANNED, "Requires a JavaScript build tool to transform JSX."),
        RuntimeCapability("Node.js", RuntimeType.EXTERNAL_RUNTIME, RuntimeStatus.ADAPTER_PLANNED, "Not executable yet: no maintained, tested Android Node.js runtime is bundled. Rhino does not provide Node APIs or built-in modules."),
        RuntimeCapability("npm", RuntimeType.EXTERNAL_RUNTIME, RuntimeStatus.ADAPTER_PLANNED, "Not executable yet: requires a working Node.js runtime, package lifecycle consent, registry handling, storage quotas, and native-addon compatibility policy."),
        RuntimeCapability("TypeScript", RuntimeType.EXTERNAL_RUNTIME, RuntimeStatus.ADAPTER_PLANNED, "Requires a TypeScript compiler or build tool."),
        RuntimeCapability("TypeScript JSX", RuntimeType.EXTERNAL_RUNTIME, RuntimeStatus.ADAPTER_PLANNED, "Requires a TypeScript and JSX build pipeline."),
        RuntimeCapability("PHP", RuntimeType.INTERPRETER, RuntimeStatus.ADAPTER_PLANNED, "Not executable yet: requires a maintained PHP CLI build for supported Android ABIs and a tested process integration."),
        RuntimeCapability("Python", RuntimeType.INTERPRETER, RuntimeStatus.AVAILABLE_FOR_EXECUTION, "Embedded CPython 3.11 via Chaquopy. Standard library support has Android-specific limitations."),
        RuntimeCapability("Ruby", RuntimeType.INTERPRETER, RuntimeStatus.ADAPTER_PLANNED, "Requires a Ruby interpreter."),
        RuntimeCapability("Java", RuntimeType.JVM, RuntimeStatus.ADAPTER_PLANNED, "Requires a Java compiler, JVM-bytecode-to-Dex conversion, dependency resolution, and controlled Android class loading."),
        RuntimeCapability("Kotlin", RuntimeType.JVM, RuntimeStatus.ADAPTER_PLANNED, "Requires the Kotlin compiler, dependency resolution, JVM-bytecode-to-Dex conversion, and controlled Android class loading."),
        RuntimeCapability("Kotlin Script", RuntimeType.JVM, RuntimeStatus.ADAPTER_PLANNED, "Requires a Kotlin scripting environment."),
        RuntimeCapability("Go", RuntimeType.COMPILER, RuntimeStatus.ADAPTER_PLANNED, "Requires a compatible Go compiler."),
        RuntimeCapability("C#", RuntimeType.DOTNET, RuntimeStatus.ADAPTER_PLANNED, "Requires a compatible .NET runtime or compiler."),
        RuntimeCapability("Dart", RuntimeType.COMPILER, RuntimeStatus.ADAPTER_PLANNED, "Requires a Dart SDK or compatible runtime."),
        RuntimeCapability("C", RuntimeType.COMPILER, RuntimeStatus.ADAPTER_PLANNED, "Requires an Android-hosted compiler, target sysroot, linker, and bounded execution environment."),
        RuntimeCapability("C Header", RuntimeType.COMPILER, RuntimeStatus.ADAPTER_PLANNED, "Header files require a compatible C compiler."),
        RuntimeCapability("C++", RuntimeType.COMPILER, RuntimeStatus.ADAPTER_PLANNED, "Requires an Android-hosted C++ compiler, standard library, target sysroot, linker, and bounded execution environment."),
        RuntimeCapability("C++ Header", RuntimeType.COMPILER, RuntimeStatus.ADAPTER_PLANNED, "Header files require a compatible C++ compiler."),
        RuntimeCapability("Rust", RuntimeType.COMPILER, RuntimeStatus.ADAPTER_PLANNED, "Requires the Rust compiler and target toolchain."),
        RuntimeCapability("Swift", RuntimeType.COMPILER, RuntimeStatus.ADAPTER_PLANNED, "Requires a compatible Swift compiler and target toolchain."),
        RuntimeCapability("Lua", RuntimeType.INTERPRETER, RuntimeStatus.ADAPTER_PLANNED, "Requires a Lua interpreter."),
        RuntimeCapability("Perl", RuntimeType.INTERPRETER, RuntimeStatus.ADAPTER_PLANNED, "Requires a Perl interpreter."),
        RuntimeCapability("Haskell", RuntimeType.COMPILER, RuntimeStatus.ADAPTER_PLANNED, "Requires a compatible Haskell compiler."),
        RuntimeCapability("R", RuntimeType.INTERPRETER, RuntimeStatus.ADAPTER_PLANNED, "Requires an R runtime."),
        RuntimeCapability("Julia", RuntimeType.INTERPRETER, RuntimeStatus.ADAPTER_PLANNED, "Requires a Julia runtime."),
        RuntimeCapability("SQL", RuntimeType.DATABASE, RuntimeStatus.AVAILABLE_FOR_EXECUTION, "Project-isolated SQLite database; SQL scripts execute locally with statement and output limits."),
        RuntimeCapability("Bash", RuntimeType.INTERPRETER, RuntimeStatus.ADAPTER_PLANNED, "Requires a compatible shell environment.")
    ).associateBy { it.language }

    fun getCapability(language: String): RuntimeCapability =
        capabilities[language] ?: RuntimeCapability(
            language, RuntimeType.NOT_SUPPORTED, RuntimeStatus.NOT_SUPPORTED,
            "No runtime adapter is registered for this language."
        )

    fun getAllCapabilities(): List<RuntimeCapability> = capabilities.values.toList()

    fun getPreviewableLanguages(): List<RuntimeCapability> =
        capabilities.values.filter { it.canPreviewInWebView }

    fun getPlannedAdapters(): List<RuntimeCapability> =
        capabilities.values.filter { it.status == RuntimeStatus.ADAPTER_PLANNED }
}
