#define NBWV2_HOST_EXPORTS

#include "nb_webview2_host.h"

#include <bcrypt.h>
#include <wrl.h>
#include <WebView2.h>
#include <WebView2EnvironmentOptions.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstddef>
#include <deque>
#include <filesystem>
#include <fstream>
#include <functional>
#include <limits>
#include <memory>
#include <mutex>
#include <new>
#include <string>
#include <thread>
#include <unordered_map>
#include <utility>
#include <vector>

using Microsoft::WRL::Callback;
using Microsoft::WRL::ComPtr;

namespace {

constexpr UINT kDispatchMessage = WM_APP + 0x4E42;
constexpr UINT kShutdownMessage = WM_APP + 0x4E43;
constexpr size_t kMaximumQueuedCommands = 64;
constexpr size_t kMaximumJsonCharacters = 1'500'000;
constexpr size_t kMaximumDiagnosticCharacters = 2'048;
constexpr size_t kMaximumPathCharacters = 32'000;
constexpr size_t kMaximumResources = 128;
constexpr uint32_t kMaximumSynchronousTimeoutMilliseconds = 120'000;
constexpr uintmax_t kMaximumResourceBytes = 64ull * 1024ull * 1024ull;
constexpr uintmax_t kMaximumSnapshotBytes = 128ull * 1024ull * 1024ull;
constexpr wchar_t kMinimumCompatibleRuntime[] = L"100.0.1185.39";
constexpr wchar_t kParkingWindowClass[] =
        L"NetBeansFlutterWebView2ParkingWindowV3";
constexpr wchar_t kCsp[] =
        L"default-src 'none'; "
        L"base-uri 'none'; "
        L"connect-src 'self'; "
        L"font-src 'self'; "
        L"form-action 'none'; "
        L"frame-ancestors 'none'; "
        L"img-src 'self' data: blob:; "
        L"manifest-src 'none'; "
        L"media-src 'none'; "
        L"object-src 'none'; "
        L"script-src 'self' 'wasm-unsafe-eval'; "
        L"style-src 'self' 'unsafe-inline'; "
        L"worker-src 'none'";

LRESULT CALLBACK parking_window_proc(
        HWND window, UINT message, WPARAM wparam, LPARAM lparam) noexcept {
    return DefWindowProcW(window, message, wparam, lparam);
}

HRESULT create_parking_window(HWND* result) noexcept {
    if (result == nullptr) {
        return E_POINTER;
    }
    *result = nullptr;
    const HINSTANCE instance = GetModuleHandleW(nullptr);
    WNDCLASSW window_class = {};
    window_class.lpfnWndProc = parking_window_proc;
    window_class.hInstance = instance;
    window_class.lpszClassName = kParkingWindowClass;
    if (RegisterClassW(&window_class) == 0) {
        const DWORD error = GetLastError();
        if (error != ERROR_CLASS_ALREADY_EXISTS) {
            return HRESULT_FROM_WIN32(error);
        }
    }
    HWND window = CreateWindowExW(
            WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE,
            kParkingWindowClass,
            L"",
            WS_POPUP,
            0,
            0,
            1,
            1,
            nullptr,
            nullptr,
            instance,
            nullptr);
    if (window == nullptr) {
        return HRESULT_FROM_WIN32(GetLastError());
    }
    if (GetParent(window) != nullptr || IsWindowVisible(window) ||
            (GetWindowLongPtrW(window, GWL_STYLE) & WS_CHILD) != 0) {
        DestroyWindow(window);
        return HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
    }
    *result = window;
    return S_OK;
}

std::wstring bounded(const wchar_t* value, size_t maximum) {
    if (value == nullptr) {
        return {};
    }
    const size_t length = wcsnlen_s(value, maximum + 1);
    if (length > maximum) {
        return {};
    }
    return std::wstring(value, length);
}

bool valid_nonce(const std::wstring& value) {
    if (value.size() != 64) {
        return false;
    }
    return std::all_of(value.begin(), value.end(), [](wchar_t ch) {
        return (ch >= L'0' && ch <= L'9') || (ch >= L'a' && ch <= L'f');
    });
}

bool valid_host(const std::wstring& value) {
    constexpr wchar_t suffix[] = L".invalid";
    if (value.size() < 10 || value.size() > 253 ||
            value.compare(value.size() - 8, 8, suffix) != 0 ||
            value.front() == L'-') {
        return false;
    }
    bool previous_hyphen = false;
    for (wchar_t ch : value) {
        if (!((ch >= L'a' && ch <= L'z') ||
                (ch >= L'0' && ch <= L'9') || ch == L'-' || ch == L'.')) {
            return false;
        }
        if (ch == L'.' && previous_hyphen) {
            return false;
        }
        previous_hyphen = ch == L'-';
    }
    return !previous_hyphen;
}

bool valid_portable_path(const std::wstring& value) {
    if (value.empty() || value.size() > 1'024 || value.front() == L'/' ||
            value.back() == L'/' || value.find(L'\\') != std::wstring::npos ||
            value.find(L':') != std::wstring::npos ||
            value.find(L'?') != std::wstring::npos ||
            value.find(L'#') != std::wstring::npos ||
            value.find(L'%') != std::wstring::npos) {
        return false;
    }
    size_t start = 0;
    while (start < value.size()) {
        const size_t end = value.find(L'/', start);
        const std::wstring component = value.substr(
                start, end == std::wstring::npos ? std::wstring::npos : end - start);
        if (component.empty() || component == L"." || component == L"..") {
            return false;
        }
        if (!std::all_of(component.begin(), component.end(), [](wchar_t ch) {
                return (ch >= L'a' && ch <= L'z') ||
                        (ch >= L'A' && ch <= L'Z') ||
                        (ch >= L'0' && ch <= L'9') ||
                        ch == L'.' || ch == L'_' || ch == L'-';
            })) {
            return false;
        }
        start = end == std::wstring::npos ? value.size() : end + 1;
    }
    return true;
}

struct ResourceSnapshot {
    std::wstring relative_path;
    std::vector<uint8_t> bytes;
};

const wchar_t* content_type(const std::wstring& path);

bool parse_decimal_size(const std::wstring& value, uintmax_t* result) {
    if (result == nullptr || value.empty() ||
            (value.size() > 1 && value.front() == L'0')) {
        return false;
    }
    uintmax_t parsed = 0;
    for (wchar_t ch : value) {
        if (ch < L'0' || ch > L'9' ||
                parsed > (std::numeric_limits<uintmax_t>::max() -
                        static_cast<uintmax_t>(ch - L'0')) / 10) {
            return false;
        }
        parsed = parsed * 10 + static_cast<uintmax_t>(ch - L'0');
    }
    if (parsed > kMaximumResourceBytes) {
        return false;
    }
    *result = parsed;
    return true;
}

bool valid_sha256(const std::wstring& value) {
    return value.size() == 64 &&
            std::all_of(value.begin(), value.end(), [](wchar_t ch) {
                return (ch >= L'0' && ch <= L'9') ||
                        (ch >= L'a' && ch <= L'f');
            });
}

bool sha256_bytes(const std::vector<uint8_t>& bytes, std::wstring* result) {
    if (result == nullptr) {
        return false;
    }
    result->clear();
    BCRYPT_ALG_HANDLE algorithm = nullptr;
    BCRYPT_HASH_HANDLE hash = nullptr;
    DWORD object_bytes = 0;
    DWORD hash_bytes = 0;
    DWORD returned = 0;
    NTSTATUS status = BCryptOpenAlgorithmProvider(
            &algorithm, BCRYPT_SHA256_ALGORITHM, nullptr, 0);
    if (status < 0) {
        return false;
    }
    status = BCryptGetProperty(
            algorithm, BCRYPT_OBJECT_LENGTH,
            reinterpret_cast<PUCHAR>(&object_bytes), sizeof(object_bytes),
            &returned, 0);
    if (status >= 0) {
        status = BCryptGetProperty(
                algorithm, BCRYPT_HASH_LENGTH,
                reinterpret_cast<PUCHAR>(&hash_bytes), sizeof(hash_bytes),
                &returned, 0);
    }
    std::vector<uint8_t> object;
    std::vector<uint8_t> digest;
    if (status >= 0 && object_bytes > 0 && hash_bytes == 32) {
        object.resize(object_bytes);
        digest.resize(hash_bytes);
        status = BCryptCreateHash(
                algorithm, &hash, object.data(), object_bytes,
                nullptr, 0, 0);
    } else if (status >= 0) {
        status = static_cast<NTSTATUS>(0xC000000DL);
    }
    if (status >= 0 && !bytes.empty()) {
        status = BCryptHashData(
                hash, const_cast<PUCHAR>(bytes.data()),
                static_cast<ULONG>(bytes.size()), 0);
    }
    if (status >= 0) {
        status = BCryptFinishHash(hash, digest.data(), hash_bytes, 0);
    }
    if (hash != nullptr) {
        BCryptDestroyHash(hash);
    }
    BCryptCloseAlgorithmProvider(algorithm, 0);
    if (status < 0) {
        return false;
    }
    constexpr wchar_t digits[] = L"0123456789abcdef";
    result->reserve(digest.size() * 2);
    for (uint8_t value : digest) {
        result->push_back(digits[value >> 4]);
        result->push_back(digits[value & 0x0F]);
    }
    return true;
}

bool read_verified_resource(
        const std::filesystem::path& root,
        const std::wstring& relative,
        uintmax_t expected_size,
        const std::wstring& expected_sha256,
        std::vector<uint8_t>* result) {
    if (result == nullptr) {
        return false;
    }
    result->clear();
    const std::filesystem::path file = root / std::filesystem::path(relative);
    const DWORD attributes = GetFileAttributesW(file.c_str());
    if (attributes == INVALID_FILE_ATTRIBUTES ||
            (attributes & (FILE_ATTRIBUTE_DIRECTORY |
                           FILE_ATTRIBUTE_REPARSE_POINT)) != 0) {
        return false;
    }
    std::error_code error;
    const auto actual_size = std::filesystem::file_size(file, error);
    if (error || actual_size != expected_size ||
            actual_size > kMaximumResourceBytes ||
            actual_size > static_cast<uintmax_t>(SIZE_MAX)) {
        return false;
    }
    std::ifstream input(file, std::ios::binary);
    if (!input) {
        return false;
    }
    result->resize(static_cast<size_t>(actual_size));
    if (!result->empty()) {
        input.read(reinterpret_cast<char*>(result->data()),
                   static_cast<std::streamsize>(result->size()));
        if (!input || input.gcount() != static_cast<std::streamsize>(result->size())) {
            result->clear();
            return false;
        }
    }
    if (input.peek() != std::char_traits<char>::eof()) {
        result->clear();
        return false;
    }
    std::wstring actual_sha256;
    if (!sha256_bytes(*result, &actual_sha256) ||
            actual_sha256 != expected_sha256) {
        result->clear();
        return false;
    }
    return true;
}

bool parse_resource_manifest(
        const std::wstring& serialized,
        const std::wstring& origin,
        const std::filesystem::path& root,
        std::unordered_map<std::wstring, ResourceSnapshot>* resources) {
    if (serialized.empty() || resources == nullptr) {
        return false;
    }
    std::wstring previous;
    uintmax_t total_bytes = 0;
    size_t start = 0;
    while (start <= serialized.size()) {
        const size_t end = serialized.find(L'\n', start);
        const std::wstring record = serialized.substr(
                start, end == std::wstring::npos ? std::wstring::npos : end - start);
        const size_t first_separator = record.find(L'|');
        const size_t second_separator = first_separator == std::wstring::npos
                ? std::wstring::npos : record.find(L'|', first_separator + 1);
        if (first_separator == std::wstring::npos ||
                second_separator == std::wstring::npos ||
                record.find(L'|', second_separator + 1) != std::wstring::npos) {
            return false;
        }
        const std::wstring path = record.substr(0, first_separator);
        const std::wstring size_text = record.substr(
                first_separator + 1, second_separator - first_separator - 1);
        const std::wstring expected_sha256 = record.substr(second_separator + 1);
        uintmax_t expected_size = 0;
        if (!valid_portable_path(path) ||
                (!previous.empty() && path <= previous) ||
                !parse_decimal_size(size_text, &expected_size) ||
                !valid_sha256(expected_sha256) ||
                content_type(path) == nullptr ||
                resources->size() >= kMaximumResources ||
                expected_size > kMaximumSnapshotBytes - total_bytes) {
            return false;
        }
        previous = path;
        ResourceSnapshot snapshot;
        snapshot.relative_path = path;
        if (!read_verified_resource(
                root, path, expected_size, expected_sha256, &snapshot.bytes)) {
            return false;
        }
        if (!resources->emplace(
                origin + L"/" + path, std::move(snapshot)).second) {
            return false;
        }
        total_bytes += expected_size;
        if (end == std::wstring::npos) {
            break;
        }
        start = end + 1;
        if (start == serialized.size()) {
            return false;
        }
    }
    return resources->find(origin + L"/index.html") != resources->end();
}

const wchar_t* content_type(const std::wstring& path) {
    if (path.size() >= 8 && path.compare(path.size() - 8, 8, L"/NOTICES") == 0) {
        return L"text/plain; charset=utf-8";
    }
    const size_t dot = path.find_last_of(L'.');
    if (dot == std::wstring::npos) return nullptr;
    std::wstring extension = path.substr(dot);
    std::transform(extension.begin(), extension.end(), extension.begin(),
                   [](wchar_t value) { return static_cast<wchar_t>(towlower(value)); });
    if (extension == L".html") return L"text/html; charset=utf-8";
    if (extension == L".js") return L"text/javascript; charset=utf-8";
    if (extension == L".css") return L"text/css; charset=utf-8";
    if (extension == L".json") return L"application/json; charset=utf-8";
    if (extension == L".wasm") return L"application/wasm";
    if (extension == L".bin") return L"application/octet-stream";
    if (extension == L".frag") return L"application/octet-stream";
    if (extension == L".ttf") return L"font/ttf";
    if (extension == L".otf") return L"font/otf";
    if (extension == L".txt") return L"text/plain; charset=utf-8";
    if (extension == L".symbols") return L"text/plain; charset=utf-8";
    return nullptr;
}

std::wstring response_headers(const wchar_t* type) {
    return L"Content-Type: " + std::wstring(type) + L"\r\n"
            L"Cache-Control: no-store\r\n"
            L"Content-Security-Policy: " + std::wstring(kCsp) + L"\r\n"
            L"Cross-Origin-Embedder-Policy: require-corp\r\n"
            L"Cross-Origin-Opener-Policy: same-origin\r\n"
            L"Cross-Origin-Resource-Policy: same-origin\r\n"
            L"Permissions-Policy: camera=(), geolocation=(), microphone=(), payment=(), usb=()\r\n"
            L"Referrer-Policy: no-referrer\r\n"
            L"X-Content-Type-Options: nosniff\r\n";
}

std::wstring hresult_text(HRESULT status, const wchar_t* operation) {
    wchar_t system[512] = {};
    FormatMessageW(
            FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
            nullptr,
            static_cast<DWORD>(status),
            0,
            system,
            static_cast<DWORD>(std::size(system)),
            nullptr);
    std::wstring message = operation == nullptr ? L"WebView2 operation failed" : operation;
    wchar_t code[24] = {};
    swprintf_s(code, L" (0x%08X)", static_cast<unsigned int>(status));
    message.append(code);
    if (system[0] != L'\0') {
        message.append(L": ");
        message.append(system);
    }
    while (!message.empty() &&
            (message.back() == L'\r' || message.back() == L'\n' ||
             message.back() == L' ')) {
        message.pop_back();
    }
    if (message.size() > kMaximumDiagnosticCharacters) {
        message.resize(kMaximumDiagnosticCharacters);
    }
    return message;
}

HRESULT stream_from_bytes(const std::vector<uint8_t>& bytes, IStream** result) {
    if (result == nullptr) {
        return E_INVALIDARG;
    }
    *result = nullptr;
    ComPtr<IStream> stream;
    HRESULT status = CreateStreamOnHGlobal(nullptr, TRUE, &stream);
    if (FAILED(status)) {
        return status;
    }
    if (!bytes.empty()) {
        ULONG written = 0;
        status = stream->Write(bytes.data(), static_cast<ULONG>(bytes.size()), &written);
        if (FAILED(status) || written != bytes.size()) {
            return FAILED(status) ? status : E_FAIL;
        }
    }
    LARGE_INTEGER zero = {};
    status = stream->Seek(zero, STREAM_SEEK_SET, nullptr);
    if (FAILED(status)) {
        return status;
    }
    *result = stream.Detach();
    return S_OK;
}

class SynchronousOperation final {
public:
    SynchronousOperation() noexcept {
        event_ = CreateEventW(nullptr, TRUE, FALSE, nullptr);
        if (event_ == nullptr) {
            event_status_ = HRESULT_FROM_WIN32(GetLastError());
        }
    }

