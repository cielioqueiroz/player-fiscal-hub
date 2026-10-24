class ResumoEntregas {
  final int total;
  final int abertas;
  final int atrasadas;
  final int concluidas;
  final int canceladas;
  final int venceEmAte7Dias;
  final int vencidasSemBaixa;

  const ResumoEntregas({
    required this.total,
    required this.abertas,
    required this.atrasadas,
    required this.concluidas,
    required this.canceladas,
    required this.venceEmAte7Dias,
    required this.vencidasSemBaixa,
  });

  factory ResumoEntregas.fromJson(Map<String, dynamic> json) => ResumoEntregas(
    total: json['total'] as int,
    abertas: json['abertas'] as int,
    atrasadas: json['atrasadas'] as int,
    concluidas: json['concluidas'] as int,
    canceladas: json['canceladas'] as int,
    venceEmAte7Dias: json['venceEmAte7Dias'] as int,
    vencidasSemBaixa: json['vencidasSemBaixa'] as int,
  );
}
