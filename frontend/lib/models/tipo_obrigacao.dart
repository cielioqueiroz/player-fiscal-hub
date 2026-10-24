const tiposObrigacao = ['DAS', 'DCTF', 'ECD'];

class TipoObrigacao {
  final String tipo;
  final String descricao;
  final String periodicidade;
  final List<String> regimesAceitos;

  const TipoObrigacao({
    required this.tipo,
    required this.descricao,
    required this.periodicidade,
    required this.regimesAceitos,
  });

  factory TipoObrigacao.fromJson(Map<String, dynamic> json) => TipoObrigacao(
        tipo: json['tipo'] as String,
        descricao: json['descricao'] as String,
        periodicidade: json['periodicidade'] as String,
        regimesAceitos: List<String>.from(json['regimesAceitos'] as List),
      );
}
