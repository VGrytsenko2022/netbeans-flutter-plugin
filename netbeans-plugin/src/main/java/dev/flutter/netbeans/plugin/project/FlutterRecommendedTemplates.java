package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.plugin.designer.wizard.FlutterDesignerFormWizardIterator;
import org.netbeans.spi.project.ui.RecommendedTemplates;

/** Restricts the Dart template category to Flutter projects. */
final class FlutterRecommendedTemplates implements RecommendedTemplates {
    static final String DART_TEMPLATE_CATEGORY = "dart";

    @Override
    public String[] getRecommendedTypes() {
        return new String[]{
            DART_TEMPLATE_CATEGORY,
            FlutterDesignerFormWizardIterator.TEMPLATE_CATEGORY
        };
    }
}
