#include "nb_webview2_host.h"

#include <bcrypt.h>
#include <windows.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <filesystem>
#include <fstream>
#include <functional>
#include <iostream>
#include <limits>
#include <set>
#include <string>
#include <thread>
#include <vector>

namespace {

constexpr wchar_t kCanvasOrigin[] = L"https://nbfc-smoke.canvas.invalid";
constexpr wchar_t kCanvasDocument[] =
        L"https://nbfc-smoke.canvas.invalid/index.html";
constexpr wchar_t kSessionNonce[] =
        L"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
constexpr char kSessionNonceAscii[] =
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
constexpr char kProtocolSession[] = "80ef60ed-b108-4674-99a6-c1f3102f01ab";

struct SmokeState {
    std::atomic<bool> document_ready = false;
    std::atomic<bool> bridge_ready = false;
    std::atomic<bool> protocol_round_trip = false;
    std::atomic<bool> failed = false;
    std::atomic<bool> closed = false;
};

bool exact_bridge_ready(const wchar_t* source, const wchar_t* payload);
bool valid_runner_hello(const wchar_t* source, const wchar_t* payload);

void __stdcall accept_event(
        void* context,
        uint32_t kind,
        int32_t code,
        const wchar_t* source,
        const wchar_t* payload) {
    auto* state = static_cast<SmokeState*>(context);
    std::wcout << L"event=" << kind << L" code=" << code
               << L" source=" << (source == nullptr ? L"" : source)
               << L" payload=" << (payload == nullptr ? L"" : payload)
               << std::endl;
    if (kind == NBWV2_EVENT_CONTROLLER_READY) {
        state->document_ready = source != nullptr
                && std::wstring(source) == kCanvasDocument;
    } else if (kind == NBWV2_EVENT_WEB_MESSAGE && payload != nullptr) {
        if (exact_bridge_ready(source, payload)) {
            state->bridge_ready = true;
        } else if (valid_runner_hello(source, payload)) {
            state->protocol_round_trip = true;
        }
    } else if (kind == NBWV2_EVENT_FAILED ||
               kind == NBWV2_EVENT_PROCESS_FAILED) {
        state->failed = true;
    } else if (kind == NBWV2_EVENT_CLOSED) {
        state->closed = true;
    }
}

LRESULT CALLBACK window_proc(HWND window, UINT message, WPARAM wparam, LPARAM lparam) {
    if (message == WM_CLOSE) {
        DestroyWindow(window);
        return 0;
    }
    return DefWindowProcW(window, message, wparam, lparam);
}

std::wstring sha256_file(const std::filesystem::path& file) {
    BCRYPT_ALG_HANDLE algorithm = nullptr;
    BCRYPT_HASH_HANDLE hash = nullptr;
    DWORD object_bytes = 0;
    DWORD hash_bytes = 0;
    DWORD returned = 0;
    NTSTATUS status = BCryptOpenAlgorithmProvider(
            &algorithm, BCRYPT_SHA256_ALGORITHM, nullptr, 0);
    if (status < 0) return {};
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
    std::vector<uint8_t> object(object_bytes);
    std::vector<uint8_t> digest(hash_bytes);
    if (status >= 0 && object_bytes > 0 && hash_bytes == 32) {
        status = BCryptCreateHash(
                algorithm, &hash, object.data(), object_bytes, nullptr, 0, 0);
    } else if (status >= 0) {
        status = static_cast<NTSTATUS>(0xC000000DL);
    }
    std::ifstream input(file, std::ios::binary);
    std::vector<uint8_t> buffer(1024 * 1024);
    while (status >= 0 && input) {
        input.read(reinterpret_cast<char*>(buffer.data()),
                   static_cast<std::streamsize>(buffer.size()));
        const std::streamsize count = input.gcount();
        if (count > 0) {
            status = BCryptHashData(
                    hash, buffer.data(), static_cast<ULONG>(count), 0);
        }
    }
    if (!input.eof()) {
        status = static_cast<NTSTATUS>(0xC000000DL);
    }
    if (status >= 0) {
        status = BCryptFinishHash(hash, digest.data(), hash_bytes, 0);
    }
    if (hash != nullptr) BCryptDestroyHash(hash);
    BCryptCloseAlgorithmProvider(algorithm, 0);
    if (status < 0) return {};
    constexpr wchar_t digits[] = L"0123456789abcdef";
    std::wstring result;
    result.reserve(64);
    for (uint8_t value : digest) {
        result.push_back(digits[value >> 4]);
        result.push_back(digits[value & 0x0F]);
    }
    return result;
}

std::wstring resource_manifest(const std::filesystem::path& root) {
    std::set<std::wstring> paths;
    for (const auto& entry : std::filesystem::recursive_directory_iterator(root)) {
        if (!entry.is_regular_file()) {
            continue;
        }
        std::wstring relative = std::filesystem::relative(entry.path(), root).generic_wstring();
        if (relative != L".last_build_id") {
            paths.insert(std::move(relative));
        }
    }
    std::wstring serialized;
    for (const auto& path : paths) {
        const std::filesystem::path file = root / std::filesystem::path(path);
        const std::wstring digest = sha256_file(file);
        if (digest.size() != 64) {
            return {};
        }
        if (!serialized.empty()) {
            serialized.push_back(L'\n');
        }
        serialized.append(path);
        serialized.push_back(L'|');
        serialized.append(std::to_wstring(std::filesystem::file_size(file)));
        serialized.push_back(L'|');
        serialized.append(digest);
    }
    return serialized;
}

std::vector<uint8_t> sha256_bytes(const std::vector<uint8_t>& bytes) {
    BCRYPT_ALG_HANDLE algorithm = nullptr;
    BCRYPT_HASH_HANDLE hash = nullptr;
    DWORD object_bytes = 0;
    DWORD hash_bytes = 0;
    DWORD returned = 0;
    NTSTATUS status = BCryptOpenAlgorithmProvider(
            &algorithm, BCRYPT_SHA256_ALGORITHM, nullptr, 0);
    if (status >= 0) {
        status = BCryptGetProperty(
                algorithm, BCRYPT_OBJECT_LENGTH,
                reinterpret_cast<PUCHAR>(&object_bytes), sizeof(object_bytes),
                &returned, 0);
    }
    if (status >= 0) {
        status = BCryptGetProperty(
                algorithm, BCRYPT_HASH_LENGTH,
                reinterpret_cast<PUCHAR>(&hash_bytes), sizeof(hash_bytes),
                &returned, 0);
    }
    std::vector<uint8_t> object(object_bytes);
    std::vector<uint8_t> digest(hash_bytes);
    if (status >= 0 && object_bytes > 0 && hash_bytes == 32) {
        status = BCryptCreateHash(
                algorithm, &hash, object.data(), object_bytes, nullptr, 0, 0);
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
    if (hash != nullptr) BCryptDestroyHash(hash);
    if (algorithm != nullptr) BCryptCloseAlgorithmProvider(algorithm, 0);
    return status >= 0 ? digest : std::vector<uint8_t>{};
}

std::string base64_encode(const std::vector<uint8_t>& bytes) {
    constexpr char alphabet[] =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string result;
    result.reserve(((bytes.size() + 2) / 3) * 4);
    for (size_t index = 0; index < bytes.size(); index += 3) {
        const uint32_t first = bytes[index];
        const uint32_t second = index + 1 < bytes.size() ? bytes[index + 1] : 0;
        const uint32_t third = index + 2 < bytes.size() ? bytes[index + 2] : 0;
        const uint32_t block = (first << 16) | (second << 8) | third;
        result.push_back(alphabet[(block >> 18) & 0x3f]);
        result.push_back(alphabet[(block >> 12) & 0x3f]);
        result.push_back(index + 1 < bytes.size()
                ? alphabet[(block >> 6) & 0x3f] : '=');
        result.push_back(index + 2 < bytes.size()
                ? alphabet[block & 0x3f] : '=');
    }
    return result;
}

int base64_value(wchar_t character) {
    if (character >= L'A' && character <= L'Z') return character - L'A';
    if (character >= L'a' && character <= L'z') return character - L'a' + 26;
    if (character >= L'0' && character <= L'9') return character - L'0' + 52;
    if (character == L'+') return 62;
    if (character == L'/') return 63;
    return -1;
}

std::vector<uint8_t> base64_decode(const std::wstring& encoded) {
    if (encoded.empty() || encoded.size() % 4 != 0) return {};
    std::vector<uint8_t> result;
    result.reserve((encoded.size() / 4) * 3);
    for (size_t index = 0; index < encoded.size(); index += 4) {
        const int first = base64_value(encoded[index]);
        const int second = base64_value(encoded[index + 1]);
        const bool third_padding = encoded[index + 2] == L'=';
        const bool fourth_padding = encoded[index + 3] == L'=';
        const int third = third_padding ? 0 : base64_value(encoded[index + 2]);
        const int fourth = fourth_padding ? 0 : base64_value(encoded[index + 3]);
        if (first < 0 || second < 0 || third < 0 || fourth < 0
                || (third_padding && !fourth_padding)
                || ((third_padding || fourth_padding) && index + 4 != encoded.size())) {
            return {};
        }
        const uint32_t block = (static_cast<uint32_t>(first) << 18)
                | (static_cast<uint32_t>(second) << 12)
                | (static_cast<uint32_t>(third) << 6)
                | static_cast<uint32_t>(fourth);
        result.push_back(static_cast<uint8_t>((block >> 16) & 0xff));
        if (!third_padding) result.push_back(static_cast<uint8_t>((block >> 8) & 0xff));
        if (!fourth_padding) result.push_back(static_cast<uint8_t>(block & 0xff));
    }
    const std::string canonical = base64_encode(result);
    if (canonical.size() != encoded.size()) return {};
    for (size_t index = 0; index < canonical.size(); index++) {
        if (encoded[index] != static_cast<wchar_t>(canonical[index])) return {};
    }
    return result;
}

bool exact_bridge_ready(const wchar_t* source, const wchar_t* payload) {
    const std::wstring expected =
            L"{\"format\":\"netbeans-flutter-canvas-web-bridge\",\"version\":1,"
            L"\"sessionNonce\":\"" + std::wstring(kSessionNonce) +
            L"\",\"direction\":\"runner-to-host\",\"kind\":\"ready\","
            L"\"sequence\":0}";
    return source != nullptr && payload != nullptr
            && std::wstring(source) == kCanvasDocument
            && std::wstring(payload) == expected;
}

bool valid_runner_hello(const wchar_t* source, const wchar_t* payload) {
    if (source == nullptr || payload == nullptr
            || std::wstring(source) != kCanvasDocument) {
        return false;
    }
    const std::wstring prefix =
            L"{\"format\":\"netbeans-flutter-canvas-web-bridge\",\"version\":1,"
            L"\"sessionNonce\":\"" + std::wstring(kSessionNonce) +
            L"\",\"direction\":\"runner-to-host\",\"kind\":\"chunk\","
            L"\"sequence\":1,\"chunk\":\"";
    const std::wstring message(payload);
    if (message.size() <= prefix.size() + 2
            || message.compare(0, prefix.size(), prefix) != 0
            || message.compare(message.size() - 2, 2, L"\"}") != 0) {
        return false;
    }
    const std::vector<uint8_t> frame = base64_decode(message.substr(
            prefix.size(), message.size() - prefix.size() - 2));
    if (frame.size() <= 44 || frame[0] != 'N' || frame[1] != 'B'
            || frame[2] != 'F' || frame[3] != 'C' || frame[4] != 1
            || frame[5] != 1 || frame[6] != 0 || frame[7] != 0) {
        return false;
    }
    const uint32_t payload_length = (static_cast<uint32_t>(frame[8]) << 24)
            | (static_cast<uint32_t>(frame[9]) << 16)
            | (static_cast<uint32_t>(frame[10]) << 8)
            | static_cast<uint32_t>(frame[11]);
    if (payload_length != frame.size() - 44) return false;
    const std::vector<uint8_t> body(frame.begin() + 44, frame.end());
    const std::vector<uint8_t> digest = sha256_bytes(body);
    if (digest.size() != 32
            || !std::equal(digest.begin(), digest.end(), frame.begin() + 12)) {
        return false;
    }
    const std::string json(body.begin(), body.end());
    return json.find("\"format\":\"netbeans-flutter-canvas-wire\"")
                    != std::string::npos
            && json.find("\"protocolVersion\":1") != std::string::npos
            && json.find("\"sessionId\":\"" + std::string(kProtocolSession) + "\"")
                    != std::string::npos
            && json.find("\"sequence\":0") != std::string::npos
            && json.find("\"type\":\"runner.hello\"") != std::string::npos
            && json.find("\"replyTo\":0") != std::string::npos;
}

std::wstring host_hello_envelope() {
    const std::string hello =
            "{\"format\":\"netbeans-flutter-canvas-wire\",\"protocolVersion\":1,"
            "\"sessionId\":\"" + std::string(kProtocolSession) +
            "\",\"sequence\":0,\"type\":\"host.hello\",\"body\":{"
            "\"hostVersion\":\"native-smoke\","
            "\"requestedCapabilities\":[\"readOnly.render\"],"
            "\"offeredLimits\":{\"maxControlMessageBytes\":262144,"
            "\"maxModelBytes\":16777216,\"maxCatalogBytes\":4194304,"
            "\"maxLayoutBytes\":8388608,\"maxEncodedImageBytes\":16777216,"
            "\"maxPhysicalDimension\":4096,\"maxPhysicalPixels\":8388608}}}";
    const std::vector<uint8_t> body(hello.begin(), hello.end());
    const std::vector<uint8_t> digest = sha256_bytes(body);
    if (digest.size() != 32) return {};
    std::vector<uint8_t> frame(44 + body.size());
    frame[0] = 'N'; frame[1] = 'B'; frame[2] = 'F'; frame[3] = 'C';
    frame[4] = 1;
    frame[5] = 1;
    const uint32_t length = static_cast<uint32_t>(body.size());
    frame[8] = static_cast<uint8_t>(length >> 24);
    frame[9] = static_cast<uint8_t>(length >> 16);
    frame[10] = static_cast<uint8_t>(length >> 8);
    frame[11] = static_cast<uint8_t>(length);
    std::copy(digest.begin(), digest.end(), frame.begin() + 12);
    std::copy(body.begin(), body.end(), frame.begin() + 44);
    const std::string encoded = base64_encode(frame);
    const std::string envelope =
            "{\"format\":\"netbeans-flutter-canvas-web-bridge\",\"version\":1,"
            "\"sessionNonce\":\"" + std::string(kSessionNonceAscii) +
            "\",\"direction\":\"host-to-runner\",\"kind\":\"chunk\","
            "\"sequence\":1,\"chunk\":\"" + encoded + "\"}";
    return std::wstring(envelope.begin(), envelope.end());
}

bool pump_until(
        const std::function<bool()>& complete,
        std::chrono::steady_clock::duration timeout) {
    const auto deadline = std::chrono::steady_clock::now() + timeout;
    while (!complete() && std::chrono::steady_clock::now() < deadline) {
        MSG message = {};
        while (PeekMessageW(&message, nullptr, 0, 0, PM_REMOVE)) {
            TranslateMessage(&message);
            DispatchMessageW(&message);
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    return complete();
}

}  // namespace

int wmain(int argc, wchar_t** argv) {
    if (argc != 3) {
        std::wcerr << L"Usage: nb_flutter_webview2_smoke <build-web-root> <user-data-root>"
                   << std::endl;
        return 64;
    }
    const std::filesystem::path content =
            std::filesystem::absolute(argv[1]).lexically_normal();
    const std::filesystem::path user_data =
            std::filesystem::absolute(argv[2]).lexically_normal();
    std::filesystem::create_directories(user_data);

    uint32_t required = 0;
    HRESULT status = nbwv2_get_runtime_version(nullptr, 0, &required);
    if (status != HRESULT_FROM_WIN32(ERROR_INSUFFICIENT_BUFFER) || required < 2) {
        std::wcerr << L"Runtime probe failed: 0x" << std::hex << status << std::endl;
        return 2;
    }
    std::wstring version(required, L'\0');
    status = nbwv2_get_runtime_version(version.data(), required, &required);
    if (FAILED(status)) {
        std::wcerr << L"Runtime version read failed: 0x" << std::hex << status << std::endl;
        return 3;
    }
    version.resize(wcslen(version.c_str()));
    std::wcout << L"runtime=" << version << std::endl;

    WNDCLASSW window_class = {};
    window_class.lpfnWndProc = window_proc;
    window_class.hInstance = GetModuleHandleW(nullptr);
    window_class.lpszClassName = L"NetBeansFlutterWebView2Smoke";
    if (!RegisterClassW(&window_class) && GetLastError() != ERROR_CLASS_ALREADY_EXISTS) {
        return 4;
    }
    HWND window = CreateWindowExW(
            0, window_class.lpszClassName, L"WebView2 smoke",
            WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, 800, 600,
            nullptr, nullptr, window_class.hInstance, nullptr);
    if (window == nullptr) {
        return 5;
    }
    ShowWindow(window, SW_SHOWNA);

    const std::wstring resources = resource_manifest(content);
    if (resources.empty()) {
        std::wcerr << L"Could not build the verified resource manifest." << std::endl;
        DestroyWindow(window);
        return 6;
    }
    SmokeState state;
    nbwv2_create_options options = {};
    options.struct_size = sizeof(options);
    options.abi_version = NBWV2_ABI_VERSION;
    options.parent_window = window;
    options.x = 0;
    options.y = 0;
    options.width = 780;
    options.height = 540;
    options.user_data_folder = user_data.c_str();
    options.content_root = content.c_str();
    options.virtual_host = L"nbfc-smoke.canvas.invalid";
    options.session_nonce = kSessionNonce;
    options.resource_manifest = resources.c_str();

    void* handle = reinterpret_cast<void*>(static_cast<uintptr_t>(1));
    nbwv2_create_options invalid_options = options;
    invalid_options.width = 32768;
    status = nbwv2_create(&invalid_options, accept_event, &state, &handle);
    if (status != E_INVALIDARG || handle != nullptr) {
        std::wcerr << L"Oversized create bounds were accepted." << std::endl;
        DestroyWindow(window);
        return 7;
    }
    handle = reinterpret_cast<void*>(static_cast<uintptr_t>(1));
    invalid_options = options;
    invalid_options.x = std::numeric_limits<int32_t>::max();
    status = nbwv2_create(&invalid_options, accept_event, &state, &handle);
    if (status != E_INVALIDARG || handle != nullptr) {
        std::wcerr << L"Overflowing create bounds were accepted." << std::endl;
        DestroyWindow(window);
        return 7;
    }
    status = nbwv2_create(&options, accept_event, &state, &handle);
    if (FAILED(status) || handle == nullptr) {
        std::wcerr << L"Host create failed: 0x" << std::hex << status << std::endl;
        DestroyWindow(window);
        return 7;
    }
    const HRESULT hide_before_ready = nbwv2_set_visible(handle, 0);
    const bool ready = pump_until(
            [&state] { return state.failed ||
                    (state.document_ready && state.bridge_ready); },
            std::chrono::seconds(45));
    const std::wstring hello = host_hello_envelope();
    const HRESULT round_trip_post = ready && !hello.empty()
            ? nbwv2_post_web_message_json(handle, hello.c_str()) : E_UNEXPECTED;
    const bool round_trip = SUCCEEDED(round_trip_post) && pump_until(
            [&state] { return state.failed || state.protocol_round_trip; },
            std::chrono::seconds(15));
    const HRESULT resize = nbwv2_set_bounds(handle, 5, 7, 760, 510);
    const HRESULT show = nbwv2_set_visible(handle, 1);
    const HRESULT focus = nbwv2_request_focus(handle);
    pump_until([] { return false; }, std::chrono::milliseconds(250));
    const HRESULT parent_release = nbwv2_prepare_parent_release(
            handle, window, 5'000);
    const bool parent_was_destroyed = SUCCEEDED(parent_release);
    if (parent_was_destroyed) {
        DestroyWindow(window);
        window = nullptr;
    }
    std::atomic<bool> destroy_complete = false;
    HRESULT destroyed = E_PENDING;
    HRESULT first_destroy = E_PENDING;
    bool invalid_destroy_retained_exact_handle = false;
    bool timeout_retained_exact_handle = false;
    nbwv2_destroy_result destroy_result = {};
    destroy_result.struct_size = sizeof(destroy_result);
    std::thread destroyer([&] {
        void* const exact_handle = handle;
        nbwv2_destroy_result invalid_result = {};
        invalid_result.struct_size = sizeof(invalid_result);
        const HRESULT invalid_destroy = nbwv2_destroy(
                &handle, 0, &invalid_result);
        invalid_destroy_retained_exact_handle =
                invalid_destroy == E_INVALIDARG &&
                handle == exact_handle &&
                invalid_result.struct_size == sizeof(invalid_result) &&
                invalid_result.terminal_hresult == E_INVALIDARG &&
                invalid_result.reserved[0] == 0 &&
                invalid_result.reserved[1] == 0;
        nbwv2_destroy_result timeout_result = {};
        timeout_result.struct_size = sizeof(timeout_result);
        first_destroy = nbwv2_destroy(&handle, 1, &timeout_result);
        if (first_destroy == HRESULT_FROM_WIN32(ERROR_TIMEOUT)) {
            timeout_retained_exact_handle = handle == exact_handle &&
                    timeout_result.struct_size == sizeof(timeout_result) &&
                    timeout_result.terminal_hresult ==
                            HRESULT_FROM_WIN32(ERROR_TIMEOUT) &&
                    timeout_result.reserved[0] == 0 &&
                    timeout_result.reserved[1] == 0;
            destroy_result = {};
            destroy_result.struct_size = sizeof(destroy_result);
            destroyed = nbwv2_destroy(&handle, 30'000, &destroy_result);
        } else {
            destroyed = first_destroy;
            destroy_result = timeout_result;
        }
        destroy_complete = true;
    });
    const bool destroy_returned = pump_until(
            [&] { return destroy_complete.load(); },
            std::chrono::seconds(35));
    if (!destroy_returned) {
        std::wcerr << L"Native destroy exceeded its hard smoke deadline."
                   << std::endl;
        TerminateProcess(GetCurrentProcess(), 9);
        return 9;
    }
    destroyer.join();
    const bool closed = state.closed.load();
    if (window != nullptr) {
        DestroyWindow(window);
        window = nullptr;
    }
    constexpr uint32_t required_destroy_flags =
            NBWV2_DESTROY_PARENT_RELEASED |
            NBWV2_DESTROY_UDF_IDENTITY_VERIFIED |
            NBWV2_DESTROY_CONTROLLER_CLOSED |
            NBWV2_DESTROY_BROWSER_EXIT_OBSERVED |
            NBWV2_DESTROY_PID_MATCHED |
            NBWV2_DESTROY_UDF_RELEASE_CONFIRMED |
            NBWV2_DESTROY_THREAD_JOINED |
            NBWV2_DESTROY_CALLBACK_RETIRED;
    const bool exact_destroy_evidence =
            destroyed == S_OK && handle == nullptr &&
            destroy_result.struct_size == sizeof(destroy_result) &&
            (destroy_result.flags & required_destroy_flags) ==
                    required_destroy_flags &&
            (destroy_result.flags & NBWV2_DESTROY_NO_BROWSER_STARTED) == 0 &&
            destroy_result.expected_browser_pid != 0 &&
            destroy_result.observed_browser_pid ==
                    destroy_result.expected_browser_pid &&
            // COREWEBVIEW2_BROWSER_PROCESS_EXIT_KIND_NORMAL is zero.
            destroy_result.browser_exit_kind == 0 &&
            destroy_result.terminal_hresult == S_OK &&
            destroy_result.reserved[0] == 0 &&
            destroy_result.reserved[1] == 0;
    const bool retry_contract =
            (first_destroy == HRESULT_FROM_WIN32(ERROR_TIMEOUT) &&
             timeout_retained_exact_handle) || first_destroy == S_OK;
    if (!ready || !round_trip || state.failed || !state.document_ready
            || !state.bridge_ready || !state.protocol_round_trip ||
            FAILED(round_trip_post) ||
            FAILED(hide_before_ready) || FAILED(resize) || FAILED(show) ||
            FAILED(focus) || FAILED(parent_release) ||
            !parent_was_destroyed || !invalid_destroy_retained_exact_handle ||
            !retry_contract ||
            !exact_destroy_evidence || !closed) {
        std::wcerr << L"destroy=0x" << std::hex << destroyed
                   << L" first-destroy=0x" << first_destroy
                   << L" parent-release=0x" << parent_release
                   << L" flags=0x" << destroy_result.flags
                   << L" expected-pid=" << std::dec
                   << destroy_result.expected_browser_pid
                   << L" observed-pid=" << destroy_result.observed_browser_pid
                   << L" exit-kind=" << destroy_result.browser_exit_kind
                   << L" terminal=0x" << std::hex
                   << destroy_result.terminal_hresult << std::endl;
        std::wcerr << L"Physical WebView2 smoke failed." << std::endl;
        return 8;
    }

    HWND early_window = CreateWindowExW(
            0, window_class.lpszClassName, L"WebView2 early-close smoke",
            WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, 320, 240,
            nullptr, nullptr, window_class.hInstance, nullptr);
    if (early_window == nullptr) {
        std::wcerr << L"Could not create early-close parent window."
                   << std::endl;
        return 10;
    }
    const std::filesystem::path early_user_data =
            user_data / L"close-during-start";
    std::filesystem::create_directories(early_user_data);
    SmokeState early_state;
    nbwv2_create_options early_options = options;
    early_options.parent_window = early_window;
    early_options.width = 300;
    early_options.height = 200;
    const std::wstring early_user_data_text = early_user_data.wstring();
    early_options.user_data_folder = early_user_data_text.c_str();
    void* early_handle = nullptr;
    const HRESULT early_create = nbwv2_create(
            &early_options, accept_event, &early_state, &early_handle);
    const HRESULT early_parent_release = SUCCEEDED(early_create) &&
            early_handle != nullptr
            ? nbwv2_prepare_parent_release(
                    early_handle, early_window, 5'000)
            : E_UNEXPECTED;
    if (SUCCEEDED(early_parent_release)) {
        DestroyWindow(early_window);
        early_window = nullptr;
    }
    std::atomic<bool> early_destroy_complete = false;
    HRESULT early_destroy = E_PENDING;
    nbwv2_destroy_result early_result = {};
    early_result.struct_size = sizeof(early_result);
    std::thread early_destroyer([&] {
        if (early_handle != nullptr) {
            early_destroy = nbwv2_destroy(
                    &early_handle, 30'000, &early_result);
        }
        early_destroy_complete = true;
    });
    const bool early_destroy_returned = pump_until(
            [&] { return early_destroy_complete.load(); },
            std::chrono::seconds(35));
    if (!early_destroy_returned) {
        std::wcerr << L"Early-close destroy exceeded its hard deadline."
                   << std::endl;
        TerminateProcess(GetCurrentProcess(), 11);
        return 11;
    }
    early_destroyer.join();
    if (early_window != nullptr) {
        DestroyWindow(early_window);
        early_window = nullptr;
    }
    constexpr uint32_t early_ownership_flags =
            NBWV2_DESTROY_PARENT_RELEASED |
            NBWV2_DESTROY_THREAD_JOINED |
            NBWV2_DESTROY_CALLBACK_RETIRED;
    const bool early_terminal_evidence =
            (early_destroy == S_OK &&
             (early_result.flags &
              (NBWV2_DESTROY_UDF_RELEASE_CONFIRMED |
               NBWV2_DESTROY_NO_BROWSER_STARTED)) != 0) ||
            (early_destroy == S_FALSE &&
             (early_result.flags &
              (NBWV2_DESTROY_UDF_RELEASE_CONFIRMED |
               NBWV2_DESTROY_NO_BROWSER_STARTED)) == 0);
    const bool early_close_contract =
            early_create == S_OK && early_parent_release == S_OK &&
            early_handle == nullptr && early_state.closed &&
            !early_state.failed &&
            (early_result.flags & early_ownership_flags) ==
                    early_ownership_flags &&
            early_result.terminal_hresult == early_destroy &&
            early_result.reserved[0] == 0 &&
            early_result.reserved[1] == 0 && early_terminal_evidence;
    if (!early_close_contract) {
        std::wcerr << L"early-create=0x" << std::hex << early_create
                   << L" early-parent-release=0x" << early_parent_release
                   << L" early-destroy=0x" << early_destroy
                   << L" early-flags=0x" << early_result.flags
                   << L" early-terminal=0x"
                   << early_result.terminal_hresult << std::endl;
        std::wcerr << L"Close-during-start smoke failed." << std::endl;
        return 12;
    }
    std::wcout << L"physical-smoke=PASS" << std::endl;
    return 0;
}
