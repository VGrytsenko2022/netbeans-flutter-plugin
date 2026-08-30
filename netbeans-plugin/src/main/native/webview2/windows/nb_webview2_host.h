#pragma once

#include <stdint.h>
#include <windows.h>

#ifdef NBWV2_HOST_EXPORTS
#define NBWV2_API extern "C" __declspec(dllexport)
#else
#define NBWV2_API extern "C" __declspec(dllimport)
#endif

#define NBWV2_ABI_VERSION 2u

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

/* Creates an asynchronous host. Completion is reported through callback. */
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
 * Synchronously drains and destroys the native host. This is idempotent only
 * for a still-live handle; callers must clear their handle after the call.
 */
NBWV2_API int32_t __stdcall nbwv2_destroy(void* host_handle) noexcept;
