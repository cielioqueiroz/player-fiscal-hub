import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/pages/entregas_page.dart';
import 'package:http/http.dart' as http;

import 'api_fake.dart';

const _empresas = [
  {'id': 1, 'nome': 'Padaria', 'cnpj': '11222333000181', 'regimeTributario': 'SIMPLES'},
  {'id': 3, 'nome': 'Mercado', 'cnpj': '12345678000195', 'regimeTributario': 'LUCRO_REAL'},
];

Future<List<http.Request>> abrir(
  WidgetTester tester, {
  Object? entregas = const [],
  int? empresaIdInicial,
  Map<String, Object?> extras = const {},
}) async {
  usarTelaGrande(tester);
  final chamadas = <http.Request>[];
  final api = apiFake({
    'GET /empresas': _empresas,
    'GET /entregas': entregas,
    ...extras,
  }, chamadas: chamadas);
  await tester.pumpWidget(MaterialApp(home: EntregasPage(api: api, empresaIdInicial: empresaIdInicial)));
  await tester.pumpAndSettle();
  return chamadas;
}

Iterable<http.Request> _listagens(List<http.Request> chamadas) =>
    chamadas.where((r) => r.method == 'GET' && r.url.path == '/entregas');

Finder _naTabela(String texto) => find.descendant(of: find.byType(DataTable), matching: find.text(texto));