    ~SynchronousOperation() noexcept {
        if (event_ != nullptr) {
            CloseHandle(event_);
        }
    }

    SynchronousOperation(const SynchronousOperation&) = delete;
    SynchronousOperation& operator=(const SynchronousOperation&) = delete;

    void complete(HRESULT status) noexcept {
        {
            std::lock_guard lock(mutex_);
            if (completed_) {
                return;
            }
            status_ = status;
            completed_ = true;
        }
        if (event_ != nullptr) {
            SetEvent(event_);
        }
    }

    HRESULT wait(uint32_t timeout_milliseconds) noexcept {
        if (event_ == nullptr) {
            return event_status_;
        }
        const ULONGLONG deadline =
                GetTickCount64() + timeout_milliseconds;
        for (;;) {
            {
                std::lock_guard lock(mutex_);
                if (completed_) {
                    return status_;
                }
            }
            const ULONGLONG now = GetTickCount64();
            if (now >= deadline) {
                return HRESULT_FROM_WIN32(ERROR_TIMEOUT);
            }
            const DWORD remaining = static_cast<DWORD>(deadline - now);
            const DWORD wait = MsgWaitForMultipleObjectsEx(
                    1,
                    &event_,
                    remaining,
                    QS_SENDMESSAGE,
                    MWMO_INPUTAVAILABLE);
            if (wait == WAIT_OBJECT_0) {
                continue;
            }
            if (wait == WAIT_OBJECT_0 + 1) {
                // PeekMessage dispatches cross-thread sent messages before it
                // inspects the queue. PM_NOREMOVE deliberately avoids pumping
                // posted/input messages and limits nested UI reentrancy.
                MSG message = {};
                PeekMessageW(&message, nullptr, 0, 0, PM_NOREMOVE);
                continue;
            }
            if (wait == WAIT_TIMEOUT) {
                return HRESULT_FROM_WIN32(ERROR_TIMEOUT);
            }
            return HRESULT_FROM_WIN32(GetLastError());
        }
    }

private:
    HANDLE event_ = nullptr;
    HRESULT event_status_ = S_OK;
    std::mutex mutex_;
    bool completed_ = false;
    HRESULT status_ = E_UNEXPECTED;
};

class Host final {
public:
    Host(
            const nbwv2_create_options& options,
            nbwv2_event_callback callback,
            void* context,
            std::wstring user_data,
            std::wstring host,
            std::wstring nonce,
            std::unordered_map<std::wstring, ResourceSnapshot> resources)
        : parent_(options.parent_window),
          initial_bounds_{options.x, options.y,
                          options.x + options.width, options.y + options.height},
          callback_(callback),
          callback_context_(context),
          user_data_(std::move(user_data)),
          virtual_host_(std::move(host)),
          nonce_(std::move(nonce)),
          origin_(L"https://" + virtual_host_),
          document_url_(origin_ + L"/index.html"),
          resources_(std::move(resources)) {}

