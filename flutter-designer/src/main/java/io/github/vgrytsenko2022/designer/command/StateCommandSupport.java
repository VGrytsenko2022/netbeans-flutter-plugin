package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StatePropertyBinding;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import java.util.Map;
import java.util.Optional;

/** Exact metadata lookup; lexical and analyzer proof remain separate admission requirements. */
final class StateCommandSupport {
    private StateCommandSupport() {}

    static StatePropertyBinding field(WidgetNode selected, String fieldName) {
        var action = selected.stateBinding().filter(binding -> binding.fieldName().equals(fieldName));
        if (action.isPresent()) {
            var binding = action.orElseThrow();
            return new StatePropertyBinding(binding.fieldName(), binding.type(), binding.referenceType(),
                    StatePropertyBinding.Transform.DIRECT);
        }
        var consumer = selected.propertyBindings().values().stream()
                .filter(binding -> binding.fieldName().equals(fieldName)).findFirst().orElseThrow(() ->
                        new IllegalArgumentException("The selected widget no longer references State field '" + fieldName + "'."));
        return new StatePropertyBinding(consumer.fieldName(), consumer.type(), consumer.referenceType(),
                StatePropertyBinding.Transform.DIRECT);
    }

    static PropertyValue renameReference(PropertyValue value, Map<String, String> names) {
        if (value instanceof PropertyValue.CallbackValue callback) {
            String replacement = names.get(callback.handler());
            return replacement == null ? value : new PropertyValue.CallbackValue(replacement);
        }
        if (value instanceof PropertyValue.DartObjectReferenceValue reference && reference.libraryUri().isEmpty()) {
            String replacement = names.get(reference.rootSymbol());
            return replacement == null ? value : new PropertyValue.DartObjectReferenceValue(reference.libraryUri(),
                    replacement, reference.member(), reference.access(), reference.constant());
        }
        if (value instanceof PropertyValue.BoxDecorationValue decoration && decoration.image().isPresent()) {
            var image = decoration.image().orElseThrow();
            Optional<PropertyValue.CallbackValue> renamed = image.onError()
                    .map(callback -> (PropertyValue.CallbackValue) renameReference(callback, names));
            if (!renamed.equals(image.onError())) {
                var replacement = new PropertyValue.DecorationImageValue(image.image(), renamed, image.colorFilter(),
                        image.fit(), image.alignment(), image.centerSlice(), image.repeat(), image.matchTextDirection(),
                        image.scale(), image.opacity(), image.filterQuality(), image.invertColors(), image.isAntiAlias());
                return new PropertyValue.BoxDecorationValue(decoration.color(), Optional.of(replacement), decoration.border(),
                        decoration.borderRadius(), decoration.boxShadow(), decoration.gradient(), decoration.backgroundBlendMode(), decoration.shape());
            }
        }
        return value;
    }
}
