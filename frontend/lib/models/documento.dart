class Documento {
  final int id;
  final String nomeArquivo;
  final String contentType;
  final int tamanhoBytes;
  final String enviadoEm;

  const Documento({
    required this.id,
    required this.nomeArquivo,
    required this.contentType,
    required this.tamanhoBytes,
    required this.enviadoEm,
  });

  factory Documento.fromJson(Map<String, dynamic> json) => Documento(
        id: json['id'] as int,
        nomeArquivo: json['nomeArquivo'] as String,
        contentType: json['contentType'] as String,
        tamanhoBytes: json['tamanhoBytes'] as int,
        enviadoEm: json['enviadoEm'] as String,
      );
}