void main() {
  testWidgets('vencidaFicaDestacada', (tester) async {
    await abrir(tester, entregas: [
      entregaJson({'id': 1, 'vencida': true}),
      entregaJson({'id': 2, 'tipo': 'ECD', 'competencia': '2025', 'vencida': false}),
    ]);

    expect(find.text('Vencida'), findsOneWidget);
    expect(find.byIcon(Icons.warning_amber), findsOneWidget);
  });

  testWidgets('listaVaziaSemFiltro', (tester) async {
    await abrir(tester);

    expect(find.text("Nenhuma entrega. Use 'Gerar entregas'."), findsOneWidget);
  });

  testWidgets('filtroInicialDaEmpresa', (tester) async {
    final chamadas = await abrir(tester, empresaIdInicial: 3);

    expect(_listagens(chamadas).first.url.queryParameters['empresaId'], '3');
    expect(find.text('Nenhuma entrega encontrada com esses filtros.'), findsOneWidget);
  });

  testWidgets('erroMostraTentarNovamente', (tester) async {
    final chamadas = await abrir(tester, extras: {
      'GET /entregas': jsonResp({'erro': 'Erro interno. Tente novamente.'}, 500),
    });
    expect(find.text('Erro interno. Tente novamente.'), findsOneWidget);

    await tester.tap(find.text('Tentar novamente'));
    await tester.pumpAndSettle();

    expect(_listagens(chamadas), hasLength(2));
  });

  testWidgets('linhaMostraDadosFormatados', (tester) async {
    await abrir(tester, entregas: [entregaJson()]);

    expect(_naTabela('Padaria'), findsOneWidget);
    expect(_naTabela('DAS'), findsOneWidget);
    expect(_naTabela('2026-05'), findsOneWidget);
    expect(_naTabela('20/06/2026'), findsOneWidget);
    expect(_naTabela('ABERTA'), findsOneWidget);
  });

  testWidgets('selecoesAguardamFiltrarEAplicamTodosOsCamposJuntos', (tester) async {
    final chamadas = await abrir(tester, extras: {
      'GET /entregas': (http.Request req) => [
        req.url.queryParameters.isEmpty
            ? entregaJson()
            : entregaJson({'empresaId': 3, 'empresaNome': 'Mercado', 'tipo': 'DCTF', 'situacao': 'ATRASADA'}),
      ],
    });

    await tester.tap(find.byKey(const Key('filtroEmpresa')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Mercado').last);
    await tester.pumpAndSettle();
    expect(_listagens(chamadas), hasLength(1));
    expect(_naTabela('Padaria'), findsOneWidget);

    await tester.tap(find.byKey(const Key('filtroTipo')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('DCTF').last);
    await tester.pumpAndSettle();
    expect(_listagens(chamadas), hasLength(1));

    await tester.tap(find.byKey(const Key('filtroSituacao')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('ATRASADA').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '2026-05');

    expect(_listagens(chamadas), hasLength(1));
    expect(_naTabela('Padaria'), findsOneWidget);
    await tester.tap(find.text('Filtrar'));
    await tester.pumpAndSettle();

    expect(_listagens(chamadas), hasLength(2));
    expect(_listagens(chamadas).last.url.queryParameters, {
      'empresaId': '3',
      'tipo': 'DCTF',
      'situacao': 'ATRASADA',
      'competencia': '2026-05',
    });
    expect(_naTabela('Mercado'), findsOneWidget);
    expect(_naTabela('Padaria'), findsNothing);
  });

  testWidgets('selecaoNaoConfirmadaMantemMensagemDaListaSemFiltro', (tester) async {
    final chamadas = await abrir(tester);

    await tester.tap(find.byKey(const Key('filtroEmpresa')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Mercado').last);
    await tester.pumpAndSettle();

    expect(find.text("Nenhuma entrega. Use 'Gerar entregas'."), findsOneWidget);
    expect(_listagens(chamadas), hasLength(1));
  });

  testWidgets('botaoFiltrarAplicaACompetencia', (tester) async {
    final chamadas = await abrir(tester);

    await tester.enterText(find.byType(TextField), '2025');
    await tester.tap(find.text('Filtrar'));
    await tester.pumpAndSettle();

    expect(_listagens(chamadas).last.url.queryParameters, {'competencia': '2025'});
  });

  testWidgets('limparZeraOsFiltros', (tester) async {
    final chamadas = await abrir(tester, empresaIdInicial: 3);

    await tester.enterText(find.byType(TextField), '2026-05');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();
    expect(_listagens(chamadas).last.url.queryParameters, {'empresaId': '3', 'competencia': '2026-05'});

    await tester.tap(find.text('Limpar'));
    await tester.pumpAndSettle();

    expect(_listagens(chamadas).last.url.queryParameters, isEmpty);
    expect(find.text("Nenhuma entrega. Use 'Gerar entregas'."), findsOneWidget);
  });

  testWidgets('respostaAntigaNaoSobrescreveAMaisRecente', (tester) async {
    usarTelaGrande(tester);
    final lenta = Completer<List<Map<String, dynamic>>>();
    var leituras = 0;
    final api = apiFake({
      'GET /empresas': _empresas,
      'GET /entregas': (http.Request _) =>
          ++leituras == 1 ? lenta.future : [entregaJson({'empresaNome': 'Mercado', 'situacao': 'ATRASADA'})],
    });
    await tester.pumpWidget(MaterialApp(home: EntregasPage(api: api)));
    await tester.pump(const Duration(seconds: 1));

    await tester.tap(find.byKey(const Key('filtroSituacao')));
    await tester.pump(const Duration(seconds: 1));
    await tester.tap(find.text('ATRASADA').last);
    await tester.pump(const Duration(seconds: 1));
    await tester.tap(find.text('Filtrar'));
    await tester.pump(const Duration(seconds: 1));
    expect(_naTabela('Mercado'), findsOneWidget);

    lenta.complete([entregaJson({'empresaNome': 'Padaria'})]);
    await tester.pump(const Duration(seconds: 1));

    expect(_naTabela('Mercado'), findsOneWidget);
    expect(_naTabela('Padaria'), findsNothing);
  });

  testWidgets('voltarDoDetalheMantemOsFiltrosAplicados', (tester) async {
    final chamadas = await abrir(tester, empresaIdInicial: 1, entregas: [entregaJson()], extras: {
      'GET /entregas/1': entregaJson(),
      'GET /entregas/1/documentos': [],
    });

    await tester.enterText(find.byType(TextField), '2026-09');
    await tester.tap(_naTabela('Padaria'));
    await tester.pumpAndSettle();
    expect(find.text('Entrega #1'), findsOneWidget);

    await tester.pageBack();
    await tester.pumpAndSettle();

    expect(_listagens(chamadas), hasLength(2));
    expect(_listagens(chamadas).last.url.queryParameters, {'empresaId': '1'});
    expect(find.text('2026-09'), findsOneWidget);
  });
}
