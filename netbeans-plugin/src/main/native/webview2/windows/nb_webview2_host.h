#pragma once

#include <stdint.h>
#include <windows.h>

#ifdef NBWV2_HOST_EXPORTS
#define NBWV2_API extern "C" __declspec(dllexport)
#else
#define NBWV2_API extern "C" __declspec(dllimport)
#endif

#define NBWV2_ABI_VERSION 3u

enum nbwv2_event_kind : uint32_t {
    NBWV2_EVENT_CONTROLLER_READY = 1u,
    NBWV2_EVENT_WEB_MESSAGE = 2u,
    NBWV2_EVENT_DIAGNOSTIC = 3u,
    NBWV2_EVENT_FAILED = 4u,
    NBWV2_EVENT_PROCESS_FAILED = 5u,
    NBWV2_EVENT_CLOSED = 6u
};

/*
 * Payload pointers are borrowed and valid only for the duration of the
 * callback. Consumers must copy them before returning. No callback is made
 * after NBWV2_EVENT_CLOSED returns.
 */
typedef void(__stdcall* nbwv2_event_callback)(
        void* context,
        uint32_t event_kind,
        int32_t status_code,
        const wchar_t* source,
        const wchar_t* payload);

struct nbwv2_create_options {
    uint32_t struct_size;
    uint32_t abi_version;
    HWND parent_window;
    int32_t x;
    int32_t y;
    int32_t width;
    int32_t height;
    const wchar_t* user_data_folder;
    const wchar_t* content_root;
    const wchar_t* virtual_host;
    const wchar_t* session_nonce;
    /*
     * Sorted, LF-separated records: portable-path|decimal-size|lowercase-sha256.
     * The adapter verifies and snapshots every resource before returning from
     * nbwv2_create; later browser requests never reread executable files.
     */
    const wchar_t* resource_manifest;
};

enum nbwv2_destroy_flag : uint32_t {
    NBWV2_DESTROY_PARENT_RELEASED = 1u << 0,
    NBWV2_DESTROY_UDF_IDENTITY_VERIFIED = 1u << 1,
    NBWV2_DESTROY_CONTROLLER_CLOSED = 1u << 2,
    NBWV2_DESTROY_BROWSER_EXIT_OBSERVED = 1u << 3,
    NBWV2_DESTROY_PID_MATCHED = 1u << 4,
    NBWV2_DESTROY_UDF_RELEASE_CONFIRMED = 1u << 5,
    NBWV2_DESTROY_THREAD_JOINED = 1u << 6,
    NBWV2_DESTROY_CALLBACK_RETIRED = 1u << 7,
    NBWV2_DESTROY_NO_BROWSER_STARTED = 1u << 8
};

/*
 * Fixed ABI-v3 teardown evidence. Callers set struct_size before each call.
 * reserved fields must be zero and are reserved for a future ABI revision.
 */
struct nbwv2_destroy_result {
    uint32_t struct_size;
    uint32_t flags;
    uint32_t expected_browser_pid;
    uint32_t observed_browser_pid;
    uint32_t browser_exit_kind;
    int32_t terminal_hresult;
    uint32_t reserved[2];
};

static_assert(sizeof(nbwv2_destroy_result) == 32,
              "nbwv2_destroy_result must remain a 32-byte ABI structure");

NBWV2_API uint32_t __stdcall nbwv2_get_abi_version(void) noexcept;

/*
 * Returns S_OK and the installed Evergreen Runtime version. If the supplied
 * buffer is null or too small, required_characters receives the size including
 * the terminator and HRESULT_FROM_WIN32(ERROR_INSUFFICIENT_BUFFER) is returned.
 */
NBWV2_API int32_t __stdcall nbwv2_get_runtime_version(
        wchar_t* buffer,
        uint32_t buffer_characters,
        uint32_t* required_characters) noexcept;

/*
 * Creates an asynchronous host. Completion is reported through callback.
 * On a failed HRESULT, a null *host_handle proves native/browser/UDF release;
 * a non-null *host_handle transfers the exact retained Host to the caller and
 * must be retired with retryable nbwv2_destroy before any UDF deletion.
 */
NBWV2_API int32_t __stdcall nbwv2_create(
        const nbwv2_create_options* options,
        nbwv2_event_callback callback,
        void* callback_context,
        void** host_handle) noexcept;

NBWV2_API int32_t __stdcall nbwv2_post_web_message_json(
        void* host_handle,
        const wchar_t* json) noexcept;

NBWV2_API int32_t __stdcall nbwv2_set_bounds(
        void* host_handle,
        int32_t x,
        int32_t y,
        int32_t width,
        int32_t height) noexcept;

NBWV2_API int32_t __stdcall nbwv2_set_visible(
        void* host_handle,
        int32_t visible) noexcept;

NBWV2_API int32_t __stdcall nbwv2_request_focus(void* host_handle) noexcept;

/*
 * Atomically fences any future attach to expected_parent and synchronously
 * verifies that an existing controller is reparented to the host's private
 * parking window. A timeout leaves the fence in force and the handle live.
 */
NBWV2_API int32_t __stdcall nbwv2_prepare_parent_release(
        void* host_handle,
        HWND expected_parent,
        uint32_t timeout_milliseconds) noexcept;

/*
 * Retryable bounded teardown. On timeout or failure, *host_handle retains the
 * exact live handle. On S_OK or S_FALSE, the native thread is joined, callback
 * ownership is retired, the Host is deleted, and *host_handle is set to null.
 * S_OK proves a matching-PID BrowserProcessExited resource-release event or
 * that environment creation was never attempted;
 * S_FALSE proves native ownership release but not UDF release confirmation.
 */
NBWV2_API int32_t __stdcall nbwv2_destroy(
        void** host_handle,
        uint32_t timeout_milliseconds,
        nbwv2_destroy_result* result) noexcept;
