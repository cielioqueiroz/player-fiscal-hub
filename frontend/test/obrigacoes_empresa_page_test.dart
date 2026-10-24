import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/models/empresa.dart';
import 'package:frontend/pages/obrigacoes_empresa_page.dart';
import 'package:http/http.dart' as http;

import 'api_fake.dart';

const _padaria = Empresa(id: 1, nome: 'Padaria', cnpj: '11222333000181', regimeTributario: 'SIMPLES');

const _catalogo = [
  {
    'tipo': 'DAS',
    'descricao': 'Documento de Arrecadação do Simples Nacional',
    'periodicidade': 'MENSAL',
    'regimesAceitos': ['SIMPLES'],
  },
  {
    'tipo': 'DCTF',
    'descricao': 'Declaração de Débitos e Créditos Tributários Federais',
    'periodicidade': 'MENSAL',
    'regimesAceitos': ['LUCRO_PRESUMIDO', 'LUCRO_REAL'],
  },
  {
    'tipo': 'ECD',
    'descricao': 'Escrituração Contábil Digital',
    'periodicidade': 'ANUAL',
    'regimesAceitos': ['SIMPLES', 'LUCRO_PRESUMIDO', 'LUCRO_REAL'],
  },
];

const _dasAnexada = {
  'id': 9,
  'tipo': 'DAS',
  'descricao': 'Documento de Arrecadação do Simples Nacional',
  'criadoEm': '2026-06-01T10:00:00',
};

Future<List<http.Request>> abrir(
  WidgetTester tester, {
  Object? obrigacoes = const [_dasAnexada],
  Map<String, Object?> extras = const {},
}) async {
  usarTelaGrande(tester);
  final chamadas = <http.Request>[];
  final api = apiFake({
    'GET /catalogo-obrigacoes': _catalogo,
    'GET /empresas/1/obrigacoes': obrigacoes,
    ...extras,
  }, chamadas: chamadas);
  await tester.pumpWidget(MaterialApp(home: ObrigacoesEmpresaPage(api: api, empresa: _padaria)));
  await tester.pumpAndSettle();
  return chamadas;
}

Iterable<http.Request> _vinculos(List<http.Request> chamadas) =>
    chamadas.where((r) => r.method == 'GET' && r.url.path == '/empresas/1/obrigacoes');

void main() {
  testWidgets('anexadaMostraDataERemover', (tester) async {
    await abrir(tester);

    expect(find.textContaining('Anexada em 01/06/2026'), findsOneWidget);
    expect(find.text('Remover'), findsOneWidget);
  });

  testWidgets('incompativelFicaDesabilitada', (tester) async {
    await abrir(tester);

    expect(find.text('Não se aplica ao regime Simples Nacional'), findsOneWidget);
    expect(find.text('Anexar'), findsOneWidget);
  });

  testWidgets('anexarEnviaTipoERecarrega', (tester) async {
    Map<String, dynamic>? corpo;
    final chamadas = await abrir(tester, extras: {
      'POST /empresas/1/obrigacoes': (http.Request r) {
        corpo = jsonDecode(r.body) as Map<String, dynamic>;
        return jsonResp({'id': 10, 'tipo': 'ECD', 'descricao': 'Escrituração Contábil Digital', 'criadoEm': '2026-06-02T10:00:00'}, 201);
      },
    });

    await tester.tap(find.text('Anexar'));
    await tester.pumpAndSettle();

    expect(corpo, {'tipo': 'ECD'});
    expect(_vinculos(chamadas), hasLength(2));
  });

  testWidgets('erroAoAnexarVaiParaSnackBar', (tester) async {
    await abrir(tester, extras: {
      'POST /empresas/1/obrigacoes': jsonResp({'erro': 'A obrigação ECD não se aplica ao regime SIMPLES'}, 422),
    });

    await tester.tap(find.text('Anexar'));
    await tester.pumpAndSettle();

    expect(find.text('A obrigação ECD não se aplica ao regime SIMPLES'), findsOneWidget);
  });

  testWidgets('falhaAoAnexarRecarregaOsVinculos', (tester) async {
    const ecd = {
      'id': 10,
      'tipo': 'ECD',
      'descricao': 'Escrituração Contábil Digital',
      'criadoEm': '2026-06-02T10:00:00',
    };
    var leituras = 0;
    await abrir(
      tester,
      obrigacoes: (http.Request _) => ++leituras == 1 ? [_dasAnexada] : [_dasAnexada, ecd],
      extras: {'POST /empresas/1/obrigacoes': jsonResp({'erro': 'A obrigação ECD já está anexada'}, 409)},
    );
    expect(find.text('Anexar'), findsOneWidget);

    await tester.tap(find.text('Anexar'));
    await tester.pumpAndSettle();

    expect(find.text('A obrigação ECD já está anexada'), findsOneWidget);
    expect(find.text('Anexar'), findsNothing);
    expect(find.textContaining('Anexada em'), findsNWidgets(2));
  });

  testWidgets('removerPedeConfirmacaoEDepoisRecarrega', (tester) async {
    final chamadas = await abrir(tester, extras: {'DELETE /empresas/1/obrigacoes/DAS': http.Response('', 204)});

    await tester.tap(find.text('Remover'));
    await tester.pumpAndSettle();
    expect(chamadas.where((r) => r.method == 'DELETE'), isEmpty);

    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar'));
    await tester.pumpAndSettle();

    expect(chamadas.where((r) => r.method == 'DELETE'), hasLength(1));
    expect(_vinculos(chamadas), hasLength(2));
  });

  testWidgets('removerCanceladoNaoChamaApi', (tester) async {
    final chamadas = await abrir(tester);

    await tester.tap(find.text('Remover'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Voltar'));
    await tester.pumpAndSettle();

    expect(chamadas.where((r) => r.method == 'DELETE'), isEmpty);
  });

  testWidgets('atalhoAbreEntregasFiltradasPelaEmpresa', (tester) async {
    final chamadas = await abrir(tester, extras: {
      'GET /empresas': [],
      'GET /entregas': [],
    });

    await tester.tap(find.text('Ver entregas desta empresa'));
    await tester.pumpAndSettle();

    final listagem = chamadas.where((r) => r.method == 'GET' && r.url.path == '/entregas');
    expect(listagem.first.url.queryParameters['empresaId'], '1');
  });

  testWidgets('erroAoCarregarMostraTentarNovamente', (tester) async {
    await abrir(tester, extras: {
      'GET /catalogo-obrigacoes': jsonResp({'erro': 'Erro interno. Tente novamente.'}, 500),
    });

    expect(find.text('Erro interno. Tente novamente.'), findsOneWidget);
    expect(find.text('Tentar novamente'), findsOneWidget);
  });
}
