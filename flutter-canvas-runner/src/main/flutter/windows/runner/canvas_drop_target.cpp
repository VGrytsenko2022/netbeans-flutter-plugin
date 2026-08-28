#include "canvas_drop_target.h"

#include <flutter/method_result_functions.h>
#include <flutter/standard_method_codec.h>
#include <objbase.h>

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <cwchar>
#include <memory>
#include <limits>
#include <mutex>
#include <optional>
#include <string>
#include <string_view>
#include <utility>

namespace {

constexpr wchar_t kTokenPrefix[] = L"nbfdnd:v1:";
constexpr std::size_t kUuidLength = 36;
constexpr std::size_t kUuidCount = 2;
constexpr std::size_t kTokenLength =
    (sizeof(kTokenPrefix) / sizeof(kTokenPrefix[0]) - 1) +
    kUuidLength * kUuidCount + 1;
constexpr SIZE_T kMaximumUnicodeStorageBytes = 256;
constexpr std::int64_t kCoordinateMicros = 1'000'000;
constexpr char kChannelName[] = "dev.flutter.netbeans/canvas_palette_drop";
constexpr char kDropPrepareMethodName[] = "paletteDropPrepare";
constexpr char kDropCommitMethodName[] = "paletteDropCommit";
constexpr char kDropCancelMethodName[] = "paletteDropCancel";
constexpr char kHoverMethodName[] = "paletteHover";
constexpr char kHoverLeaveMethodName[] = "paletteHoverLeave";
constexpr char kAvailabilityMethodName[] = "isAvailable";
constexpr char kInvalidateHoverMethodName[] = "invalidateHover";
constexpr DWORD kDropReplyTimeoutMilliseconds = 250;

enum class DropReplyOutcome : int {
  kPending = 0,
  kAccepted = 1,
  kRejected = 2,
};

class DropReplyState final {
 public:
  DropReplyState()
      : event_(::CreateEventW(nullptr, TRUE, FALSE, nullptr)) {}

  ~DropReplyState() {
    if (event_ != nullptr) {
      ::CloseHandle(event_);
    }
  }

  DropReplyState(const DropReplyState&) = delete;
  DropReplyState& operator=(const DropReplyState&) = delete;

  HANDLE event() const { return event_; }

  void Complete(bool accepted) {
    int expected = static_cast<int>(DropReplyOutcome::kPending);
    const int outcome = static_cast<int>(
        accepted ? DropReplyOutcome::kAccepted : DropReplyOutcome::kRejected);
    if (outcome_.compare_exchange_strong(expected, outcome,
                                         std::memory_order_release,
                                         std::memory_order_relaxed) &&
        event_ != nullptr) {
      ::SetEvent(event_);
    }
  }

  bool accepted() const {
    return outcome_.load(std::memory_order_acquire) ==
           static_cast<int>(DropReplyOutcome::kAccepted);
  }

 private:
  HANDLE event_ = nullptr;
  std::atomic<int> outcome_{
      static_cast<int>(DropReplyOutcome::kPending)};
};

bool WaitForDropReply(const std::shared_ptr<DropReplyState>& reply,
                      HANDLE shutdown_event) {
  if (reply->event() == nullptr || shutdown_event == nullptr) {
    return false;
  }
  HANDLE handles[] = {reply->event(), shutdown_event};
  DWORD signaled_index = 0;
  const HRESULT wait = ::CoWaitForMultipleHandles(
      static_cast<DWORD>(COWAIT_DISPATCH_CALLS |
                         COWAIT_DISPATCH_WINDOW_MESSAGES),
      kDropReplyTimeoutMilliseconds, 2, handles, &signaled_index);
  return SUCCEEDED(wait) && signaled_index == 0 && reply->accepted();
}

class ComSelfReference final {
 public:
  explicit ComSelfReference(CanvasDropTarget* target) : target_(target) {
    target_->AddRef();
  }
  ~ComSelfReference() { target_->Release(); }

  ComSelfReference(const ComSelfReference&) = delete;
  ComSelfReference& operator=(const ComSelfReference&) = delete;