    ~Host() = default;
    Host(const Host&) = delete;
    Host& operator=(const Host&) = delete;

    HRESULT start() {
        try {
            thread_ = std::thread([this] { thread_main(); });
        } catch (...) {
            return E_OUTOFMEMORY;
        }
        std::unique_lock lock(start_mutex_);
        if (!start_condition_.wait_for(lock, std::chrono::seconds(10),
                                       [this] { return thread_started_; })) {
            // The std::thread exists, so return its owned handle to the caller
            // even if this machine has not scheduled its entry point within
            // the bounded bootstrap wait. Early commands remain queued and
            // the Java startup deadline retains/destroys the callback safely.
            return S_OK;
        }
        return thread_start_status_;
    }

    HRESULT post(std::function<void()> command) {
        if (!command) {
            return E_INVALIDARG;
        }
        std::lock_guard lock(queue_mutex_);
        if (closing_ || failed_ || commands_.size() >= kMaximumQueuedCommands) {
            return closing_ ? RO_E_CLOSED : HRESULT_FROM_WIN32(ERROR_NOT_ENOUGH_QUOTA);
        }
        commands_.push_back(std::move(command));
        if (thread_id_ == 0) {
            return S_OK;
        }
        if (!PostThreadMessageW(thread_id_, kDispatchMessage, 0, 0)) {
            const DWORD error = GetLastError();
            commands_.pop_back();
            return HRESULT_FROM_WIN32(error);
        }
        return S_OK;
    }

    HRESULT post_message(std::wstring json) {
        if (json.empty() || json.size() > kMaximumJsonCharacters) {
            return E_INVALIDARG;
        }
        return post([this, json = std::move(json)] {
            if (!webview_) {
                emit_failure(E_UNEXPECTED, L"PostWebMessageAsJson before controller readiness");
                return;
            }
            const HRESULT status = webview_->PostWebMessageAsJson(json.c_str());
            if (FAILED(status)) {
                emit_failure(status, L"PostWebMessageAsJson");
            }
        });
    }

    HRESULT set_bounds(RECT bounds) {
        return post([this, bounds] {
            if (controller_) {
                HRESULT status = controller_->put_Bounds(bounds);
                RECT applied = {};
                if (SUCCEEDED(status)) {
                    status = controller_->get_Bounds(&applied);
                }
                if (SUCCEEDED(status) && !EqualRect(&bounds, &applied)) {
                    status = HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
                }
                if (FAILED(status)) {
                    emit_failure(status, L"Apply and verify WebView2 controller bounds");
                }
            } else {
                initial_bounds_ = bounds;
            }
        });
    }

    HRESULT set_visible(bool visible) {
        return post([this, visible] {
            desired_visible_ = visible;
            if (controller_) {
                const bool can_show =
                        !parent_release_fenced_.load(std::memory_order_acquire);
                HRESULT status = controller_->put_IsVisible(
                        can_show && visible ? TRUE : FALSE);
                BOOL applied = FALSE;
                if (SUCCEEDED(status)) {
                    status = controller_->get_IsVisible(&applied);
                }
                if (SUCCEEDED(status)
                        && ((applied != FALSE) != (can_show && visible))) {
                    status = HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
                }
                if (FAILED(status)) {
                    emit_failure(status, L"Apply and verify WebView2 controller visibility");
                }
            }
        });
    }

    HRESULT request_focus() {
        return post([this] {
            if (!controller_ ||
                    parent_release_fenced_.load(std::memory_order_acquire)) {
                return;
            }
            const HRESULT status = controller_->MoveFocus(
                    COREWEBVIEW2_MOVE_FOCUS_REASON_PROGRAMMATIC);
            if (FAILED(status)) {
                emit_failure(status, L"WebView2 controller focus");
            }
        });
    }

    HRESULT prepare_parent_release(
            HWND expected_parent,
            uint32_t timeout_milliseconds) {
        if (expected_parent == nullptr || expected_parent != parent_ ||
                timeout_milliseconds == 0 ||
                timeout_milliseconds > kMaximumSynchronousTimeoutMilliseconds) {
            return E_INVALIDARG;
        }
        parent_release_fenced_.store(true, std::memory_order_release);
        if ((shutdown_flags_.load() & NBWV2_DESTROY_PARENT_RELEASED) != 0) {
            return S_OK;
        }
        auto operation = std::make_shared<SynchronousOperation>();
        const HRESULT queued = post_control(
                [this, expected_parent, operation] {
                    operation->complete(release_parent_on_thread(expected_parent));
                }, false);
        if (FAILED(queued)) {
            if ((shutdown_flags_.load() &
                 NBWV2_DESTROY_PARENT_RELEASED) != 0) {
                return S_OK;
            }
            return queued;
        }
        const HRESULT waited = operation->wait(timeout_milliseconds);
        if (FAILED(waited) &&
                (shutdown_flags_.load() &
                 NBWV2_DESTROY_PARENT_RELEASED) != 0) {
            return S_OK;
        }
        return waited;
    }

    HRESULT destroy(
            uint32_t timeout_milliseconds,
            nbwv2_destroy_result* result) noexcept {
        if (result == nullptr ||
                timeout_milliseconds == 0 ||
                timeout_milliseconds > kMaximumSynchronousTimeoutMilliseconds) {
            return E_INVALIDARG;
        }
        const HRESULT requested = request_shutdown();
        if (FAILED(requested)) {
            terminal_hresult_.store(requested);
            fill_destroy_result(result);
            return requested;
        }
        if (thread_.joinable()) {
            const DWORD wait = WaitForSingleObject(
                    thread_.native_handle(), timeout_milliseconds);
            if (wait == WAIT_TIMEOUT) {
                const HRESULT timeout = HRESULT_FROM_WIN32(ERROR_TIMEOUT);
                terminal_hresult_.store(timeout);
                fill_destroy_result(result);
                return timeout;
            }
            if (wait != WAIT_OBJECT_0) {
                const HRESULT failure = HRESULT_FROM_WIN32(GetLastError());
                terminal_hresult_.store(failure);
                fill_destroy_result(result);
                return failure;
            }
            try {
                thread_.join();
            } catch (...) {
                const HRESULT failure = E_FAIL;
                terminal_hresult_.store(failure);
                fill_destroy_result(result);
                return failure;
            }
        }
        shutdown_flags_.fetch_or(NBWV2_DESTROY_THREAD_JOINED);
        const HRESULT shutdown_failure = shutdown_failure_.load();
        if (FAILED(shutdown_failure)) {
            // The native thread and callback are already retired, so no live
            // handle remains to retry. Release native ownership conservatively
            // as S_FALSE and withhold every UDF-release claim.
            shutdown_flags_.fetch_and(
                    ~(NBWV2_DESTROY_UDF_RELEASE_CONFIRMED |
                      NBWV2_DESTROY_NO_BROWSER_STARTED));
            terminal_hresult_.store(S_FALSE);
            fill_destroy_result(result);
            return S_FALSE;
        }
        const uint32_t flags = shutdown_flags_.load();
        const HRESULT terminal =
                (flags & (NBWV2_DESTROY_UDF_RELEASE_CONFIRMED |
                          NBWV2_DESTROY_NO_BROWSER_STARTED)) != 0
                ? S_OK : S_FALSE;
        terminal_hresult_.store(terminal);
        fill_destroy_result(result);
        return terminal;
    }

private:
    template <typename Action>
    HRESULT guarded_callback(const wchar_t* operation, Action&& action) noexcept {
        try {
            return action();
        } catch (const std::bad_alloc&) {
            emit_failure(E_OUTOFMEMORY, operation);
        } catch (...) {
            emit_failure(E_FAIL, operation);
        }
        return S_OK;
    }

    void thread_main() noexcept {
        HRESULT status = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
        const bool com_initialized = SUCCEEDED(status);
        if (status == RPC_E_CHANGED_MODE) {
            status = E_UNEXPECTED;
        }
        MSG bootstrap = {};
        PeekMessageW(&bootstrap, nullptr, WM_USER, WM_USER, PM_NOREMOVE);
        {
            // Publishing the native thread ID under the command-queue mutex
            // closes the pre-bootstrap lost-wakeup window: a producer either
            // leaves its command for the initial drain or observes the ID and
            // posts the dispatch message.
            std::lock_guard lock(queue_mutex_);
            thread_id_ = GetCurrentThreadId();
        }
        {
            std::lock_guard lock(start_mutex_);
            thread_start_status_ = status;
            thread_started_ = true;
        }
        start_condition_.notify_all();
        if (SUCCEEDED(status)) {
            status = create_parking_window(&parking_window_);
            if (FAILED(status)) {
                emit_failure(status, L"Create private WebView2 parking window");
            }
            if (closing_) {
                begin_shutdown_on_thread();
            } else {
                drain_commands();
                if (!closing_ && !failed_) {
                    begin_environment();
                }
            }
            MSG message = {};
            BOOL message_status = TRUE;
            while ((message_status = GetMessageW(&message, nullptr, 0, 0)) > 0) {
                if (message.message == kDispatchMessage) {
                    drain_commands();
                } else if (message.message == kShutdownMessage) {
                    begin_shutdown_on_thread();
                } else {
                    TranslateMessage(&message);
                    DispatchMessageW(&message);
                }
            }
            if (message_status == -1) {
                terminal_hresult_.store(HRESULT_FROM_WIN32(GetLastError()));
            }
        } else {
            shutdown_flags_.fetch_or(NBWV2_DESTROY_NO_BROWSER_STARTED);
        }
        cleanup_final();
        if (com_initialized) {
            CoUninitialize();
        }
        emit_closed();
        shutdown_flags_.fetch_or(NBWV2_DESTROY_CALLBACK_RETIRED);
        {
            std::lock_guard lock(queue_mutex_);
            thread_id_ = 0;
        }
    }

