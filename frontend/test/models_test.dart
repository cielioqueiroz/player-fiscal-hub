import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/models/documento.dart';
import 'package:frontend/models/entrega.dart';
import 'package:frontend/models/resumo_geracao.dart';
import 'package:frontend/models/pagina_entregas.dart';
import 'package:frontend/models/tipo_obrigacao.dart';

final entregaJson = <String, dynamic>{
  'id': 7,
  'empresaId': 1,
  'empresaNome': 'Padaria',
  'empresaCnpj': '11222333000181',
  'tipo': 'DAS',
  'competencia': '2026-05',
  'dataVencimento': '2026-06-20',
  'situacao': 'ABERTA',
  'dataConclusao': null,
  'motivoCancelamento': null,
  'criadoEm': '2026-06-01T10:00:00',
  'vencida': true,
  'transicoesPermitidas': ['ATRASADA', 'CONCLUIDA', 'CANCELADA'],
};

void main() {
  test('Entrega lê vencida e transicoesPermitidas', () {
    final e = Entrega.fromJson(entregaJson);
    expect(e.vencida, isTrue);
    expect(e.transicoesPermitidas, ['ATRASADA', 'CONCLUIDA', 'CANCELADA']);
    expect(e.dataConclusao, isNull);
  });

  test('isFinal só para CONCLUIDA e CANCELADA', () {
    bool finalPara(String s) => Entrega.fromJson({...entregaJson, 'situacao': s}).isFinal;
    expect(finalPara('CONCLUIDA'), isTrue);
    expect(finalPara('CANCELADA'), isTrue);
    expect(finalPara('ABERTA'), isFalse);
    expect(finalPara('ATRASADA'), isFalse);
  });

  test('TipoObrigacao lê regimesAceitos', () {
    final t = TipoObrigacao.fromJson({
      'tipo': 'DCTF',
      'descricao': 'Declaração',
      'periodicidade': 'MENSAL',
      'regimesAceitos': ['LUCRO_PRESUMIDO', 'LUCRO_REAL'],
    });
    expect(t.regimesAceitos, contains('LUCRO_REAL'));
    expect(t.regimesAceitos, isNot(contains('SIMPLES')));
  });

  test('Documento lê tamanhoBytes como int', () {
    final d = Documento.fromJson({
      'id': 1,
      'nomeArquivo': 'guia.pdf',
      'contentType': 'application/pdf',
      'tamanhoBytes': 250880,
      'enviadoEm': '2026-06-18T10:00:00',
    });
    expect(d.tamanhoBytes, 250880);
  });

  test('ResumoGeracao', () {
    final r = ResumoGeracao.fromJson({'mesReferencia': '2026-09', 'criadas': 6, 'jaExistentes': 0});
    expect([r.criadas, r.jaExistentes], [6, 0]);
  });

  test('PaginaEntregas lê dados da paginação', () {
    final pagina = PaginaEntregas.fromJson({
      'conteudo': [entregaJson],
      'pagina': 1,
      'tamanho': 20,
      'totalElementos': 21,
      'totalPaginas': 2,
    });
    expect(pagina.conteudo.single.id, 7);
    expect(pagina.pagina, 1);
    expect(pagina.totalElementos, 21);
    expect(pagina.totalPaginas, 2);
  });
}
