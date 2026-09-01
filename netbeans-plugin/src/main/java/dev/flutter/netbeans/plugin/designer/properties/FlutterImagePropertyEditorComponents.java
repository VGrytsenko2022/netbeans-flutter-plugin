package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyEditor;
import java.util.Objects;
import javax.swing.JScrollPane;
import org.openide.explorer.propertysheet.PropertyEnv;

/** NetBeans custom editor for Image.image's required typed ImageProvider. */
final class FlutterImagePropertyEditorComponents {
    private FlutterImagePropertyEditorComponents() {
    }

    static Component customEditor(
            PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding,
            PropertyEnv environment) {
        return new ImageProviderPanel(editor, binding, environment);
    }

    private static final class ImageProviderPanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private static final String BASE_DESCRIPTION =
                "Edits required Image.image with a declared Flutter asset and "
                + "typed AssetImage, ExactAssetImage, or ResizeImage settings.";
        private final FlutterImageProviderEditorComponent providerEditor;

        ImageProviderPanel(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            if (binding.optional()) {
                throw new IllegalArgumentException(
                        "The direct ImageProvider editor requires a non-optional property.");
            }
            setLayout(new BorderLayout());
            setName("flutter.image.imageProvider.custom");
            getAccessibleContext().setAccessibleName("Image image-provider editor");
            getAccessibleContext().setAccessibleDescription(BASE_DESCRIPTION);

            providerEditor = new FlutterImageProviderEditorComponent(
                    assetChoices(environment),
                    "Image.image",
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX,
                    this::refresh);
            PropertyValue.ImageProviderValue initial = initialValue().explicitValue()
                    .map(PropertyValue.ImageProviderValue.class::cast)
                    .orElseThrow(() -> new IllegalArgumentException(
                    "Required Image.image has no explicit ImageProvider value."));
            providerEditor.populate(initial);
            JScrollPane scroll = new JScrollPane(providerEditor);
            scroll.setBorder(null);
            add(scroll, BorderLayout.CENTER);
            refresh();
            activate();
        }

        private void refresh() {
            try {
                PropertyValue.ImageProviderValue value = providerEditor.value();
                clearInvalid(providerEditor, BASE_DESCRIPTION);
                markValid(FlutterPropertyCellValue.explicit(value));
            } catch (IllegalArgumentException failure) {
                markInvalid(concreteMessage(failure), providerEditor);
            }
        }

        private static FlutterImageAssetChoices assetChoices(
                PropertyEnv environment) {
            if (environment == null || environment.getFeatureDescriptor() == null) {
                return FlutterImageAssetChoices.empty();
            }
            Object value = environment.getFeatureDescriptor().getValue(
                    FlutterImageAssetChoices.FEATURE_ATTRIBUTE);
            return value instanceof FlutterImageAssetChoices choices
                    ? choices : FlutterImageAssetChoices.empty();
        }

        private static String concreteMessage(RuntimeException failure) {
            String message = Objects.toString(failure.getMessage(), "").strip();
            return message.isEmpty()
                    ? failure.getClass().getSimpleName() : message;
        }
    }
}