    HRESULT post_control(
            std::function<void()> command,
            bool reject_when_failed) {
        if (!command) {
            return E_INVALIDARG;
        }
        std::lock_guard lock(queue_mutex_);
        if ((reject_when_failed && failed_) ||
                control_commands_.size() >= kMaximumQueuedCommands) {
            return failed_ ? E_FAIL : HRESULT_FROM_WIN32(ERROR_NOT_ENOUGH_QUOTA);
        }
        if ((shutdown_flags_.load() & NBWV2_DESTROY_CALLBACK_RETIRED) != 0) {
            return RO_E_CLOSED;
        }
        control_commands_.push_back(std::move(command));
        if (thread_id_ == 0) {
            return S_OK;
        }
        if (!PostThreadMessageW(thread_id_, kDispatchMessage, 0, 0)) {
            const DWORD error = GetLastError();
            control_commands_.pop_back();
            return HRESULT_FROM_WIN32(error);
        }
        return S_OK;
    }

    HRESULT request_shutdown() noexcept {
        // A prior bounded attempt may have failed while reparenting or closing
        // the controller. Keep the exact live Host and retry the operation on
        // its COM thread instead of permanently wedging the handle.
        shutdown_failure_.store(S_OK);
        parent_release_fenced_.store(true, std::memory_order_release);
        DWORD id = 0;
        {
            std::lock_guard lock(queue_mutex_);
            closing_.store(true);
            commands_.clear();
            id = thread_id_;
        }
        if (id == 0) {
            return S_OK;
        }
        if (PostThreadMessageW(id, kShutdownMessage, 0, 0)) {
            return S_OK;
        }
        const DWORD error = GetLastError();
        if (thread_.joinable() &&
                WaitForSingleObject(thread_.native_handle(), 0) == WAIT_OBJECT_0) {
            return S_OK;
        }
        return HRESULT_FROM_WIN32(error);
    }

    void fill_destroy_result(nbwv2_destroy_result* result) const noexcept {
        if (result == nullptr) {
            return;
        }
        result->struct_size = sizeof(nbwv2_destroy_result);
        result->flags = shutdown_flags_.load();
        result->expected_browser_pid = expected_browser_pid_.load();
        result->observed_browser_pid = observed_browser_pid_.load();
        result->browser_exit_kind = browser_exit_kind_.load();
        result->terminal_hresult = terminal_hresult_.load();
        result->reserved[0] = 0;
        result->reserved[1] = 0;
    }

    HRESULT verify_environment_user_data_folder() noexcept {
        ComPtr<ICoreWebView2Environment7> environment7;
        HRESULT status = environment_.As(&environment7);
        if (FAILED(status) || !environment7) {
            return FAILED(status) ? status : E_NOINTERFACE;
        }
        LPWSTR actual = nullptr;
        status = environment7->get_UserDataFolder(&actual);
        if (FAILED(status) || actual == nullptr) {
            if (actual != nullptr) {
                CoTaskMemFree(actual);
            }
            return FAILED(status) ? status : E_POINTER;
        }
        const bool exact = user_data_ == actual;
        CoTaskMemFree(actual);
        if (!exact) {
            return HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
        }
        shutdown_flags_.fetch_or(NBWV2_DESTROY_UDF_IDENTITY_VERIFIED);
        return S_OK;
    }

    HRESULT register_browser_process_exit() noexcept {
        HRESULT status = environment_.As(&environment5_);
        if (FAILED(status) || !environment5_) {
            browser_exit_resolved_ = true;
            return FAILED(status) ? status : E_NOINTERFACE;
        }
        status = environment5_->add_BrowserProcessExited(
                Callback<ICoreWebView2BrowserProcessExitedEventHandler>(
                    [this](ICoreWebView2Environment*,
                           ICoreWebView2BrowserProcessExitedEventArgs* args) noexcept -> HRESULT {
                        return guarded_callback(
                                L"WebView2 browser-process-exited callback",
                                [&]() -> HRESULT {
                                    return browser_process_exited(args);
                                });
                    }).Get(),
                &browser_process_exit_token_);
        if (FAILED(status)) {
            environment5_.Reset();
            browser_exit_resolved_ = true;
            return status;
        }
        browser_process_exit_registered_ = true;
        return S_OK;
    }

    HRESULT browser_process_exited(
            ICoreWebView2BrowserProcessExitedEventArgs* args) noexcept {
        if (args == nullptr) {
            browser_exit_resolved_ = true;
            unregister_browser_process_exit();
            maybe_finish_shutdown();
            return S_OK;
        }
        COREWEBVIEW2_BROWSER_PROCESS_EXIT_KIND kind =
                COREWEBVIEW2_BROWSER_PROCESS_EXIT_KIND_FAILED;
        UINT32 observed_pid = 0;
        HRESULT status = args->get_BrowserProcessExitKind(&kind);
        if (SUCCEEDED(status)) {
            status = args->get_BrowserProcessId(&observed_pid);
        }
        if (FAILED(status) || observed_pid == 0) {
            browser_exit_resolved_ = true;
            unregister_browser_process_exit();
            maybe_finish_shutdown();
            return S_OK;
        }
        observed_browser_pid_.store(observed_pid);
        browser_exit_kind_.store(static_cast<uint32_t>(kind));
        shutdown_flags_.fetch_or(NBWV2_DESTROY_BROWSER_EXIT_OBSERVED);
        const UINT32 expected_pid = expected_browser_pid_.load();
        if (expected_pid != 0 && observed_pid == expected_pid) {
            shutdown_flags_.fetch_or(NBWV2_DESTROY_PID_MATCHED);
            // BrowserProcessExited is delivered for both normal shutdown and
            // browser failure only after the environment has released all
            // associated resources, including its UDF.
            if ((shutdown_flags_.load() &
                 NBWV2_DESTROY_UDF_IDENTITY_VERIFIED) != 0) {
                shutdown_flags_.fetch_or(NBWV2_DESTROY_UDF_RELEASE_CONFIRMED);
            }
        }
        browser_exit_resolved_ = true;
        unregister_browser_process_exit();
        maybe_finish_shutdown();
        return S_OK;
    }

    void unregister_browser_process_exit() noexcept {
        if (browser_process_exit_registered_ && environment5_) {
            environment5_->remove_BrowserProcessExited(
                    browser_process_exit_token_);
        }
        browser_process_exit_registered_ = false;
    }

    HRESULT attach_to_parent_if_allowed() noexcept {
        std::lock_guard lock(parent_mutex_);
        if (!controller_) {
            return E_UNEXPECTED;
        }
        const bool fenced =
                parent_release_fenced_.load(std::memory_order_acquire);
        const HWND target = fenced ? parking_window_ : parent_;
        if (target == nullptr || !IsWindow(target)) {
            return HRESULT_FROM_WIN32(ERROR_INVALID_WINDOW_HANDLE);
        }
        HRESULT status = controller_->put_ParentWindow(target);
        HWND applied = nullptr;
        if (SUCCEEDED(status)) {
            status = controller_->get_ParentWindow(&applied);
        }
        if (SUCCEEDED(status) && applied != target) {
            status = HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
        }
        if (SUCCEEDED(status) && fenced) {
            shutdown_flags_.fetch_or(NBWV2_DESTROY_PARENT_RELEASED);
        }
        return status;
    }

    HRESULT release_parent_on_thread(HWND expected_parent) noexcept {
        if (expected_parent == nullptr || expected_parent != parent_) {
            return E_INVALIDARG;
        }
        parent_release_fenced_.store(true, std::memory_order_release);
        std::lock_guard lock(parent_mutex_);
        if (!controller_) {
            shutdown_flags_.fetch_or(NBWV2_DESTROY_PARENT_RELEASED);
            return S_OK;
        }
        if (parking_window_ == nullptr || !IsWindow(parking_window_)) {
            return HRESULT_FROM_WIN32(ERROR_INVALID_WINDOW_HANDLE);
        }
        HRESULT status = controller_->put_IsVisible(FALSE);
        HWND current = nullptr;
        if (SUCCEEDED(status)) {
            status = controller_->get_ParentWindow(&current);
        }
        if (SUCCEEDED(status) && current == expected_parent) {
            status = controller_->put_ParentWindow(parking_window_);
        } else if (SUCCEEDED(status) && current != parking_window_) {
            status = HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
        }
        HWND applied = nullptr;
        if (SUCCEEDED(status)) {
            status = controller_->get_ParentWindow(&applied);
        }
        if (SUCCEEDED(status) && applied != parking_window_) {
            status = HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
        }
        if (SUCCEEDED(status)) {
            shutdown_flags_.fetch_or(NBWV2_DESTROY_PARENT_RELEASED);
        }
        return status;
    }

    void async_callback_completed() noexcept {
        if (pending_async_callbacks_ > 0) {
            --pending_async_callbacks_;
        }
        if (closing_) {
            maybe_finish_shutdown();
        }
    }

