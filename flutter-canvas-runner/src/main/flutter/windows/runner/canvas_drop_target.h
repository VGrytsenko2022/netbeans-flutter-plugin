#ifndef RUNNER_CANVAS_DROP_TARGET_H_
#define RUNNER_CANVAS_DROP_TARGET_H_

#include <flutter/encodable_value.h>
#include <flutter/method_channel.h>
#include <oleidl.h>
#include <windows.h>

#include <atomic>
#include <cstdint>
#include <memory>
#include <optional>
#include <string>

// Native OLE drop target for the exact FlutterView child window. It accepts
// only the private bounded NetBeans Flutter Designer palette token and forwards
// a normalized semantic event to Flutter; it never receives project data.
class CanvasDropTarget final : public IDropTarget {
 public:
  CanvasDropTarget(HWND flutter_view,
                   flutter::BinaryMessenger* messenger);

  // Records that this target is owned by OLE for the exact FlutterView HWND.
  // Until this is called the availability MethodChannel deliberately reports
  // false, so a failed RegisterDragDrop cannot enable the wire capability.
  void MarkRegistered();

  // Detaches every messenger callback while the Flutter engine is still
  // alive. Safe to call reentrantly from WM_DESTROY during Drop's nested wait.
  void Shutdown();

  // IUnknown.
  HRESULT STDMETHODCALLTYPE QueryInterface(REFIID interface_id,
                                           void** object) override;
  ULONG STDMETHODCALLTYPE AddRef() override;
  ULONG STDMETHODCALLTYPE Release() override;

  // IDropTarget.
  HRESULT STDMETHODCALLTYPE DragEnter(IDataObject* data_object,
                                      DWORD key_state,
                                      POINTL point,
                                      DWORD* effect) override;
  HRESULT STDMETHODCALLTYPE DragOver(DWORD key_state,
                                     POINTL point,
                                     DWORD* effect) override;
  HRESULT STDMETHODCALLTYPE DragLeave() override;
  HRESULT STDMETHODCALLTYPE Drop(IDataObject* data_object,
                                 DWORD key_state,
                                 POINTL point,
                                 DWORD* effect) override;

 private:
  struct HoverCallbackBridge;

  ~CanvasDropTarget();

  void BeginDrag(std::string token, POINTL point);
  void UpdateHoverPoint(POINTL point);
  void InvalidateHoverApproval();
  void ScheduleHoverProbe();
  void CompleteHoverProbe(std::int64_t generation,
                          std::int64_t probe_id,
                          bool approved);
  void FinishDrag(bool send_leave);
  void SendHoverLeave(std::int64_t generation);
  bool IsLatestHoverApproved(POINTL point) const;
  bool AdvanceProbeId();
  void DetachHoverCallbackBridge();

  std::atomic<ULONG> reference_count_{1};
  HWND flutter_view_ = nullptr;
  flutter::MethodChannel<flutter::EncodableValue> method_channel_;
  std::shared_ptr<HoverCallbackBridge> hover_callback_bridge_;
  HANDLE shutdown_event_ = nullptr;
  bool registered_ = false;
  bool shutdown_ = false;
  bool channel_handler_installed_ = false;
  bool drag_active_ = false;
  bool drop_wait_in_progress_ = false;
  bool drop_wait_cancelled_ = false;
  bool hover_point_valid_ = false;
  bool hover_probe_in_flight_ = false;
  std::string palette_token_;
  std::int64_t generation_counter_ = 0;
  std::int64_t active_generation_ = 0;
  std::int64_t probe_id_counter_ = 0;
  std::int64_t desired_probe_id_ = -1;
  std::int64_t last_sent_probe_id_ = -1;
  std::int64_t approved_probe_id_ = -1;
  std::int64_t hover_x_micros_ = 0;
  std::int64_t hover_y_micros_ = 0;
};

#endif  // RUNNER_CANVAS_DROP_TARGET_H_