 private:
  CanvasDropTarget* target_;
};

bool IsAsciiHex(wchar_t character) {
  return (character >= L'0' && character <= L'9') ||
         (character >= L'a' && character <= L'f') ||
         (character >= L'A' && character <= L'F');
}

bool IsUuidLike(std::wstring_view value) {
  if (value.size() != kUuidLength) {
    return false;
  }
  for (std::size_t index = 0; index < value.size(); ++index) {
    const bool hyphen =
        index == 8 || index == 13 || index == 18 || index == 23;
    if (hyphen ? value[index] != L'-' : !IsAsciiHex(value[index])) {
      return false;
    }
  }
  return true;
}

std::optional<std::string> ReadPaletteToken(IDataObject* data_object) {
  if (data_object == nullptr) {
    return std::nullopt;
  }

  FORMATETC format{};
  format.cfFormat = CF_UNICODETEXT;
  format.ptd = nullptr;
  format.dwAspect = DVASPECT_CONTENT;
  format.lindex = -1;
  format.tymed = TYMED_HGLOBAL;
  if (FAILED(data_object->QueryGetData(&format))) {
    return std::nullopt;
  }

  STGMEDIUM medium{};
  if (FAILED(data_object->GetData(&format, &medium))) {
    return std::nullopt;
  }

  std::optional<std::string> result;
  if (medium.tymed == TYMED_HGLOBAL && medium.hGlobal != nullptr) {
    const SIZE_T byte_count = ::GlobalSize(medium.hGlobal);
    if (byte_count >= (kTokenLength + 1) * sizeof(wchar_t) &&
        byte_count <= kMaximumUnicodeStorageBytes &&
        byte_count % sizeof(wchar_t) == 0) {
      const auto* text = static_cast<const wchar_t*>(
          ::GlobalLock(medium.hGlobal));
      if (text != nullptr) {
        const std::size_t character_capacity =
            byte_count / sizeof(wchar_t);
        const wchar_t* terminator = std::find(
            text,
            text + std::min(character_capacity, kTokenLength + 1),
            L'\0');
        if (terminator == text + kTokenLength) {
          const std::wstring_view token(text, kTokenLength);
          const std::wstring_view prefix(
              kTokenPrefix,
              sizeof(kTokenPrefix) / sizeof(kTokenPrefix[0]) - 1);
          const std::size_t separator = prefix.size() + kUuidLength;
          if (token.substr(0, prefix.size()) == prefix &&
              token[separator] == L':' &&
              IsUuidLike(token.substr(prefix.size(), kUuidLength)) &&
              IsUuidLike(token.substr(separator + 1, kUuidLength))) {
            std::string ascii;
            ascii.reserve(token.size());
            bool ascii_only = true;
            for (wchar_t character : token) {
              if (character < 0 || character > 0x7f) {
                ascii_only = false;
                break;
              }
              ascii.push_back(static_cast<char>(character));
            }
            if (ascii_only) {
              result = std::move(ascii);
            }
          }
        }
        ::GlobalUnlock(medium.hGlobal);
      }
    }
  }
  ::ReleaseStgMedium(&medium);
  return result;
}

struct NormalizedPoint {
  std::int64_t x_micros;
  std::int64_t y_micros;
};

std::optional<NormalizedPoint> NormalizeClientPoint(
    HWND window,
    POINTL screen_point) {
  if (window == nullptr || !::IsWindow(window)) {
    return std::nullopt;
  }

  POINT client_point{screen_point.x, screen_point.y};
  RECT client_bounds{};
  if (!::ScreenToClient(window, &client_point) ||
      !::GetClientRect(window, &client_bounds)) {
    return std::nullopt;
  }

  const LONG width = client_bounds.right - client_bounds.left;
  const LONG height = client_bounds.bottom - client_bounds.top;
  const LONG x = client_point.x - client_bounds.left;
  const LONG y = client_point.y - client_bounds.top;
  if (width <= 0 || height <= 0 || x < 0 || y < 0 || x >= width ||
      y >= height) {
    return std::nullopt;
  }

  return NormalizedPoint{
      static_cast<std::int64_t>(x) * kCoordinateMicros / width,
      static_cast<std::int64_t>(y) * kCoordinateMicros / height};
}

void SetMoveEffect(bool accepted, DWORD allowed_effects, DWORD* effect) {
  if (effect != nullptr) {
    *effect = accepted && (allowed_effects & DROPEFFECT_MOVE) != 0
                  ? DROPEFFECT_MOVE
                  : DROPEFFECT_NONE;
  }
}

flutter::EncodableMap DropArguments(const std::string& token,
                                    const NormalizedPoint& point,
                                    std::int64_t generation,
                                    std::int64_t probe_id) {
  return flutter::EncodableMap{
      {flutter::EncodableValue("token"), flutter::EncodableValue(token)},
      {flutter::EncodableValue("xMicros"),
       flutter::EncodableValue(point.x_micros)},
      {flutter::EncodableValue("yMicros"),
       flutter::EncodableValue(point.y_micros)},
      {flutter::EncodableValue("generation"),
       flutter::EncodableValue(generation)},
      {flutter::EncodableValue("probeId"),
       flutter::EncodableValue(probe_id)}};
}

}  // namespace