    void remove_webview_handlers() noexcept {
        if (!webview_) {
            return;
        }
        if (navigation_registered_) {
            webview_->remove_NavigationStarting(navigation_token_);
            navigation_registered_ = false;
        }
        if (navigation_completed_registered_) {
            webview_->remove_NavigationCompleted(navigation_completed_token_);
            navigation_completed_registered_ = false;
        }
        if (new_window_registered_) {
            webview_->remove_NewWindowRequested(new_window_token_);
            new_window_registered_ = false;
        }
        if (permission_registered_) {
            webview_->remove_PermissionRequested(permission_token_);
            permission_registered_ = false;
        }
        if (web_message_registered_) {
            webview_->remove_WebMessageReceived(web_message_token_);
            web_message_registered_ = false;
        }
        if (resource_registered_) {
            webview_->remove_WebResourceRequested(resource_token_);
            resource_registered_ = false;
        }
        if (process_failed_registered_) {
            webview_->remove_ProcessFailed(process_failed_token_);
            process_failed_registered_ = false;
        }
        if (download_registered_) {
            ComPtr<ICoreWebView2_4> webview4;
            if (SUCCEEDED(webview_.As(&webview4))) {
                webview4->remove_DownloadStarting(download_token_);
            }
            download_registered_ = false;
        }
        if (resource_filter_installed_) {
            webview_->RemoveWebResourceRequestedFilter(
                    L"*", COREWEBVIEW2_WEB_RESOURCE_CONTEXT_ALL);
            resource_filter_installed_ = false;
        }
    }

    void begin_shutdown_on_thread() noexcept {
        if (!closing_) {
            return;
        }
        drain_control_commands();
        if (FAILED(shutdown_failure_.load())) {
            return;
        }
        const HRESULT parent_status = release_parent_on_thread(parent_);
        if (FAILED(parent_status)) {
            if ((shutdown_flags_.load() &
                 NBWV2_DESTROY_UDF_RELEASE_CONFIRMED) != 0) {
                // BrowserProcessExited means the Runtime already released all
                // resources, including the child window and UDF. A stale COM
                // controller can no longer keep the AWT parent alive.
                shutdown_flags_.fetch_or(NBWV2_DESTROY_PARENT_RELEASED);
            } else {
                shutdown_failure_.store(parent_status);
                terminal_hresult_.store(parent_status);
                return;
            }
        }
        remove_webview_handlers();
        if (controller_) {
            if (!controller_close_attempted_) {
                HRESULT status = controller_->put_IsVisible(FALSE);
                if (SUCCEEDED(status)) {
                    status = controller_->Close();
                }
                if (FAILED(status)) {
                    if ((shutdown_flags_.load() &
                         NBWV2_DESTROY_UDF_RELEASE_CONFIRMED) == 0) {
                        shutdown_failure_.store(status);
                        terminal_hresult_.store(status);
                        return;
                    }
                    // A matching BrowserProcessExited event is stronger than
                    // a late Close HRESULT: the Runtime and UDF are already
                    // released. Drop the stale COM wrappers below.
                } else {
                    controller_close_attempted_ = true;
                    shutdown_flags_.fetch_or(NBWV2_DESTROY_CONTROLLER_CLOSED);
                }
            }
            webview_.Reset();
            controller_.Reset();
        }
        maybe_finish_shutdown();
    }

    void maybe_finish_shutdown() noexcept {
        if (!closing_ || quit_posted_ || FAILED(shutdown_failure_.load())) {
            return;
        }
        if (controller_ || pending_async_callbacks_ != 0) {
            return;
        }
        if (browser_process_exit_registered_ && !browser_exit_resolved_) {
            if (expected_browser_pid_.load() != 0) {
                return;
            }
            // An environment was created, but no controller completed and no
            // exact browser PID was captured. Waiting for an unmatchable event
            // can keep this environment alive indefinitely. Retire tracking,
            // release all native ownership, and report S_FALSE rather than
            // claiming UDF release or timing out the caller.
            browser_exit_resolved_ = true;
            unregister_browser_process_exit();
        }
        if (!environment_created_ && !environment_creation_attempted_) {
            shutdown_flags_.fetch_or(NBWV2_DESTROY_NO_BROWSER_STARTED);
        }
        quit_posted_ = true;
        PostQuitMessage(0);
    }

    void begin_environment() {
        if (closing_ || parking_window_ == nullptr || !IsWindow(parking_window_)) {
            if (closing_) {
                begin_shutdown_on_thread();
            } else {
                emit_failure(HRESULT_FROM_WIN32(ERROR_INVALID_WINDOW_HANDLE),
                             L"Validate private WebView2 parking window");
            }
            return;
        }
        auto environment_options =
                Microsoft::WRL::Make<CoreWebView2EnvironmentOptions>();
        if (!environment_options) {
            emit_failure(E_OUTOFMEMORY, L"Create exclusive WebView2 environment options");
            return;
        }
        HRESULT status = environment_options->put_TargetCompatibleBrowserVersion(
                kMinimumCompatibleRuntime);
        if (FAILED(status)) {
            emit_failure(status, L"Set minimum compatible WebView2 Runtime");
            return;
        }
        status = environment_options->put_ExclusiveUserDataFolderAccess(TRUE);
        if (FAILED(status)) {
            emit_failure(status, L"Require exclusive WebView2 user-data ownership");
            return;
        }
        environment_creation_attempted_ = true;
        ++pending_async_callbacks_;
        status = CreateCoreWebView2EnvironmentWithOptions(
                nullptr,
                user_data_.c_str(),
                environment_options.Get(),
                Callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(
                    [this](HRESULT result,
                           ICoreWebView2Environment* environment) noexcept -> HRESULT {
                        const HRESULT callback_status = guarded_callback(
                                L"WebView2 environment completion callback",
                                [&]() -> HRESULT {
                                    return environment_created(result, environment);
                        });
                        async_callback_completed();
                        return callback_status;
                    }).Get());
        if (FAILED(status)) {
            async_callback_completed();
            emit_failure(status, L"CreateCoreWebView2EnvironmentWithOptions");
        }
    }

    HRESULT environment_created(
            HRESULT result,
            ICoreWebView2Environment* environment) {
        if (FAILED(result) || environment == nullptr) {
            emit_failure(FAILED(result) ? result : E_POINTER,
                         L"CreateCoreWebView2EnvironmentWithOptions completion");
            return S_OK;
        }
        environment_ = environment;
        environment_created_ = true;
        const HRESULT identity_status =
                verify_environment_user_data_folder();
        if (FAILED(identity_status)) {
            emit_failure(identity_status,
                         L"Verify exact WebView2 user-data folder identity");
        }
        const HRESULT tracking_status = register_browser_process_exit();
        if (FAILED(tracking_status)) {
            emit_failure(tracking_status,
                         L"Register WebView2 BrowserProcessExited");
        }
        if (closing_ || failed_) {
            begin_shutdown_on_thread();
            return S_OK;
        }
        ++pending_async_callbacks_;
        const HRESULT controller_status = environment_->CreateCoreWebView2Controller(
                parking_window_,
                Callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(
                    [this](HRESULT controller_result,
                           ICoreWebView2Controller* controller) noexcept -> HRESULT {
                        const HRESULT callback_status = guarded_callback(
                                L"WebView2 controller completion callback",
                                [&]() -> HRESULT {
                                    return controller_created(
                                            controller_result, controller);
                                });
                        async_callback_completed();
                        return callback_status;
                    }).Get());
        if (FAILED(controller_status)) {
            async_callback_completed();
            emit_failure(controller_status, L"CreateCoreWebView2Controller");
        }
        return S_OK;
    }

