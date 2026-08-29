package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.MaterialThemeTokenCatalog;
import dev.flutter.netbeans.designer.model.ThemeToken;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Reviewed Material 3 theme roles exposed by the designer property editors. */
final class FlutterThemePropertyRoles {
    private FlutterThemePropertyRoles() {
    }

    static List<String> colorDisplayRoles(List<ThemeToken> allowed) {
        return allowed.stream().filter(ThemeToken::isColorSchemeToken)
                .map(FlutterThemePropertyRoles::displayRole).toList();
    }

    static List<String> textStyleDisplayRoles(List<ThemeToken> allowed) {
        return allowed.stream().filter(ThemeToken::isTextThemeToken)
                .map(FlutterThemePropertyRoles::displayRole).toList();
    }

    static Optional<ThemeToken> findColorToken(
            String value, List<ThemeToken> allowed) {
        return find(value, allowed.stream()
                .filter(ThemeToken::isColorSchemeToken).toList());
    }

    static Optional<ThemeToken> findTextStyleToken(
            String value, List<ThemeToken> allowed) {
        return find(value, allowed.stream()
                .filter(ThemeToken::isTextThemeToken).toList());
    }

    static String displayRole(ThemeToken token) {
        Objects.requireNonNull(token, "token");
        String role = MaterialThemeTokenCatalog.colorRole(token)
                .or(() -> MaterialThemeTokenCatalog.textStyleRole(token))
                .orElse(token.role());
        StringBuilder value = new StringBuilder(role.length() + 4);
        for (int index = 0; index < role.length(); index++) {
            char current = role.charAt(index);
            if (index > 0 && Character.isUpperCase(current)) {
                value.append(' ');
            }
            value.append(index == 0
                    ? Character.toUpperCase(current) : current);
        }
        return value.toString();
    }

    private static Optional<ThemeToken> find(String value, List<ThemeToken> tokens) {
        Objects.requireNonNull(value, "value");
        String normalized = value.strip();
        return tokens.stream().filter(token ->
                token.wireId().equals(normalized)
                || token.role().equals(normalized)
                || displayRole(token).equalsIgnoreCase(normalized))
                .findFirst();
    }

}
