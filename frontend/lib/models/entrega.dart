const situacoesEntrega = ['ABERTA', 'ATRASADA', 'CONCLUIDA', 'CANCELADA'];

class Entrega {
  final int id;
  final int empresaId;
  final String empresaNome;
  final String empresaCnpj;
  final String tipo;
  final String competencia;
  final String dataVencimento;
  final String situacao;
  final String? dataConclusao;
  final String? motivoCancelamento;
  final String criadoEm;
  final bool vencida;
  final List<String> transicoesPermitidas;

  const Entrega({
    required this.id,
    required this.empresaId,
    required this.empresaNome,
    required this.empresaCnpj,
    required this.tipo,
    required this.competencia,
    required this.dataVencimento,
    required this.situacao,
    required this.dataConclusao,
    required this.motivoCancelamento,
    required this.criadoEm,
    required this.vencida,
    required this.transicoesPermitidas,
  });

  factory Entrega.fromJson(Map<String, dynamic> json) => Entrega(
        id: json['id'] as int,
        empresaId: json['empresaId'] as int,
        empresaNome: json['empresaNome'] as String,
        empresaCnpj: json['empresaCnpj'] as String,
        tipo: json['tipo'] as String,
        competencia: json['competencia'] as String,
        dataVencimento: json['dataVencimento'] as String,
        situacao: json['situacao'] as String,
        dataConclusao: json['dataConclusao'] as String?,
        motivoCancelamento: json['motivoCancelamento'] as String?,
        criadoEm: json['criadoEm'] as String,
        vencida: json['vencida'] as bool,
        transicoesPermitidas: List<String>.from(json['transicoesPermitidas'] as List),
      );

  bool get isFinal => situacao == 'CONCLUIDA' || situacao == 'CANCELADA';
}