struct CanvasDropTarget::HoverCallbackBridge {
  explicit HoverCallbackBridge(CanvasDropTarget* initial_owner)
      : owner(initial_owner) {}

  std::mutex mutex;
  CanvasDropTarget* owner;
};

CanvasDropTarget::CanvasDropTarget(
    HWND flutter_view,
    flutter::BinaryMessenger* messenger)
    : flutter_view_(flutter_view),
      method_channel_(messenger, kChannelName,
                      &flutter::StandardMethodCodec::GetInstance()),
      hover_callback_bridge_(std::make_shared<HoverCallbackBridge>(this)),
      shutdown_event_(::CreateEventW(nullptr, TRUE, FALSE, nullptr)),
      channel_handler_installed_(true) {
  method_channel_.SetMethodCallHandler(
      [this](const flutter::MethodCall<flutter::EncodableValue>& call,
             std::unique_ptr<
                 flutter::MethodResult<flutter::EncodableValue>> result) {
        if (call.method_name() == kAvailabilityMethodName) {
          result->Success(
              flutter::EncodableValue(registered_ && !shutdown_));
          return;
        }
        if (call.method_name() == kInvalidateHoverMethodName) {
          result->Success();
          InvalidateHoverApproval();
          return;
        }
        result->NotImplemented();
      });
}

CanvasDropTarget::~CanvasDropTarget() {
  DetachHoverCallbackBridge();
  if (shutdown_event_ != nullptr) {
    ::CloseHandle(shutdown_event_);
    shutdown_event_ = nullptr;
  }
}

void CanvasDropTarget::MarkRegistered() {
  registered_ = !shutdown_ && shutdown_event_ != nullptr;
}

void CanvasDropTarget::Shutdown() {
  if (shutdown_) {
    return;
  }
  shutdown_ = true;
  registered_ = false;
  drop_wait_cancelled_ = true;
  if (shutdown_event_ != nullptr) {
    ::SetEvent(shutdown_event_);
  }
  DetachHoverCallbackBridge();
  if (channel_handler_installed_) {
    // FlutterWindow calls Shutdown before destroying its engine, so this is
    // the final and only legal messenger access during teardown.
    method_channel_.SetMethodCallHandler(nullptr);
    channel_handler_installed_ = false;
  }
  FinishDrag(false);
}

void CanvasDropTarget::DetachHoverCallbackBridge() {
  const auto bridge = hover_callback_bridge_;
  if (bridge == nullptr) {
    return;
  }
  std::lock_guard<std::mutex> lock(bridge->mutex);
  bridge->owner = nullptr;
}

void CanvasDropTarget::BeginDrag(std::string token, POINTL point) {
  if (generation_counter_ == std::numeric_limits<std::int64_t>::max()) {
    FinishDrag(false);
    return;
  }
  drag_active_ = true;
  hover_point_valid_ = false;
  approved_probe_id_ = -1;
  palette_token_ = std::move(token);
  active_generation_ = ++generation_counter_;
  desired_probe_id_ = -1;
  last_sent_probe_id_ = -1;
  UpdateHoverPoint(point);
}

bool CanvasDropTarget::AdvanceProbeId() {
  if (probe_id_counter_ == std::numeric_limits<std::int64_t>::max()) {
    FinishDrag(true);
    return false;
  }
  desired_probe_id_ = ++probe_id_counter_;
  approved_probe_id_ = -1;
  return true;
}

