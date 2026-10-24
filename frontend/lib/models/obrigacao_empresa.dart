class ObrigacaoEmpresa {
  final int id;
  final String tipo;
  final String descricao;
  final String criadoEm;

  const ObrigacaoEmpresa({
    required this.id,
    required this.tipo,
    required this.descricao,
    required this.criadoEm,
  });

  factory ObrigacaoEmpresa.fromJson(Map<String, dynamic> json) => ObrigacaoEmpresa(
        id: json['id'] as int,
        tipo: json['tipo'] as String,
        descricao: json['descricao'] as String,
        criadoEm: json['criadoEm'] as String,
      );
}
