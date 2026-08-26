import 'package:flutter/material.dart';

void main() {
  runApp(const NativeCanvasApp());
}

class NativeCanvasApp extends StatelessWidget {
  const NativeCanvasApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
      ),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Native Flutter Canvas'),
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        ),
        body: const Center(
          child: Padding(
            padding: EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                FlutterLogo(size: 72),
                SizedBox(height: 20),
                Text(
                  'FlutterView is embedded in the NetBeans Design view',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
                ),
                SizedBox(height: 12),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.phone_android),
                    SizedBox(width: 12),
                    Icon(Icons.tablet),
                    SizedBox(width: 12),
                    Icon(Icons.desktop_windows),
                    SizedBox(width: 12),
                    Icon(Icons.web),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