void CanvasDropTarget::UpdateHoverPoint(POINTL point) {
  if (!drag_active_) {
    return;
  }
  const std::optional<NormalizedPoint> normalized =
      NormalizeClientPoint(flutter_view_, point);
  if (!normalized.has_value()) {
    // Do not leave Flutter's last approved overlay visible when Windows
    // reports a point outside the exact FlutterView (or the view temporarily
    // has no usable client geometry). Ending this native generation is the
    // conservative choice; a fresh Palette drag can negotiate a new one.
    FinishDrag(true);
    return;
  }
  if (hover_point_valid_ &&
      hover_x_micros_ == normalized->x_micros &&
      hover_y_micros_ == normalized->y_micros) {
    return;
  }
  hover_point_valid_ = true;
  hover_x_micros_ = normalized->x_micros;
  hover_y_micros_ = normalized->y_micros;
  if (AdvanceProbeId()) {
    ScheduleHoverProbe();
  }
}

void CanvasDropTarget::InvalidateHoverApproval() {
  if (shutdown_) {
    return;
  }
  approved_probe_id_ = -1;
  if (drag_active_ && hover_point_valid_ && AdvanceProbeId()) {
    ScheduleHoverProbe();
  }
}

void CanvasDropTarget::ScheduleHoverProbe() {
  if (shutdown_ || !channel_handler_installed_ || !drag_active_ ||
      !hover_point_valid_ || hover_probe_in_flight_ ||
      desired_probe_id_ <= last_sent_probe_id_) {
    return;
  }
  const std::int64_t generation = active_generation_;
  const std::int64_t probe_id = desired_probe_id_;
  const std::int64_t x_micros = hover_x_micros_;
  const std::int64_t y_micros = hover_y_micros_;
  last_sent_probe_id_ = probe_id;
  hover_probe_in_flight_ = true;

  flutter::EncodableMap arguments{
      {flutter::EncodableValue("token"),
       flutter::EncodableValue(palette_token_)},
      {flutter::EncodableValue("xMicros"),
       flutter::EncodableValue(x_micros)},
      {flutter::EncodableValue("yMicros"),
       flutter::EncodableValue(y_micros)},
      {flutter::EncodableValue("generation"),
       flutter::EncodableValue(generation)},
      {flutter::EncodableValue("probeId"),
       flutter::EncodableValue(probe_id)}};

  const auto bridge = hover_callback_bridge_;
  auto complete = [bridge, generation, probe_id](bool approved) {
    CanvasDropTarget* owner = nullptr;
    {
      std::lock_guard<std::mutex> lock(bridge->mutex);
      owner = bridge->owner;
      if (owner != nullptr) {
        owner->AddRef();
      }
    }
    if (owner != nullptr) {
      owner->CompleteHoverProbe(generation, probe_id, approved);
      owner->Release();
    }
  };
  method_channel_.InvokeMethod(
      kHoverMethodName,
      std::make_unique<flutter::EncodableValue>(std::move(arguments)),
      std::make_unique<
          flutter::MethodResultFunctions<flutter::EncodableValue>>(
          [complete](const flutter::EncodableValue* value) {
            const bool* approved =
                value == nullptr ? nullptr : std::get_if<bool>(value);
            complete(approved != nullptr && *approved);
          },
          [complete](const std::string&, const std::string&,
                     const flutter::EncodableValue*) { complete(false); },
          [complete]() { complete(false); }));
}

void CanvasDropTarget::CompleteHoverProbe(std::int64_t generation,
                                          std::int64_t probe_id,
                                          bool approved) {
  hover_probe_in_flight_ = false;
  if (shutdown_) {
    return;
  }
  if (drag_active_ && generation == active_generation_ &&
      probe_id == desired_probe_id_ && hover_point_valid_) {
    approved_probe_id_ = approved ? probe_id : -1;
  }
  ScheduleHoverProbe();
}

void CanvasDropTarget::SendHoverLeave(std::int64_t generation) {
  if (shutdown_ || !channel_handler_installed_) {
    return;
  }
  flutter::EncodableMap arguments{
      {flutter::EncodableValue("generation"),
       flutter::EncodableValue(generation)}};
  method_channel_.InvokeMethod(
      kHoverLeaveMethodName,
      std::make_unique<flutter::EncodableValue>(std::move(arguments)));
}

void CanvasDropTarget::FinishDrag(bool send_leave) {
  const bool was_active = drag_active_;
  const std::int64_t generation = active_generation_;
  drag_active_ = false;
  hover_point_valid_ = false;
  approved_probe_id_ = -1;
  desired_probe_id_ = -1;
  last_sent_probe_id_ = -1;
  palette_token_.clear();
  if (send_leave && was_active) {
    SendHoverLeave(generation);
  }
}

