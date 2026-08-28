#include "flutter_window.h"

#include <new>
#include <optional>

#include "canvas_drop_target.h"
#include "flutter/generated_plugin_registrant.h"

FlutterWindow::FlutterWindow(const flutter::DartProject& project)
    : project_(project) {}

FlutterWindow::~FlutterWindow() {
  // Normally WM_DESTROY already performed the full teardown. Keep this
  // idempotent fallback so the channel is still detached before the controller
  // member destroys its messenger if window creation exits unusually.
  RevokeCanvasDropTarget();
}

bool FlutterWindow::OnCreate() {
  if (!Win32Window::OnCreate()) {
    return false;
  }

  RECT frame = GetClientArea();

  // The size here must match the window dimensions to avoid unnecessary surface
  // creation / destruction in the startup path.
  flutter_controller_ = std::make_unique<flutter::FlutterViewController>(
      frame.right - frame.left, frame.bottom - frame.top, project_);
  // Ensure that basic setup of the controller was successful.
  if (!flutter_controller_->engine() || !flutter_controller_->view()) {
    return false;
  }
  RegisterPlugins(flutter_controller_->engine());
  HWND flutter_view = flutter_controller_->view()->GetNativeWindow();
  if (flutter_view == nullptr || !::IsWindow(flutter_view)) {
    flutter_controller_ = nullptr;
    return false;
  }
  SetChildContent(flutter_view);

  CanvasDropTarget* drop_target = new (std::nothrow) CanvasDropTarget(
      flutter_view, flutter_controller_->engine()->messenger());
  if (drop_target != nullptr) {
    // DnD is an optional enhancement. Retain the channel endpoint even when
    // OLE registration fails so Dart can query a definitive false result and
    // continue with the read-only Canvas capabilities.
    canvas_drop_target_ = drop_target;
    const HRESULT registration = ::RegisterDragDrop(flutter_view, drop_target);
    if (SUCCEEDED(registration)) {
      drop_target->MarkRegistered();
      canvas_drop_target_window_ = flutter_view;
    }
  }

  flutter_controller_->engine()->SetNextFrameCallback([&]() {
    this->Show();
  });

  // Flutter can complete the first frame before the "show window" callback is
  // registered. The following call ensures a frame is pending to ensure the
  // window is shown. It is a no-op if the first frame hasn't completed yet.
  flutter_controller_->ForceRedraw();

  return true;
}

void FlutterWindow::OnDestroy() {
  RevokeCanvasDropTarget();
  if (flutter_controller_) {
    flutter_controller_ = nullptr;
  }

  Win32Window::OnDestroy();
}

void FlutterWindow::RevokeCanvasDropTarget() {
  if (canvas_drop_target_ != nullptr) {
    // Detach MethodChannel callbacks while the engine/messenger is still
    // alive. This can run reentrantly from WM_DESTROY while IDropTarget::Drop
    // is inside its bounded COM message-pump wait.
    canvas_drop_target_->Shutdown();
  }
  if (canvas_drop_target_window_ != nullptr) {
    ::RevokeDragDrop(canvas_drop_target_window_);
    canvas_drop_target_window_ = nullptr;
  }
  if (canvas_drop_target_ != nullptr) {
    canvas_drop_target_->Release();
    canvas_drop_target_ = nullptr;
  }
}

LRESULT
FlutterWindow::MessageHandler(HWND hwnd, UINT const message,
                              WPARAM const wparam,
                              LPARAM const lparam) noexcept {
  // Give Flutter, including plugins, an opportunity to handle window messages.
  if (flutter_controller_) {
    std::optional<LRESULT> result =
        flutter_controller_->HandleTopLevelWindowProc(hwnd, message, wparam,
                                                      lparam);
    if (result) {
      return *result;
    }
  }

  switch (message) {
    case WM_FONTCHANGE:
      flutter_controller_->engine()->ReloadSystemFonts();
      break;
  }

  return Win32Window::MessageHandler(hwnd, message, wparam, lparam);
}
