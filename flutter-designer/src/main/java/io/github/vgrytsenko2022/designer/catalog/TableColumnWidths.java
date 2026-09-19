package io.github.vgrytsenko2022.designer.catalog;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

/**
 * Closed, bounded table-width value language, not Dart source.
 * Serialized as StringValue to retain the existing FD wire format.
 */
public final class TableColumnWidths {
    public static final int MAX_DEPTH = 8, MAX_NODES = 256, MAX_INDEX = 9999;
    public static final String VERSION = "tableColumnWidths.v1";
    public enum Family { FIXED, FLEX, FRACTION, INTRINSIC, MIN, MAX }
    public record Width(Family family, Optional<BigDecimal> value, Optional<Width> a, Optional<Width> b) {
        public Width {
            Objects.requireNonNull(family); Objects.requireNonNull(value); Objects.requireNonNull(a); Objects.requireNonNull(b);
            boolean pair = family == Family.MIN || family == Family.MAX;
            if (pair != a.isPresent() || pair != b.isPresent() || pair && value.isPresent()
                    || !pair && family != Family.INTRINSIC && value.isEmpty())
                throw new IllegalArgumentException("Invalid column width shape");
            value.ifPresent(v -> {
                if (v.precision() > 64 || Math.abs((long)v.scale()) > 999 || !Double.isFinite(v.doubleValue()) || v.signum() < 0
                        || (family == Family.FLEX || family == Family.INTRINSIC) && v.doubleValue() <= 0)
                    throw new IllegalArgumentException("Column widths must be finite and non-negative; flex must be positive");
            });
            value=value.map(BigDecimal::stripTrailingZeros);
        }
        public String encode() {
            return family.name().toLowerCase(Locale.ROOT) + "("
                    + (a.isPresent() ? a.orElseThrow().encode() + "," + b.orElseThrow().encode()
                            : value.map(TableColumnWidths::number).orElse("")) + ")";
        }
        public String dart(Function<String,String> symbol) {
            String name = switch (family) {
                case FIXED -> "FixedColumnWidth"; case FLEX -> "FlexColumnWidth";
                case FRACTION -> "FractionColumnWidth"; case INTRINSIC -> "IntrinsicColumnWidth";
                case MIN -> "MinColumnWidth"; case MAX -> "MaxColumnWidth";
            };
            return "const " + symbol.apply(name) + "("
                    + (a.isPresent() ? a.orElseThrow().dart(symbol) + ", " + b.orElseThrow().dart(symbol)
                       : (family == Family.INTRINSIC && value.isPresent() ? "flex: " : "")
                         + value.map(TableColumnWidths::number).orElse("")) + ")";
        }
        public Set<String> symbols() {
            var result = new LinkedHashSet<String>();
            dart(s -> { result.add(s); return s; });
            return Collections.unmodifiableSet(result);
        }
    }
    public static Width parse(String text) {
        var parser = new Parser(text);
        Width result = parser.width(0); parser.end();
        return result;
    }
    public static SortedMap<Integer,Width> parseMap(String text) {
        var p = new Parser(text);
        var result = new TreeMap<Integer,Width>();
        p.space();
        while (!p.done()) {
            String token = p.digits();
            if (token.isEmpty() || token.length() > 4) throw p.error("Column index must be 0..9999");
            int index = Integer.parseInt(token);
            p.expect('=');
            if (result.putIfAbsent(index,p.width(0)) != null) throw p.error("Duplicate column index " + index);
            p.space();
            if (p.done()) break;
            p.expect(';'); p.space();
            if (p.done()) throw p.error("Trailing column separator");
        }
        p.end();
        return Collections.unmodifiableSortedMap(result);
    }
    public static String encodeMap(Map<Integer,Width> values) {
        return new TreeMap<>(values).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().encode()).collect(java.util.stream.Collectors.joining(";"));
    }
    private static String number(BigDecimal value) {
        BigDecimal normalized=value.stripTrailingZeros();
        return Math.abs((long)normalized.scale()) <= 20 ? normalized.toPlainString() : normalized.toString();
    }
    private static final class Parser {
        final String text; int position, nodes;
        Parser(String text) {
            this.text = Objects.requireNonNull(text);
            if (text.length() > 16384) throw error("Table width value is too long");
        }
        Width width(int depth) {
            if (depth > MAX_DEPTH || ++nodes > MAX_NODES) throw error("Table width nesting/node limit exceeded");
            space(); int start = position;
            while (!done() && text.charAt(position) >= 'a' && text.charAt(position) <= 'z') position++;
            Family family;
            try { family = Family.valueOf(text.substring(start,position).toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { throw error("Choose fixed, flex, fraction, intrinsic, min or max"); }
            expect('('); space();
            Optional<BigDecimal> value = Optional.empty(); Optional<Width> a = Optional.empty(), b = Optional.empty();
            if (family == Family.MIN || family == Family.MAX) {
                a = Optional.of(width(depth+1)); expect(','); b = Optional.of(width(depth+1));
            } else if (family != Family.INTRINSIC || done() || text.charAt(position) != ')') {
                start = position;
                while (!done() && "0123456789.eE+-".indexOf(text.charAt(position)) >= 0) position++;
                String token = text.substring(start,position);
                if (token.length() > 80 || !token.matches("[+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]{1,3})?"))
                    throw error("Expected a finite non-negative number");
                value = Optional.of(new BigDecimal(token));
            }
            expect(')');
            return new Width(family,value,a,b);
        }
        String digits() {
            space(); int start=position;
            while (!done() && Character.isDigit(text.charAt(position)) && text.charAt(position) <= '9') position++;
            return text.substring(start,position);
        }
        void space() { while (!done() && " \t\r\n".indexOf(text.charAt(position)) >= 0) position++; }
        void expect(char c) { space(); if (done() || text.charAt(position++) != c) throw error("Expected '" + c + "'"); }
        void end() { space(); if (!done()) throw error("Unexpected trailing data"); }
        boolean done() { return position == text.length(); }
        IllegalArgumentException error(String message) { return new IllegalArgumentException(message + " at offset " + position); }
    }
    private TableColumnWidths() {}
}