    HRESULT controller_created(HRESULT result, ICoreWebView2Controller* controller) {
        if (FAILED(result) || controller == nullptr) {
            if (!closing_) {
                emit_failure(FAILED(result) ? result : E_POINTER,
                             L"CreateCoreWebView2Controller completion");
            }
            return S_OK;
        }
        controller_ = controller;
        HWND created_parent = nullptr;
        HRESULT status = controller_->get_ParentWindow(&created_parent);
        if (SUCCEEDED(status) && created_parent != parking_window_) {
            status = HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
        }
        if (FAILED(status)) {
            emit_failure(status, L"Verify initial WebView2 parking parent");
            return S_OK;
        }
        status = controller_->get_CoreWebView2(&webview_);
        if (FAILED(status) || !webview_) {
            emit_failure(FAILED(status) ? status : E_NOINTERFACE,
                         L"ICoreWebView2Controller::get_CoreWebView2");
            return S_OK;
        }
        UINT32 browser_pid = 0;
        status = webview_->get_BrowserProcessId(&browser_pid);
        if (FAILED(status) || browser_pid == 0) {
            emit_failure(FAILED(status) ? status : E_UNEXPECTED,
                         L"Capture WebView2 browser process ID");
            return S_OK;
        }
        expected_browser_pid_.store(browser_pid);
        if (closing_) {
            begin_shutdown_on_thread();
            return S_OK;
        }
        if (failed_) {
            std::lock_guard lock(parent_mutex_);
            parent_release_fenced_ = true;
            shutdown_flags_.fetch_or(NBWV2_DESTROY_PARENT_RELEASED);
            return S_OK;
        }
        ComPtr<ICoreWebView2Controller4> controller4;
        status = controller_.As(&controller4);
        if (FAILED(status)) {
            emit_failure(status, L"Query ICoreWebView2Controller4");
            return S_OK;
        }
        status = controller4->put_AllowExternalDrop(FALSE);
        if (FAILED(status)) {
            emit_failure(status, L"Disable WebView2 external drop");
            return S_OK;
        }
        status = configure_settings();
        if (FAILED(status)) {
            emit_failure(status, L"Configure WebView2 settings");
            return S_OK;
        }
        status = install_policy();
        if (FAILED(status)) {
            emit_failure(status, L"Install WebView2 policy");
            return S_OK;
        }
        status = controller_->put_IsVisible(FALSE);
        if (SUCCEEDED(status)) {
            status = controller_->put_Bounds(initial_bounds_);
        }
        RECT applied_bounds = {};
        if (SUCCEEDED(status)) {
            status = controller_->get_Bounds(&applied_bounds);
        }
        if (SUCCEEDED(status) && !EqualRect(&initial_bounds_, &applied_bounds)) {
            status = HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
        }
        if (SUCCEEDED(status)) {
            status = attach_to_parent_if_allowed();
        }
        HWND applied_parent = nullptr;
        if (SUCCEEDED(status)) {
            status = controller_->get_ParentWindow(&applied_parent);
        }
        const bool attached_to_awt = SUCCEEDED(status) && applied_parent == parent_;
        if (SUCCEEDED(status)) {
            status = controller_->put_IsVisible(
                    attached_to_awt && desired_visible_ ? TRUE : FALSE);
        }
        BOOL applied_visibility = FALSE;
        if (SUCCEEDED(status)) {
            status = controller_->get_IsVisible(&applied_visibility);
        }
        if (SUCCEEDED(status)
                && ((applied_visibility != FALSE) !=
                    (attached_to_awt && desired_visible_))) {
            status = HRESULT_FROM_WIN32(ERROR_INVALID_DATA);
        }
        if (FAILED(status)) {
            emit_failure(status, L"Initialize and verify WebView2 controller geometry");
            return S_OK;
        }
        const std::wstring script =
                L"if(globalThis===globalThis.top&&location.origin==='" + origin_ +
                L"'&&location.pathname==='/index.html'){Object.defineProperty(globalThis,"
                L"'__netBeansCanvasBootstrap',{value:Object.freeze({sessionNonce:'" + nonce_ +
                L"'}),configurable:false,enumerable:false,writable:false});}";
        ++pending_async_callbacks_;
        status = webview_->AddScriptToExecuteOnDocumentCreated(
                script.c_str(),
                Callback<ICoreWebView2AddScriptToExecuteOnDocumentCreatedCompletedHandler>(
                    [this](HRESULT script_result, LPCWSTR) noexcept -> HRESULT {
                        const HRESULT callback_status = guarded_callback(
                                L"WebView2 bootstrap-script completion callback",
                                [&]() -> HRESULT {
                        if (closing_ || failed_) {
                            return S_OK;
                        }
                        if (FAILED(script_result)) {
                            emit_failure(script_result,
                                         L"AddScriptToExecuteOnDocumentCreated completion");
                            return S_OK;
                        }
                        const HRESULT navigate_status = webview_->Navigate(document_url_.c_str());
                        if (FAILED(navigate_status)) {
                            emit_failure(navigate_status, L"Navigate Web Canvas document");
                        }
                        return S_OK;
                        });
                        async_callback_completed();
                        return callback_status;
                    }).Get());
        if (FAILED(status)) {
            async_callback_completed();
            emit_failure(status, L"AddScriptToExecuteOnDocumentCreated");
        }
        return S_OK;
    }

    HRESULT configure_settings() {
        ComPtr<ICoreWebView2Settings> settings;
        HRESULT status = webview_->get_Settings(&settings);
        if (FAILED(status) || !settings) {
            return FAILED(status) ? status : E_NOINTERFACE;
        }
        const std::pair<HRESULT, const wchar_t*> operations[] = {
            {settings->put_IsScriptEnabled(TRUE), L"script"},
            {settings->put_IsWebMessageEnabled(TRUE), L"web message"},
            {settings->put_AreDefaultScriptDialogsEnabled(FALSE), L"script dialog"},
            {settings->put_IsStatusBarEnabled(FALSE), L"status bar"},
            {settings->put_AreDevToolsEnabled(FALSE), L"developer tools"},
            {settings->put_AreDefaultContextMenusEnabled(FALSE), L"context menu"},
            {settings->put_AreHostObjectsAllowed(FALSE), L"host objects"},
            {settings->put_IsZoomControlEnabled(FALSE), L"zoom control"}
        };
        for (const auto& operation : operations) {
            if (FAILED(operation.first)) {
                return operation.first;
            }
        }
        ComPtr<ICoreWebView2Settings3> settings3;
        if (SUCCEEDED(settings.As(&settings3))) {
            status = settings3->put_AreBrowserAcceleratorKeysEnabled(FALSE);
            if (FAILED(status)) {
                return status;
            }
        }
        return S_OK;
    }

    HRESULT install_policy() {
        HRESULT status = webview_->AddWebResourceRequestedFilter(
                L"*", COREWEBVIEW2_WEB_RESOURCE_CONTEXT_ALL);
        if (FAILED(status)) {
            return status;
        }
        resource_filter_installed_ = true;

        status = webview_->add_NavigationStarting(
                Callback<ICoreWebView2NavigationStartingEventHandler>(
                    [this](ICoreWebView2*,
                           ICoreWebView2NavigationStartingEventArgs* args) noexcept -> HRESULT {
                        return guarded_callback(
                                L"WebView2 navigation-starting callback",
                                [&]() -> HRESULT {
                        LPWSTR uri = nullptr;
                        BOOL redirected = FALSE;
                        HRESULT read = args->get_Uri(&uri);
                        if (SUCCEEDED(read)) {
                            args->get_IsRedirected(&redirected);
                        }
                        const bool allowed = SUCCEEDED(read) && uri != nullptr &&
                                !navigation_started_ && !redirected && document_url_ == uri;
                        if (uri != nullptr) {
                            CoTaskMemFree(uri);
                        }
                        if (!allowed) {
                            args->put_Cancel(TRUE);
                            emit_failure(E_ACCESSDENIED,
                                         L"Blocked WebView2 top-level navigation");
                        } else {
                            navigation_started_ = true;
                        }
                        return S_OK;
                        });
                    }).Get(), &navigation_token_);
        if (FAILED(status)) return status;
        navigation_registered_ = true;

        status = webview_->add_NavigationCompleted(
                Callback<ICoreWebView2NavigationCompletedEventHandler>(
                    [this](ICoreWebView2*,
                           ICoreWebView2NavigationCompletedEventArgs* args) noexcept -> HRESULT {
                        return guarded_callback(
                                L"WebView2 navigation-completed callback",
                                [&]() -> HRESULT {
                        BOOL success = FALSE;
                        HRESULT read = args->get_IsSuccess(&success);
                        if (FAILED(read) || !success) {
                            COREWEBVIEW2_WEB_ERROR_STATUS error = COREWEBVIEW2_WEB_ERROR_STATUS_UNKNOWN;
                            args->get_WebErrorStatus(&error);
                            emit_failure(HRESULT_FROM_WIN32(ERROR_BAD_NET_RESP),
                                         L"WebView2 document navigation failed");
                            return S_OK;
                        }
                        emit(NBWV2_EVENT_CONTROLLER_READY, S_OK,
                             document_url_.c_str(), L"Web Canvas document loaded");
                        return S_OK;
                        });
                    }).Get(), &navigation_completed_token_);
        if (FAILED(status)) return status;
        navigation_completed_registered_ = true;

        status = webview_->add_NewWindowRequested(
                Callback<ICoreWebView2NewWindowRequestedEventHandler>(
                    [this](ICoreWebView2*,
                           ICoreWebView2NewWindowRequestedEventArgs* args) noexcept -> HRESULT {
                        return guarded_callback(
                                L"WebView2 new-window callback",
                                [&]() -> HRESULT {
                        args->put_Handled(TRUE);
                        return S_OK;
                        });
                    }).Get(), &new_window_token_);
        if (FAILED(status)) return status;
        new_window_registered_ = true;

        status = webview_->add_PermissionRequested(
                Callback<ICoreWebView2PermissionRequestedEventHandler>(
                    [this](ICoreWebView2*,
                           ICoreWebView2PermissionRequestedEventArgs* args) noexcept -> HRESULT {
                        return guarded_callback(
                                L"WebView2 permission callback",
                                [&]() -> HRESULT {
                        args->put_State(COREWEBVIEW2_PERMISSION_STATE_DENY);
                        return S_OK;
                        });
                    }).Get(), &permission_token_);
        if (FAILED(status)) return status;
        permission_registered_ = true;

        status = webview_->add_WebMessageReceived(
                Callback<ICoreWebView2WebMessageReceivedEventHandler>(
                    [this](ICoreWebView2*,
                           ICoreWebView2WebMessageReceivedEventArgs* args) noexcept -> HRESULT {
                        return guarded_callback(
                                L"WebView2 web-message callback",
                                [&]() -> HRESULT {
                        LPWSTR source = nullptr;
                        LPWSTR json = nullptr;
                        HRESULT read = args->get_Source(&source);
                        if (SUCCEEDED(read)) {
                            read = args->get_WebMessageAsJson(&json);
                        }
                        const bool exact_source = source != nullptr && document_url_ == source;
                        const size_t length = json == nullptr ? 0 :
                                wcsnlen_s(json, kMaximumJsonCharacters + 1);
                        if (FAILED(read) || !exact_source || json == nullptr ||
                                length == 0 || length > kMaximumJsonCharacters) {
                            if (source) CoTaskMemFree(source);
                            if (json) CoTaskMemFree(json);
                            emit_failure(FAILED(read) ? read : E_ACCESSDENIED,
                                         L"Reject WebView2 web message source or size");
                            return S_OK;
                        }
                        emit(NBWV2_EVENT_WEB_MESSAGE, S_OK, source, json);
                        CoTaskMemFree(source);
                        CoTaskMemFree(json);
                        return S_OK;
                        });
                    }).Get(), &web_message_token_);
        if (FAILED(status)) return status;
        web_message_registered_ = true;

        status = webview_->add_WebResourceRequested(
                Callback<ICoreWebView2WebResourceRequestedEventHandler>(
                    [this](ICoreWebView2*,
                           ICoreWebView2WebResourceRequestedEventArgs* args) noexcept -> HRESULT {
                        return guarded_callback(
                                L"WebView2 resource-request callback",
                                [&]() -> HRESULT {
                            return resource_requested(args);
                        });
                    }).Get(), &resource_token_);
        if (FAILED(status)) return status;
        resource_registered_ = true;

        status = webview_->add_ProcessFailed(
                Callback<ICoreWebView2ProcessFailedEventHandler>(
                    [this](ICoreWebView2*,
                           ICoreWebView2ProcessFailedEventArgs* args) noexcept -> HRESULT {
                        return guarded_callback(
                                L"WebView2 process-failed callback",
                                [&]() -> HRESULT {
                        COREWEBVIEW2_PROCESS_FAILED_KIND kind =
                                COREWEBVIEW2_PROCESS_FAILED_KIND_UNKNOWN_PROCESS_EXITED;
                        args->get_ProcessFailedKind(&kind);
                        failed_ = true;
                        emit(NBWV2_EVENT_PROCESS_FAILED, static_cast<int32_t>(kind),
                             document_url_.c_str(), L"WebView2 process failed");
                        return S_OK;
                        });
                    }).Get(), &process_failed_token_);
        if (FAILED(status)) return status;
        process_failed_registered_ = true;

        ComPtr<ICoreWebView2_4> webview4;
        status = webview_.As(&webview4);
        if (FAILED(status)) {
            return status;
        }
        status = webview4->add_DownloadStarting(
                Callback<ICoreWebView2DownloadStartingEventHandler>(
                    [this](ICoreWebView2*,
                           ICoreWebView2DownloadStartingEventArgs* args) noexcept -> HRESULT {
                        return guarded_callback(
                                L"WebView2 download callback",
                                [&]() -> HRESULT {
                        args->put_Cancel(TRUE);
                        args->put_Handled(TRUE);
                        return S_OK;
                        });
                    }).Get(), &download_token_);
        if (FAILED(status)) return status;
        download_registered_ = true;
        return S_OK;
    }

