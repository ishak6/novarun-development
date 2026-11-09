#include <jni.h>
#include <node.h>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>
#include <unistd.h>
#include <fcntl.h>

extern "C" JNIEXPORT jint JNICALL
Java_com_mangalocalrunnerplus_NodeNativeBridge_startNodeWithArguments(
    JNIEnv* env, jobject, jobjectArray arguments, jstring workingDirectory,
    jstring modulesPath, jstring tempDirectory, jstring logPath, jstring portValue) {
    if (!arguments || !workingDirectory || !modulesPath || !tempDirectory || !logPath || !portValue) return 2;

    const char* cwd = env->GetStringUTFChars(workingDirectory, nullptr);
    const char* modules = env->GetStringUTFChars(modulesPath, nullptr);
    const char* temp = env->GetStringUTFChars(tempDirectory, nullptr);
    const char* log = env->GetStringUTFChars(logPath, nullptr);
    const char* port = env->GetStringUTFChars(portValue, nullptr);
    if (!cwd || !modules || !temp || !log || !port) {
        if (cwd) env->ReleaseStringUTFChars(workingDirectory, cwd);
        if (modules) env->ReleaseStringUTFChars(modulesPath, modules);
        if (temp) env->ReleaseStringUTFChars(tempDirectory, temp);
        if (log) env->ReleaseStringUTFChars(logPath, log);
        if (port) env->ReleaseStringUTFChars(portValue, port);
        return 2;
    }

    const bool changed = chdir(cwd) == 0;
    int logFd = -1;
    if (changed) {
        setenv("NODE_PATH", modules, 1);
        setenv("TMPDIR", temp, 1);
        setenv("HOME", cwd, 1);
        setenv("PORT", port, 1);
        logFd = open(log, O_WRONLY | O_CREAT | O_APPEND, 0600);
    }

    env->ReleaseStringUTFChars(workingDirectory, cwd);
    env->ReleaseStringUTFChars(modulesPath, modules);
    env->ReleaseStringUTFChars(tempDirectory, temp);
    env->ReleaseStringUTFChars(logPath, log);
    env->ReleaseStringUTFChars(portValue, port);

    if (!changed) {
        if (logFd >= 0) close(logFd);
        return 3;
    }

    const jsize count = env->GetArrayLength(arguments);
    if (count < 2 || count > 32) {
        if (logFd >= 0) close(logFd);
        return 4;
    }

    std::vector<std::string> values;
    size_t total = 0;
    for (jsize i = 0; i < count; ++i) {
        auto item = static_cast<jstring>(env->GetObjectArrayElement(arguments, i));
        if (!item) {
            if (logFd >= 0) close(logFd);
            return 5;
        }
        const char* value = env->GetStringUTFChars(item, nullptr);
        if (!value) {
            env->DeleteLocalRef(item);
            if (logFd >= 0) close(logFd);
            return 5;
        }
        values.emplace_back(value);
        total += values.back().size() + 1;
        env->ReleaseStringUTFChars(item, value);
        env->DeleteLocalRef(item);
    }

    std::vector<char> buffer(total, '\0');
    std::vector<char*> argv;
    size_t offset = 0;
    for (const auto& value : values) {
        argv.push_back(buffer.data() + offset);
        std::memcpy(buffer.data() + offset, value.c_str(), value.size() + 1);
        offset += value.size() + 1;
    }

    if (logFd >= 0) {
        dup2(logFd, STDOUT_FILENO);
        dup2(logFd, STDERR_FILENO);
        close(logFd);
    }
    return node::Start(static_cast<int>(argv.size()), argv.data());
}
