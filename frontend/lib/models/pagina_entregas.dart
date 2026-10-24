import 'entrega.dart';

class PaginaEntregas {
  final List<Entrega> conteudo;
  final int pagina;
  final int tamanho;
  final int totalElementos;
  final int totalPaginas;

  const PaginaEntregas({
    required this.conteudo,
    required this.pagina,
    required this.tamanho,
    required this.totalElementos,
    required this.totalPaginas,
  });

  factory PaginaEntregas.fromJson(Map<String, dynamic> json) => PaginaEntregas(
    conteudo: (json['conteudo'] as List)
        .map((item) => Entrega.fromJson(item as Map<String, dynamic>))
        .toList(),
    pagina: json['pagina'] as int,
    tamanho: json['tamanho'] as int,
    totalElementos: json['totalElementos'] as int,
    totalPaginas: json['totalPaginas'] as int,
  );
}