bool CanvasDropTarget::IsLatestHoverApproved(POINTL point) const {
  if (!drag_active_ || !hover_point_valid_ ||
      approved_probe_id_ < 0 || approved_probe_id_ != desired_probe_id_) {
    return false;
  }
  const std::optional<NormalizedPoint> normalized =
      NormalizeClientPoint(flutter_view_, point);
  return normalized.has_value() &&
         normalized->x_micros == hover_x_micros_ &&
         normalized->y_micros == hover_y_micros_;
}

HRESULT STDMETHODCALLTYPE CanvasDropTarget::QueryInterface(
    REFIID interface_id,
    void** object) {
  if (object == nullptr) {
    return E_POINTER;
  }
  *object = nullptr;
  if (::IsEqualIID(interface_id, IID_IUnknown) ||
      ::IsEqualIID(interface_id, IID_IDropTarget)) {
    *object = static_cast<IDropTarget*>(this);
    AddRef();
    return S_OK;
  }
  return E_NOINTERFACE;
}

ULONG STDMETHODCALLTYPE CanvasDropTarget::AddRef() {
  return reference_count_.fetch_add(1, std::memory_order_relaxed) + 1;
}

ULONG STDMETHODCALLTYPE CanvasDropTarget::Release() {
  const ULONG remaining =
      reference_count_.fetch_sub(1, std::memory_order_acq_rel) - 1;
  if (remaining == 0) {
    delete this;
  }
  return remaining;
}

HRESULT STDMETHODCALLTYPE CanvasDropTarget::DragEnter(
    IDataObject* data_object,
    DWORD key_state,
    POINTL point,
    DWORD* effect) {
  (void)key_state;
  if (shutdown_) {
    if (effect != nullptr) {
      *effect = DROPEFFECT_NONE;
    }
    return effect == nullptr ? E_INVALIDARG : S_OK;
  }
  if (drop_wait_in_progress_) {
    drop_wait_cancelled_ = true;
    if (effect != nullptr) {
      *effect = DROPEFFECT_NONE;
    }
    return effect == nullptr ? E_INVALIDARG : S_OK;
  }
  if (effect == nullptr) {
    FinishDrag(true);
    return E_INVALIDARG;
  }
  const DWORD allowed_effects = *effect;
  FinishDrag(true);
  const std::optional<std::string> token = ReadPaletteToken(data_object);
  if ((allowed_effects & DROPEFFECT_MOVE) != 0 && token.has_value()) {
    BeginDrag(*token, point);
  }
  // Flutter owns hit testing. DragEnter therefore fails closed while its first
  // asynchronous hover probe is pending.
  SetMoveEffect(false, allowed_effects, effect);
  return S_OK;
}

HRESULT STDMETHODCALLTYPE CanvasDropTarget::DragOver(
    DWORD key_state,
    POINTL point,
    DWORD* effect) {
  (void)key_state;
  if (shutdown_) {
    if (effect != nullptr) {
      *effect = DROPEFFECT_NONE;
    }
    return effect == nullptr ? E_INVALIDARG : S_OK;
  }
  if (drop_wait_in_progress_) {
    drop_wait_cancelled_ = true;
    if (effect != nullptr) {
      *effect = DROPEFFECT_NONE;
    }
    return effect == nullptr ? E_INVALIDARG : S_OK;
  }
  if (effect == nullptr) {
    FinishDrag(true);
    return E_INVALIDARG;
  }
  const DWORD allowed_effects = *effect;
  UpdateHoverPoint(point);
  SetMoveEffect(
      (allowed_effects & DROPEFFECT_MOVE) != 0 &&
          IsLatestHoverApproved(point),
      allowed_effects, effect);
  return S_OK;
}

HRESULT STDMETHODCALLTYPE CanvasDropTarget::DragLeave() {
  if (shutdown_) {
    return S_OK;
  }
  if (drop_wait_in_progress_) {
    drop_wait_cancelled_ = true;
    return S_OK;
  }
  FinishDrag(true);
  return S_OK;
}

