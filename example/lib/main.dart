import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: true),
      themeMode: ThemeMode.dark,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Tentron'),
        ),
        body: const Center(
          child: Text("Tentron"),
        ),
      ),
    );
  }
}
