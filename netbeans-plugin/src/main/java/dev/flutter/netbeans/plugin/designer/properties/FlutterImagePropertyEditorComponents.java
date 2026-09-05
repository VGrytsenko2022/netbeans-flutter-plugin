package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyEditor;
import java.util.Objects;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
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

    static Component nullableCustomEditor(
            PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding,
            PropertyEnv environment) {
        return new NullableImageProviderPanel(editor, binding, environment);
    }

    private static final class NullableImageProviderPanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private static final String DESCRIPTION =
                "Edits required ImageIcon.image: explicit None (Dart null), or a typed "
                + "declared AssetImage, ExactAssetImage, or ResizeImage. None is not <not set>; "
                + "the required positional argument cannot be omitted or reset.";
        private final JCheckBox none = new JCheckBox("None (empty image icon)");
        private final FlutterImageProviderEditorComponent providerEditor;

        NullableImageProviderPanel(PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            if (binding.optional()) {
                throw new IllegalArgumentException("ImageIcon.image must be a required nullable property.");
            }
            setLayout(new BorderLayout());
            setName("flutter.imageIcon.imageProvider.custom");
            getAccessibleContext().setAccessibleName("ImageIcon image-provider editor");
            getAccessibleContext().setAccessibleDescription(DESCRIPTION);
            none.setName("flutter.imageIcon.imageProvider.none");
            none.getAccessibleContext().setAccessibleDescription(
                    "Stores explicit null without removing ImageIcon.image. Uncheck to choose an image provider.");
            PropertyValue initial = initialValue().explicitValue().orElseThrow(
                    () -> new IllegalArgumentException("Required ImageIcon.image has no explicit value."));
            boolean unresolved = initial instanceof PropertyValue.ImageProviderValue value
                    && value.isUnresolved();
            providerEditor = new FlutterImageProviderEditorComponent(
                    ImageProviderPanel.assetChoices(environment), "ImageIcon.image",
                    "flutter.imageIcon.imageProvider",
                    unresolved
                            ? FlutterImageProviderEditorComponent.EmptySelectionPolicy.PRESERVE_INITIAL_UNRESOLVED
                            : FlutterImageProviderEditorComponent.EmptySelectionPolicy.REQUIRE_DECLARED_ASSET,
                    this::refresh);
            if (initial instanceof PropertyValue.ImageProviderValue value) {
                providerEditor.populate(value);
            } else if (!(initial instanceof PropertyValue.NullValue)) {
                throw new IllegalArgumentException("ImageIcon.image requires null or a typed image provider.");
            }
            none.setSelected(initial instanceof PropertyValue.NullValue);
            none.addActionListener(ignored -> refresh());
            add(none, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(providerEditor);
            scroll.setBorder(null);
            add(scroll, BorderLayout.CENTER);
            refresh();
            activate();
        }

        private void refresh() {
            providerEditor.updateEnabledState(!none.isSelected());
            try {
                PropertyValue value = none.isSelected()
                        ? new PropertyValue.NullValue() : providerEditor.value();
                clearInvalid(providerEditor, DESCRIPTION);
                markValid(FlutterPropertyCellValue.explicit(value));
            } catch (IllegalArgumentException failure) {
                markInvalid(ImageProviderPanel.concreteMessage(failure), providerEditor);
            }
        }
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
                    FlutterImageProviderEditorComponent.EmptySelectionPolicy
                            .PRESERVE_INITIAL_UNRESOLVED,
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
