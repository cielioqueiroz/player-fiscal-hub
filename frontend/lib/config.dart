import 'package:flutter/foundation.dart';

String resolverApiBaseUrl({required bool android, String? apiUrl}) {
  final configurada = apiUrl ?? const String.fromEnvironment('API_URL');
  if (configurada.isNotEmpty) return configurada;
  return android ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
}

String get apiBaseUrl {
  return resolverApiBaseUrl(
    android: !kIsWeb && defaultTargetPlatform == TargetPlatform.android,
  );
}
