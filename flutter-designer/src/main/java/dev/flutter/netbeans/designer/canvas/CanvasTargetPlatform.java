package dev.flutter.netbeans.designer.canvas;

/**
 * Concrete Flutter runtime platform used to render one Canvas presentation.
 *
 * <p>Typical native pairings use Android or iOS for mobile/tablet modes and a
 * desktop host for desktop mode. The platform remains independent from the
 * responsive development mode, however, so a web runtime can render mobile,
 * tablet or desktop viewports.</p>
 */
public enum CanvasTargetPlatform {
    ANDROID,
    IOS,
    WINDOWS,
    MACOS,
    LINUX,
    WEB
}