    HRESULT resource_requested(ICoreWebView2WebResourceRequestedEventArgs* args) {
        ComPtr<ICoreWebView2WebResourceRequest> request;
        HRESULT status = args->get_Request(&request);
        LPWSTR uri = nullptr;
        LPWSTR method = nullptr;
        if (SUCCEEDED(status) && request) {
            status = request->get_Uri(&uri);
        }
        if (SUCCEEDED(status) && request) {
            status = request->get_Method(&method);
        }
        const std::wstring requested = uri == nullptr ? L"" : uri;
        const std::wstring requested_method = method == nullptr ? L"" : method;
        if (uri) {
            CoTaskMemFree(uri);
        }
        if (method) {
            CoTaskMemFree(method);
        }
        if (FAILED(status) || requested_method != L"GET") {
            return block_resource(args, 403, L"Forbidden");
        }
        if (requested.rfind(L"data:", 0) == 0 ||
                requested.rfind(L"blob:" + origin_ + L"/", 0) == 0) {
            return S_OK;
        }
        const auto resource = resources_.find(requested);
        if (resource == resources_.end()) {
            return block_resource(args, 403, L"Forbidden");
        }
        const wchar_t* type = content_type(resource->second.relative_path);
        if (type == nullptr) {
            return block_resource(args, 415, L"Unsupported Media Type");
        }
        ComPtr<IStream> stream;
        status = stream_from_bytes(resource->second.bytes, &stream);
        if (FAILED(status)) {
            return status;
        }
        const std::wstring headers = response_headers(type);
        ComPtr<ICoreWebView2WebResourceResponse> response;
        status = environment_->CreateWebResourceResponse(
                stream.Get(), 200, L"OK", headers.c_str(), &response);
        if (FAILED(status)) {
            return status;
        }
        return args->put_Response(response.Get());
    }

    HRESULT block_resource(
            ICoreWebView2WebResourceRequestedEventArgs* args,
            int status_code,
            const wchar_t* reason) {
        static const std::vector<uint8_t> body = {'B', 'l', 'o', 'c', 'k', 'e', 'd'};
        ComPtr<IStream> stream;
        HRESULT status = stream_from_bytes(body, &stream);
        if (FAILED(status)) return status;
        ComPtr<ICoreWebView2WebResourceResponse> response;
        status = environment_->CreateWebResourceResponse(
                stream.Get(), status_code, reason,
                L"Content-Type: text/plain; charset=utf-8\r\n"
                L"X-Content-Type-Options: nosniff\r\n"
                L"Cache-Control: no-store\r\n",
                &response);
        if (FAILED(status)) return status;
        return args->put_Response(response.Get());
    }

    void drain_commands() noexcept {
        for (;;) {
            std::function<void()> command;
            {
                std::lock_guard lock(queue_mutex_);
                if (!control_commands_.empty()) {
                    command = std::move(control_commands_.front());
                    control_commands_.pop_front();
                } else if (commands_.empty() || closing_) {
                    return;
                } else {
                    command = std::move(commands_.front());
                    commands_.pop_front();
                }
            }
            try {
                command();
            } catch (...) {
                emit_failure(E_FAIL, L"Native WebView2 command");
                return;
            }
        }
    }

    void drain_control_commands() noexcept {
        for (;;) {
            std::function<void()> command;
            {
                std::lock_guard lock(queue_mutex_);
                if (control_commands_.empty()) {
                    return;
                }
                command = std::move(control_commands_.front());
                control_commands_.pop_front();
            }
            try {
                command();
            } catch (...) {
                shutdown_failure_.store(E_FAIL);
                terminal_hresult_.store(E_FAIL);
                return;
            }
        }
    }

    void emit(
            uint32_t kind,
            int32_t code,
            const wchar_t* source,
            const wchar_t* payload) noexcept {
        nbwv2_event_callback callback = callback_.load();
        if (callback != nullptr && !closed_emitted_) {
            callback(callback_context_, kind, code,
                     source == nullptr ? L"" : source,
                     payload == nullptr ? L"" : payload);
        }
    }

    void emit_failure(HRESULT status, const wchar_t* operation) noexcept {
        if (closing_ || failed_.exchange(true)) {
            return;
        }
        try {
            const std::wstring message = hresult_text(status, operation);
            emit(NBWV2_EVENT_FAILED, status, document_url_.c_str(), message.c_str());
        } catch (...) {
            emit(NBWV2_EVENT_FAILED, E_OUTOFMEMORY, document_url_.c_str(),
                 L"Native WebView2 failure diagnostic could not be allocated");
        }
    }

    void emit_closed() noexcept {
        if (closed_emitted_.exchange(true)) {
            return;
        }
        nbwv2_event_callback callback = callback_.exchange(nullptr);
        if (callback != nullptr) {
            callback(callback_context_, NBWV2_EVENT_CLOSED, S_OK, L"", L"");
        }
    }

    void cleanup_final() noexcept {
        parent_release_fenced_.store(true, std::memory_order_release);
        if (controller_) {
            release_parent_on_thread(parent_);
        } else {
            shutdown_flags_.fetch_or(NBWV2_DESTROY_PARENT_RELEASED);
        }
        remove_webview_handlers();
        if (controller_ && !controller_close_attempted_) {
            controller_close_attempted_ = true;
            HRESULT status = controller_->put_IsVisible(FALSE);
            if (SUCCEEDED(status)) {
                status = controller_->Close();
            }
            if (SUCCEEDED(status)) {
                shutdown_flags_.fetch_or(NBWV2_DESTROY_CONTROLLER_CLOSED);
            } else {
                shutdown_failure_.store(status);
                terminal_hresult_.store(status);
            }
        }
        webview_.Reset();
        controller_.Reset();
        unregister_browser_process_exit();
        environment5_.Reset();
        environment_.Reset();
        if (parking_window_ != nullptr) {
            DestroyWindow(parking_window_);
            parking_window_ = nullptr;
        }
    }

    HWND parent_;
    RECT initial_bounds_;
    std::atomic<nbwv2_event_callback> callback_;
    void* callback_context_;
    std::wstring user_data_;
    std::wstring virtual_host_;
    std::wstring nonce_;
    std::wstring origin_;
    std::wstring document_url_;
    std::unordered_map<std::wstring, ResourceSnapshot> resources_;

    std::thread thread_;
    std::mutex start_mutex_;
    std::condition_variable start_condition_;
    bool thread_started_ = false;
    HRESULT thread_start_status_ = E_UNEXPECTED;
    DWORD thread_id_ = 0;
    std::mutex queue_mutex_;
    std::deque<std::function<void()>> commands_;
    std::deque<std::function<void()>> control_commands_;
    std::atomic<bool> closing_ = false;
    std::atomic<bool> failed_ = false;
    std::atomic<bool> closed_emitted_ = false;

    HWND parking_window_ = nullptr;
    std::mutex parent_mutex_;
    std::atomic<bool> parent_release_fenced_ = false;

    ComPtr<ICoreWebView2Environment> environment_;
    ComPtr<ICoreWebView2Environment5> environment5_;
    ComPtr<ICoreWebView2Controller> controller_;
    ComPtr<ICoreWebView2> webview_;

