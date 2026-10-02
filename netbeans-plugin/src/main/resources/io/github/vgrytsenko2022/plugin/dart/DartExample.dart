import 'package:flutter/material.dart';

/// A small preview for the NetBeans Fonts & Colors page.
@immutable
class CounterCard extends StatelessWidget {
  const CounterCard({super.key, required this.count});

  final int count;

  @override
  Widget build(BuildContext context) {
    final String label = 'Count: $count';
    return Text(label);
  }
}
