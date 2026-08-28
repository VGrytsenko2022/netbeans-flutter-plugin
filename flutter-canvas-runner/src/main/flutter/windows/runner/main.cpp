#include <flutter/dart_project.h>
#include <flutter/flutter_view_controller.h>
#include <windows.h>

#include <algorithm>
#include <charconv>
#include <cstdint>
#include <optional>
#include <string>
#include <string_view>

#include "flutter_window.h"
#include "utils.h"

namespace {

constexpr std::string_view kParentArgument = "--netbeans-parent-hwnd=0x";
constexpr std::string_view kHostProcessArgument = "--netbeans-host-pid=";
constexpr std::string_view kSurfaceEpochArgument = "--netbeans-surface-epoch=";
constexpr std::string_view kSessionNonceArgument = "--netbeans-session-nonce=";

template <typename Value>
std::optional<Value> ParseUnsignedArgument(
    const std::vector<std::string>& arguments,
    std::string_view prefix,
    int radix) {
  for (const std::string& argument : arguments) {
    if (argument.size() < prefix.size() ||
        argument.compare(0, prefix.size(), prefix) != 0) {
      continue;
    }
    const std::string_view value(argument.data() + prefix.size(),
                                 argument.size() - prefix.size());
    Value parsed = 0;
    const auto result = std::from_chars(
        value.data(), value.data() + value.size(), parsed, radix);
    if (value.empty() || result.ec != std::errc() ||
        result.ptr != value.data() + value.size() || parsed == 0) {
      return std::nullopt;
    }
    return parsed;
  }
  return std::nullopt;
}

std::optional<std::string> ParseNonce(
    const std::vector<std::string>& arguments) {
  for (const std::string& argument : arguments) {
    if (argument.size() < kSessionNonceArgument.size() ||
        argument.compare(0, kSessionNonceArgument.size(),
                         kSessionNonceArgument) != 0) {
      continue;
    }
    const std::string value = argument.substr(kSessionNonceArgument.size());
    if (value.size() != 64 || !std::all_of(
            value.begin(), value.end(), [](unsigned char character) {
              return (character >= '0' && character <= '9') ||
                     (character >= 'a' && character <= 'f');
            })) {
      return std::nullopt;
    }
    return value;
  }
  return std::nullopt;
}

struct RunnerArguments {
  HWND parent;
  DWORD host_process_id;
  std::uint64_t surface_epoch;
  std::string session_nonce;
};

std::optional<RunnerArguments> ParseRunnerArguments(
    const std::vector<std::string>& arguments) {
  const auto raw_parent = ParseUnsignedArgument<std::uintptr_t>(
      arguments, kParentArgument, 16);
  const auto raw_host_process = ParseUnsignedArgument<std::uint32_t>(
      arguments, kHostProcessArgument, 10);
  const auto surface_epoch = ParseUnsignedArgument<std::uint64_t>(
      arguments, kSurfaceEpochArgument, 10);
  const auto nonce = ParseNonce(arguments);
  if (!raw_parent || !raw_host_process || !surface_epoch || !nonce) {
    return std::nullopt;
  }

  HWND parent = reinterpret_cast<HWND>(*raw_parent);
  DWORD actual_host_process = 0;
  if (!::IsWindow(parent) ||
      ::GetWindowThreadProcessId(parent, &actual_host_process) == 0 ||
      actual_host_process != *raw_host_process) {
    return std::nullopt;
  }
  return RunnerArguments{
      parent, actual_host_process, *surface_epoch, std::move(*nonce)};
}

}  // namespace

int APIENTRY wWinMain(_In_ HINSTANCE instance, _In_opt_ HINSTANCE prev,
                      _In_ wchar_t *command_line, _In_ int show_command) {
  // Attach to console when present (e.g., 'flutter run') or create a
  // new console when running with a debugger.
  if (!::AttachConsole(ATTACH_PARENT_PROCESS) && ::IsDebuggerPresent()) {
    CreateAndAttachConsole();
  }

  // OLE drag-and-drop requires OleInitialize on this single-threaded UI
  // thread. DnD is optional, so an unavailable OLE apartment must not prevent
  // the native read-only Flutter Canvas from starting; RegisterDragDrop will
  // fail closed and the runner will omit the DnD capability.
  const HRESULT ole_result = ::OleInitialize(nullptr);
  const bool ole_initialized = SUCCEEDED(ole_result);

  flutter::DartProject project(L"data");

  std::vector<std::string> command_line_arguments =
      GetCommandLineArguments();

  const std::optional<RunnerArguments> arguments =
      ParseRunnerArguments(command_line_arguments);
  if (!arguments.has_value()) {
    if (ole_initialized) {
      ::OleUninitialize();
    }
    return EXIT_FAILURE;
  }

  FlutterWindow window(project);
  RECT parent_client{};
  if (!::GetClientRect(arguments->parent, &parent_client)) {
    if (ole_initialized) {
      ::OleUninitialize();
    }
    return EXIT_FAILURE;
  }
  const auto width = static_cast<unsigned int>(
      std::max<LONG>(1, parent_client.right - parent_client.left));
  const auto height = static_cast<unsigned int>(
      std::max<LONG>(1, parent_client.bottom - parent_client.top));
  Win32Window::Point origin(0, 0);
  Win32Window::Size size(width, height);
  if (!window.Create(
          L"NetBeans Flutter Native Canvas", origin, size,
          arguments->parent)) {
    if (ole_initialized) {
      ::OleUninitialize();
    }
    return EXIT_FAILURE;
  }
  window.SetQuitOnClose(true);

  ::MSG msg;
  while (::GetMessage(&msg, nullptr, 0, 0)) {
    ::TranslateMessage(&msg);
    ::DispatchMessage(&msg);
  }

  if (ole_initialized) {
    ::OleUninitialize();
  }
  return EXIT_SUCCESS;
}
