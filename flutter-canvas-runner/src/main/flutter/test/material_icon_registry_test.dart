import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/material_icon_registry.dart';

void main() {
  test('compact admission exactly matches the locked Flutter registry', () {
    final expected = <(int, bool)>{};
    var sourceRows = 0;
    for (final line in _lockedRegistry().readAsLinesSync()) {
      if (line.startsWith('#')) {
        continue;
      }
      sourceRows++;
      final columns = line.split('\t');
      expect(columns, hasLength(3), reason: line);
      expected.add((
        int.parse(columns[1], radix: 16),
        switch (columns[2]) {
          '0' => false,
          '1' => true,
          _ => throw StateError('Invalid direction flag in locked TSV: $line'),
        },
      ));
    }

    expect(sourceRows, 8825);
    expect(expected, hasLength(8626));

    final actual = <(int, bool)>{};
    for (var codePoint = 0; codePoint <= 0x10ffff; codePoint++) {
      if (isReviewedMaterialIconDataPair(codePoint, false)) {
        actual.add((codePoint, false));
      }
      if (isReviewedMaterialIconDataPair(codePoint, true)) {
        actual.add((codePoint, true));
      }
    }

    expect(actual.difference(expected), isEmpty, reason: 'unexpected pairs');
    expect(expected.difference(actual), isEmpty, reason: 'missing pairs');
  });

  test('rejects unknown scalars and the wrong direction metadata', () {
    expect(isReviewedMaterialIconDataPair(0xe29f, false), isFalse);
    expect(isReviewedMaterialIconDataPair(0xe5f9, true), isFalse);
    expect(isReviewedMaterialIconDataPair(0xe5fc, false), isFalse);

    expect(isReviewedMaterialIconDataPair(0xe5f9, false), isTrue);
    expect(isReviewedMaterialIconDataPair(0xe5fc, true), isTrue);
    expect(isReviewedMaterialIconDataPair(0xe67e, false), isTrue);
    expect(isReviewedMaterialIconDataPair(0xe67e, true), isTrue);
  });
}

File _lockedRegistry() {
  var directory = Directory.current.absolute;
  while (true) {
    final candidate = File(
      '${directory.path}${Platform.pathSeparator}'
      'flutter-designer${Platform.pathSeparator}src${Platform.pathSeparator}'
      'main${Platform.pathSeparator}resources${Platform.pathSeparator}dev'
      '${Platform.pathSeparator}flutter${Platform.pathSeparator}netbeans'
      '${Platform.pathSeparator}designer${Platform.pathSeparator}catalog'
      '${Platform.pathSeparator}material-icons-3.44.8.tsv',
    );
    if (candidate.existsSync()) {
      return candidate;
    }
    final parent = directory.parent;
    if (parent.path == directory.path) {
      throw StateError('Cannot locate the locked Material Icons TSV.');
    }
    directory = parent;
  }
}