    EventRegistrationToken navigation_token_ = {};
    EventRegistrationToken navigation_completed_token_ = {};
    EventRegistrationToken new_window_token_ = {};
    EventRegistrationToken permission_token_ = {};
    EventRegistrationToken web_message_token_ = {};
    EventRegistrationToken resource_token_ = {};
    EventRegistrationToken process_failed_token_ = {};
    EventRegistrationToken download_token_ = {};
    EventRegistrationToken browser_process_exit_token_ = {};
    bool navigation_registered_ = false;
    bool navigation_completed_registered_ = false;
    bool new_window_registered_ = false;
    bool permission_registered_ = false;
    bool web_message_registered_ = false;
    bool resource_registered_ = false;
    bool process_failed_registered_ = false;
    bool download_registered_ = false;
    bool resource_filter_installed_ = false;
    bool navigation_started_ = false;
    bool desired_visible_ = true;
    bool environment_creation_attempted_ = false;
    bool environment_created_ = false;
    bool browser_process_exit_registered_ = false;
    bool browser_exit_resolved_ = false;
    bool controller_close_attempted_ = false;
    bool quit_posted_ = false;
    size_t pending_async_callbacks_ = 0;

    std::atomic<uint32_t> shutdown_flags_ = 0;
    std::atomic<uint32_t> expected_browser_pid_ = 0;
    std::atomic<uint32_t> observed_browser_pid_ = 0;
    std::atomic<uint32_t> browser_exit_kind_ = 0;
    std::atomic<HRESULT> terminal_hresult_ = E_PENDING;
    std::atomic<HRESULT> shutdown_failure_ = S_OK;
};

Host* require_host(void* handle) {
    return static_cast<Host*>(handle);
}

}  // namespace

uint32_t __stdcall nbwv2_get_abi_version(void) noexcept {
    return NBWV2_ABI_VERSION;
}

int32_t __stdcall nbwv2_get_runtime_version(
        wchar_t* buffer,
        uint32_t buffer_characters,
        uint32_t* required_characters) noexcept {
    if (required_characters == nullptr) {
        return E_POINTER;
    }
    *required_characters = 0;
    LPWSTR version = nullptr;
    const HRESULT status = GetAvailableCoreWebView2BrowserVersionString(nullptr, &version);
    if (FAILED(status)) {
        return status;
    }
    const size_t length = wcsnlen_s(version, 256);
    if (length == 0 || length >= 256 || length + 1 > UINT32_MAX) {
        CoTaskMemFree(version);
        return E_UNEXPECTED;
    }
    *required_characters = static_cast<uint32_t>(length + 1);
    if (buffer == nullptr || buffer_characters < length + 1) {
        CoTaskMemFree(version);
        return HRESULT_FROM_WIN32(ERROR_INSUFFICIENT_BUFFER);
    }
    memcpy(buffer, version, (length + 1) * sizeof(wchar_t));
    CoTaskMemFree(version);
    return S_OK;
}

int32_t __stdcall nbwv2_create(
        const nbwv2_create_options* options,
        nbwv2_event_callback callback,
        void* callback_context,
        void** host_handle) noexcept {
    try {
    if (host_handle == nullptr) {
        return E_POINTER;
    }
    *host_handle = nullptr;
    if (options == nullptr || callback == nullptr ||
            options->struct_size != sizeof(nbwv2_create_options) ||
            options->abi_version != NBWV2_ABI_VERSION ||
            options->parent_window == nullptr || !IsWindow(options->parent_window) ||
            options->width <= 0 || options->height <= 0 ||
            options->width > 32767 || options->height > 32767 ||
            options->x > std::numeric_limits<int32_t>::max() - options->width ||
            options->y > std::numeric_limits<int32_t>::max() - options->height) {
        return E_INVALIDARG;
    }
    const std::wstring user_data = bounded(options->user_data_folder, kMaximumPathCharacters);
    const std::wstring content_root = bounded(options->content_root, kMaximumPathCharacters);
    const std::wstring host = bounded(options->virtual_host, 253);
    const std::wstring nonce = bounded(options->session_nonce, 64);
    const std::wstring manifest = bounded(options->resource_manifest, 128 * 1024);
    if (user_data.empty() || content_root.empty() || !valid_host(host) ||
            !valid_nonce(nonce) || manifest.empty()) {
        return E_INVALIDARG;
    }
    const DWORD root_attributes = GetFileAttributesW(content_root.c_str());
    if (root_attributes == INVALID_FILE_ATTRIBUTES ||
            (root_attributes & FILE_ATTRIBUTE_DIRECTORY) == 0 ||
            (root_attributes & FILE_ATTRIBUTE_REPARSE_POINT) != 0) {
        return HRESULT_FROM_WIN32(ERROR_DIRECTORY);
    }
    const std::wstring origin = L"https://" + host;
    std::unordered_map<std::wstring, ResourceSnapshot> resources;
    if (!parse_resource_manifest(
            manifest, origin, std::filesystem::path(content_root), &resources)) {
        return HRESULT_FROM_WIN32(ERROR_FILE_INVALID);
    }
    std::unique_ptr<Host> native(new (std::nothrow) Host(
            *options, callback, callback_context, user_data,
            host, nonce, std::move(resources)));
    if (!native) {
        return E_OUTOFMEMORY;
    }
    const HRESULT status = native->start();
    if (FAILED(status)) {
        nbwv2_destroy_result teardown = {};
        teardown.struct_size = sizeof(teardown);
        const HRESULT teardown_status = native->destroy(
                10'000, &teardown);
        if (teardown_status != S_OK) {
            // A failed create must not orphan native ownership. Return the
            // exact Host even though the HRESULT reports the original startup
            // failure whenever teardown either remains live (failure/timeout)
            // or retired conservatively without UDF-release proof (S_FALSE).
            // Java adopts this handle and drives the normal retryable destroy
            // path while retaining the callback and ownership evidence.
            *host_handle = native.release();
        }
        return status;
    }
    *host_handle = native.release();
    return S_OK;
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    } catch (...) {
        return E_FAIL;
    }
}

int32_t __stdcall nbwv2_post_web_message_json(
        void* host_handle,
        const wchar_t* json) noexcept {
    try {
    Host* host = require_host(host_handle);
    if (host == nullptr || json == nullptr) {
        return E_INVALIDARG;
    }
    std::wstring copy = bounded(json, kMaximumJsonCharacters);
    if (copy.empty()) {
        return E_INVALIDARG;
    }
    return host->post_message(std::move(copy));
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    } catch (...) {
        return E_FAIL;
    }
}

int32_t __stdcall nbwv2_set_bounds(
        void* host_handle,
        int32_t x,
        int32_t y,
        int32_t width,
        int32_t height) noexcept {
    try {
    Host* host = require_host(host_handle);
    if (host == nullptr || width <= 0 || height <= 0 ||
            width > 32767 || height > 32767 ||
            x > std::numeric_limits<int32_t>::max() - width ||
            y > std::numeric_limits<int32_t>::max() - height) {
        return E_INVALIDARG;
    }
    return host->set_bounds(RECT{x, y, x + width, y + height});
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    } catch (...) {
        return E_FAIL;
    }
}

int32_t __stdcall nbwv2_set_visible(
        void* host_handle, int32_t visible) noexcept {
    try {
    Host* host = require_host(host_handle);
    if (host == nullptr || (visible != 0 && visible != 1)) {
        return E_INVALIDARG;
    }
    return host->set_visible(visible != 0);
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    } catch (...) {
        return E_FAIL;
    }
}

int32_t __stdcall nbwv2_request_focus(void* host_handle) noexcept {
    try {
        Host* host = require_host(host_handle);
        return host == nullptr ? E_INVALIDARG : host->request_focus();
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    } catch (...) {
        return E_FAIL;
    }
}

int32_t __stdcall nbwv2_prepare_parent_release(
        void* host_handle,
        HWND expected_parent,
        uint32_t timeout_milliseconds) noexcept {
    try {
        Host* host = require_host(host_handle);
        if (host == nullptr) {
            return E_INVALIDARG;
        }
        return host->prepare_parent_release(
                expected_parent, timeout_milliseconds);
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    } catch (...) {
        return E_FAIL;
    }
}

int32_t __stdcall nbwv2_destroy(
        void** host_handle,
        uint32_t timeout_milliseconds,
        nbwv2_destroy_result* result) noexcept {
    if (result == nullptr) {
        return E_POINTER;
    }
    if (result->struct_size != sizeof(nbwv2_destroy_result)) {
        return E_INVALIDARG;
    }
    *result = {};
    result->struct_size = sizeof(nbwv2_destroy_result);
    if (host_handle == nullptr) {
        result->terminal_hresult = E_POINTER;
        return E_POINTER;
    }
    if (timeout_milliseconds == 0 ||
            timeout_milliseconds >
                    kMaximumSynchronousTimeoutMilliseconds) {
        result->terminal_hresult = E_INVALIDARG;
        return E_INVALIDARG;
    }
    Host* host = require_host(*host_handle);
    if (host == nullptr) {
        result->terminal_hresult = E_INVALIDARG;
        return E_INVALIDARG;
    }
    try {
        const HRESULT status = host->destroy(timeout_milliseconds, result);
        if (SUCCEEDED(status)) {
            delete host;
            *host_handle = nullptr;
        }
        return status;
    } catch (const std::bad_alloc&) {
        result->terminal_hresult = E_OUTOFMEMORY;
        return E_OUTOFMEMORY;
    } catch (...) {
        // Do not delete a host whose thread teardown could not be proven.
        // Leaking the fenced native object is safer than a use-after-free.
        result->terminal_hresult = E_FAIL;
        return E_FAIL;
    }
}
