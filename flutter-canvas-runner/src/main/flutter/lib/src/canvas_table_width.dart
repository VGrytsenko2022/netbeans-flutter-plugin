import 'package:flutter/rendering.dart';

/// Closed local value grammar. Never evaluates Dart or any project-owned code.
TableColumnWidth canvasTableWidth(String value) {
  final parser = _WidthParser(value);
  final result = parser.width(0);
  parser.end();
  return result;
}

Map<int, TableColumnWidth> canvasTableWidths(String value) {
  final parser = _WidthParser(value);
  final result = <int, TableColumnWidth>{};
  parser.space();
  while (!parser.done) {
    final start = parser.position;
    while (!parser.done &&
        RegExp(r'[0-9]').hasMatch(parser.text[parser.position])) {
      parser.position++;
    }
    final token = parser.text.substring(start, parser.position);
    if (token.isEmpty || token.length > 4) {
      throw parser.error('Column index must be 0..9999');
    }
    final index = int.parse(token);
    parser.expect('=');
    if (result.containsKey(index)) {
      throw parser.error('Duplicate column index $index');
    }
    result[index] = parser.width(0);
    parser.space();
    if (parser.done) break;
    parser.expect(';');
    parser.space();
    if (parser.done) throw parser.error('Trailing column separator');
  }
  parser.end();
  return Map.unmodifiable(result);
}

class _WidthParser {
  _WidthParser(this.text) {
    if (text.length > 16384) throw error('Table width value is too long');
  }
  final String text;
  int position = 0, nodes = 0;
  bool get done => position == text.length;
  FormatException error(String message) =>
      FormatException('$message at offset $position');
  void space() {
    while (!done && ' \t\r\n'.contains(text[position])) {
      position++;
    }
  }

  void expect(String value) {
    space();
    if (done || text[position++] != value) throw error('Expected $value');
  }

  void end() {
    space();
    if (!done) throw error('Unexpected trailing data');
  }

  TableColumnWidth width(int depth) {
    if (depth > 8 || ++nodes > 256) {
      throw error('Table width nesting/node limit exceeded');
    }
    space();
    var start = position;
    while (!done && RegExp(r'[a-z]').hasMatch(text[position])) {
      position++;
    }
    final family = text.substring(start, position);
    if (!const {
      'fixed',
      'flex',
      'fraction',
      'intrinsic',
      'min',
      'max',
    }.contains(family)) {
      throw error('Unknown width family');
    }
    expect('(');
    space();
    if (family == 'min' || family == 'max') {
      final a = width(depth + 1);
      expect(',');
      final b = width(depth + 1);
      expect(')');
      return family == 'min' ? MinColumnWidth(a, b) : MaxColumnWidth(a, b);
    }
    double? number;
    if (family != 'intrinsic' || done || text[position] != ')') {
      start = position;
      while (!done && '0123456789.eE+-'.contains(text[position])) {
        position++;
      }
      final token = text.substring(start, position);
      if (token.length > 80 ||
          !RegExp(
            r'^[+]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]{1,3})?$',
          ).hasMatch(token)) {
        throw error('Expected finite non-negative number');
      }
      final parts = token.toLowerCase().split('e');
      final mantissa = parts.first.replaceFirst('+', '');
      final digits = mantissa
          .replaceAll('.', '')
          .replaceFirst(RegExp(r'^0+'), '');
      final dot = mantissa.indexOf('.');
      final scale =
          (dot < 0 ? 0 : mantissa.length - dot - 1) -
          (parts.length == 1 ? 0 : int.parse(parts.last));
      if (digits.length > 64 || scale.abs() > 999) {
        throw error(
          'Width precision or scale exceeds the bounded numeric range',
        );
      }
      number = double.tryParse(token);
      if (number == null ||
          !number.isFinite ||
          number < 0 ||
          (family == 'flex' || family == 'intrinsic') && number <= 0) {
        throw error(
          'Widths must be finite and non-negative; flex must be positive',
        );
      }
    }
    expect(')');
    return switch (family) {
      'fixed' => FixedColumnWidth(number!),
      'flex' => FlexColumnWidth(number!),
      'fraction' => FractionColumnWidth(number!),
      'intrinsic' => IntrinsicColumnWidth(flex: number),
      _ => throw error('Unknown width family'),
    };
  }
}
