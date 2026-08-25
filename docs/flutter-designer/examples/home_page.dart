// <netbeans-flutter-designer region="imports">
import 'package:flutter/material.dart';
// </netbeans-flutter-designer>

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  // <netbeans-flutter-designer region="build">
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Hello from NetBeans'),
      ),
      body: Center(
        child: ElevatedButton(
          onPressed: onContinue,
          child: const Text('Continue'),
        ),
      ),
    );
  }
  // </netbeans-flutter-designer>

  void onContinue() {
    // This method is user-owned and is preserved by designer saves.
  }
}
