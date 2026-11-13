#include <jni.h>
#include <node.h>
#include <algorithm>
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <string>
#include <thread>
#include <vector>
#include <unistd.h>
#include <fcntl.h>

namespace {
constexpr size_t kMaxCapturedLogBytes = 2U * 1024U * 1024U;
const std::string kTruncatedMarker = "\n[NovaRun] log output truncated after 2 MiB.\n";

bool writeAll(int fd, const char* data, size_t length) {
    size_t offset = 0;
    while (offset < length) {
        const ssize_t written = write(fd, data + offset, length - offset);
        if (written < 0 && errno == EINTR) continue;
        if (written <= 0) return false;
        offset += static_cast<size_t>(written);
    }
    return true;
}

void restoreDescriptor(int saved, int target) {
    if (saved < 0) {
        close(target);
        return;
    }
    if (dup2(saved, target) < 0) close(target);
    close(saved);
}
} // namespace

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

    const std::string logFilePath(log);
    const bool changed = chdir(cwd) == 0;
    if (changed) {
        setenv("NODE_PATH", modules, 1);
        setenv("TMPDIR", temp, 1);
        setenv("HOME", cwd, 1);
        setenv("PORT", port, 1);
        setenv("HOST", "127.0.0.1", 1);
        setenv("NODE_ENV", "development", 1);
    }

    env->ReleaseStringUTFChars(workingDirectory, cwd);
    env->ReleaseStringUTFChars(modulesPath, modules);
    env->ReleaseStringUTFChars(tempDirectory, temp);
    env->ReleaseStringUTFChars(logPath, log);
    env->ReleaseStringUTFChars(portValue, port);

    if (!changed) return 3;

    const jsize count = env->GetArrayLength(arguments);
    if (count < 2 || count > 32) return 4;

    std::vector<std::string> values;
    size_t total = 0;
    for (jsize i = 0; i < count; ++i) {
        auto item = static_cast<jstring>(env->GetObjectArrayElement(arguments, i));
        if (!item) return 5;
        const char* value = env->GetStringUTFChars(item, nullptr);
        if (!value) {
            env->DeleteLocalRef(item);
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

    const int logFd = open(logFilePath.c_str(), O_WRONLY | O_CREAT | O_APPEND | O_CLOEXEC, 0600);
    if (logFd < 0) return 6;

    int outputPipe[2] = {-1, -1};
    if (pipe(outputPipe) != 0) {
        close(logFd);
        return 7;
    }

    const int savedStdout = dup(STDOUT_FILENO);
    const int savedStderr = dup(STDERR_FILENO);
    if (savedStdout < 0 || savedStderr < 0 ||
        dup2(outputPipe[1], STDOUT_FILENO) < 0 ||
        dup2(outputPipe[1], STDERR_FILENO) < 0) {
        restoreDescriptor(savedStdout, STDOUT_FILENO);
        restoreDescriptor(savedStderr, STDERR_FILENO);
        close(outputPipe[0]);
        close(outputPipe[1]);
        close(logFd);
        return 8;
    }
    close(outputPipe[1]);

    std::thread logReader;
    try {
        logReader = std::thread([readFd = outputPipe[0], logFd]() {
            const size_t markerSize = kTruncatedMarker.size();
            const size_t payloadLimit = kMaxCapturedLogBytes - markerSize;
            off_t existing = lseek(logFd, 0, SEEK_END);
            size_t captured = existing > 0 ? static_cast<size_t>(existing) : 0U;
            bool canWrite = true;
            bool truncated = false;

            if (captured > payloadLimit) {
                if (ftruncate(logFd, static_cast<off_t>(payloadLimit)) == 0) {
                    captured = payloadLimit;
                }
                truncated = true;
            }

            char chunk[8192];
            while (true) {
                const ssize_t countRead = read(readFd, chunk, sizeof(chunk));
                if (countRead < 0 && errno == EINTR) continue;
                if (countRead <= 0) break;

                const size_t count = static_cast<size_t>(countRead);
                const size_t remaining = captured < payloadLimit ? payloadLimit - captured : 0U;
                const size_t allowed = std::min(remaining, count);
                if (allowed > 0 && canWrite) {
                    if (writeAll(logFd, chunk, allowed)) {
                        captured += allowed;
                    } else {
                        canWrite = false;
                        truncated = true;
                    }
                }
                if (allowed < count || !canWrite) truncated = true;
            }

            if (truncated && canWrite) {
                writeAll(logFd, kTruncatedMarker.data(), markerSize);
            }
            close(readFd);
            close(logFd);
        });
    } catch (...) {
        restoreDescriptor(savedStdout, STDOUT_FILENO);
        restoreDescriptor(savedStderr, STDERR_FILENO);
        close(outputPipe[0]);
        close(logFd);
        return 9;
    }

    const int argc = static_cast<int>(argv.size());
    argv.push_back(nullptr); // Node's embedder entry point expects argv[argc] to be null.
    const int result = node::Start(argc, argv.data());

    fflush(stdout);
    fflush(stderr);
    restoreDescriptor(savedStdout, STDOUT_FILENO);
    restoreDescriptor(savedStderr, STDERR_FILENO);
    logReader.join();
    return result;
}
