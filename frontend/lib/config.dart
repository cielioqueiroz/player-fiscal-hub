String get apiBaseUrl {
  const configurada = String.fromEnvironment('API_URL');
  if (configurada.isNotEmpty) return configurada;
  return 'http://localhost:8080';
}
