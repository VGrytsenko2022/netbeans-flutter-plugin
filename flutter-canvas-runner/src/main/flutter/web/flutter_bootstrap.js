{{flutter_js}}
{{flutter_build_config}}

_flutter.loader.load({
  onEntrypointLoaded: async function onEntrypointLoaded(engineInitializer) {
    const engine = await engineInitializer.initializeEngine({
      multiViewEnabled: true,
      canvasKitBaseUrl: 'canvaskit/',
      fontFallbackBaseUrl: 'assets/fonts/',
    });
    const app = await engine.runApp();
    const hostElement = document.getElementById('flutter-host');
    if (!hostElement) {
      throw new Error('Flutter Web Canvas host element is missing.');
    }
    const viewId = app.addView({hostElement});
    globalThis.netBeansCanvasBridge.bindView(app, viewId);
  },
});
