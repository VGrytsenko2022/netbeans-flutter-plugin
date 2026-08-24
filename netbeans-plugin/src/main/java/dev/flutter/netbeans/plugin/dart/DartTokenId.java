package dev.flutter.netbeans.plugin.dart;

import java.util.Arrays;
import java.util.Collection;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/** Token types and NetBeans lexer-language registration for Dart source files. */
public enum DartTokenId implements TokenId {
    WHITESPACE("whitespace"),
    KEYWORD("keyword"),
    BUILT_IN_TYPE("type"),
    LITERAL("literal"),
    IDENTIFIER("identifier"),
    NUMBER("number"),
    STRING("string"),
    STRING_INTERPOLATION("string-interpolation"),
    COMMENT("comment"),
    DOC_COMMENT("doc-comment"),
    ANNOTATION("annotation"),
    OPERATOR("operator"),
    SEPARATOR("separator"),
    ERROR("error");

    public static final String MIME_TYPE = "text/x-dart";
    private static final Language<DartTokenId> LANGUAGE = new LanguageHierarchy<DartTokenId>() {
        @Override
        protected Collection<DartTokenId> createTokenIds() {
            return Arrays.asList(DartTokenId.values());
        }

        @Override
        protected Lexer<DartTokenId> createLexer(LexerRestartInfo<DartTokenId> info) {
            return new DartLexer(info);
        }

        @Override
        protected String mimeType() {
            return MIME_TYPE;
        }
    }.language();

    private final String primaryCategory;

    DartTokenId(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }

    @Override
    public String primaryCategory() {
        return primaryCategory;
    }

    @MimeRegistration(mimeType = MIME_TYPE, service = Language.class, position = 100)
    public static Language<DartTokenId> language() {
        return LANGUAGE;
    }
}
