class ResumoGeracao {
  final String mesReferencia;
  final int criadas;
  final int jaExistentes;

  const ResumoGeracao({
    required this.mesReferencia,
    required this.criadas,
    required this.jaExistentes,
  });

  factory ResumoGeracao.fromJson(Map<String, dynamic> json) => ResumoGeracao(
        mesReferencia: json['mesReferencia'] as String,
        criadas: json['criadas'] as int,
        jaExistentes: json['jaExistentes'] as int,
      );
}