HRESULT STDMETHODCALLTYPE CanvasDropTarget::Drop(
    IDataObject* data_object,
    DWORD key_state,
    POINTL point,
    DWORD* effect) {
  ComSelfReference self_reference(this);
  (void)key_state;
  if (shutdown_) {
    if (effect != nullptr) {
      *effect = DROPEFFECT_NONE;
    }
    return effect == nullptr ? E_INVALIDARG : S_OK;
  }
  if (drop_wait_in_progress_) {
    drop_wait_cancelled_ = true;
    if (effect != nullptr) {
      *effect = DROPEFFECT_NONE;
    }
    return effect == nullptr ? E_INVALIDARG : S_OK;
  }
  if (effect == nullptr) {
    FinishDrag(true);
    return E_INVALIDARG;
  }

  const DWORD allowed_effects = *effect;
  *effect = DROPEFFECT_NONE;
  UpdateHoverPoint(point);
  const std::optional<NormalizedPoint> normalized =
      NormalizeClientPoint(flutter_view_, point);
  const bool exact_hover_point =
      normalized.has_value() && hover_point_valid_ &&
      normalized->x_micros == hover_x_micros_ &&
      normalized->y_micros == hover_y_micros_;
  const bool latest_probe_approved =
      approved_probe_id_ >= 0 && approved_probe_id_ == desired_probe_id_;
  const bool latest_probe_in_flight =
      hover_probe_in_flight_ && last_sent_probe_id_ == desired_probe_id_;
  if ((allowed_effects & DROPEFFECT_MOVE) == 0 || !drag_active_ ||
      desired_probe_id_ < 0 || !exact_hover_point ||
      (!latest_probe_approved && !latest_probe_in_flight)) {
    FinishDrag(true);
    return S_OK;
  }

  const std::optional<std::string> token = ReadPaletteToken(data_object);
  if (!token.has_value() || *token != palette_token_) {
    FinishDrag(true);
    return S_OK;
  }

  const std::int64_t generation = active_generation_;
  const std::int64_t probe_id = desired_probe_id_;

  const auto reply = std::make_shared<DropReplyState>();
  FinishDrag(false);
  drop_wait_in_progress_ = true;
  drop_wait_cancelled_ = false;
  method_channel_.InvokeMethod(
      kDropPrepareMethodName,
      std::make_unique<flutter::EncodableValue>(
          DropArguments(*token, *normalized, generation, probe_id)),
      std::make_unique<
          flutter::MethodResultFunctions<flutter::EncodableValue>>(
          [reply](const flutter::EncodableValue* value) {
            const bool* accepted =
                value == nullptr ? nullptr : std::get_if<bool>(value);
            reply->Complete(accepted != nullptr && *accepted);
          },
          [reply](const std::string&, const std::string&,
                  const flutter::EncodableValue*) { reply->Complete(false); },
          [reply]() { reply->Complete(false); }));

  // A user can release immediately after entering the FlutterView, before the
  // asynchronous hover callback reaches this native object. Admit that fast
  // path only when the exact latest probe has already been sent and remains in
  // flight. The hover probe and prepare call share one ordered MethodChannel,
  // so Flutter receives the hover first and still authoritatively validates
  // generation, token, probe, point, presentation, and semantic target.
  //
  // IDropTarget::Drop is synchronous, while the Flutter MethodChannel is not.
  // CoWaitForMultipleHandles is the STA-safe bounded wait: it dispatches COM
  // calls and Windows messages so the platform-thread Flutter reply can make
  // progress without an unbounded UI-thread block.
  const bool flutter_accepted = WaitForDropReply(reply, shutdown_event_);
  const bool cancelled = drop_wait_cancelled_;
  drop_wait_in_progress_ = false;
  drop_wait_cancelled_ = false;
  if (!shutdown_) {
    const char* terminal_method = flutter_accepted && !cancelled
                                      ? kDropCommitMethodName
                                      : kDropCancelMethodName;
    method_channel_.InvokeMethod(
        terminal_method,
        std::make_unique<flutter::EncodableValue>(
            DropArguments(*token, *normalized, generation, probe_id)));
    SendHoverLeave(generation);
  }

  // ACTION_MOVE is the NetBeans Palette's offered contract. Palette items are
  // immutable prototypes, so reporting MOVE never removes an item from it.
  // A positive prepare result emits no Java intent. Only this MOVE branch
  // sends the ordered single-use commit, so a timeout/NONE can never mutate
  // later. The Java mutation edge may still reject a committed intent (for
  // example, if its opaque token expired); no synchronous Java ack exists in
  // this protocol slice.
  *effect = flutter_accepted && !cancelled && !shutdown_
                ? DROPEFFECT_MOVE
                : DROPEFFECT_NONE;
  return S_OK;
}
