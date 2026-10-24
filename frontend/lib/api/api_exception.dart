class ApiException implements Exception {
  final int status;
  final String mensagem;
  ApiException(this.status, this.mensagem);

  @override
  String toString() => mensagem;
}
