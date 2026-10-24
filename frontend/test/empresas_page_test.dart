import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/pages/empresas_page.dart';
import 'package:http/http.dart' as http;

import 'api_fake.dart';

const _padaria = {'id': 1, 'nome': 'Padaria', 'cnpj': '11222333000181', 'regimeTributario': 'SIMPLES'};

Future<List<http.Request>> abrir(
  WidgetTester tester, {
  Object? empresas = const [_padaria],
  Map<String, Object?> extras = const {},
}) async {
  usarTelaGrande(tester);
  final chamadas = <http.Request>[];
  final api = apiFake({'GET /empresas': empresas, ...extras}, chamadas: chamadas);
  await tester.pumpWidget(MaterialApp(home: EmpresasPage(api: api)));
  await tester.pumpAndSettle();
  return chamadas;
}

Future<void> abrirDialogo(WidgetTester tester) async {
  await tester.tap(find.text('Nova empresa'));
  await tester.pumpAndSettle();
}

Iterable<http.Request> _listagens(List<http.Request> chamadas) =>
    chamadas.where((r) => r.method == 'GET' && r.url.path == '/empresas');

void main() {
  testWidgets('listaComCnpjFormatadoERegime', (tester) async {
    await abrir(tester);

    expect(find.text('Padaria'), findsOneWidget);
    expect(find.text('11.222.333/0001-81'), findsOneWidget);
    expect(find.text('Simples Nacional'), findsOneWidget);
  });

  testWidgets('listaVazia', (tester) async {
    await abrir(tester, empresas: []);

    expect(find.text('Nenhuma empresa cadastrada. Use "Nova empresa".'), findsOneWidget);
  });

  testWidgets('erroAoCarregarMostraTentarNovamente', (tester) async {
    final chamadas = await abrir(tester, empresas: jsonResp({'erro': 'Erro interno. Tente novamente.'}, 500));
    expect(find.text('Erro interno. Tente novamente.'), findsOneWidget);

    await tester.tap(find.text('Tentar novamente'));
    await tester.pumpAndSettle();

    expect(_listagens(chamadas), hasLength(2));
  });

  testWidgets('camposObrigatorios', (tester) async {
    final chamadas = await abrir(tester);
    await abrirDialogo(tester);

    await tester.tap(find.widgetWithText(FilledButton, 'Salvar'));
    await tester.pumpAndSettle();

    expect(find.text('Informe o nome'), findsOneWidget);
    expect(find.text('Informe o CNPJ'), findsOneWidget);
    expect(chamadas.where((r) => r.method == 'POST'), isEmpty);
  });

  testWidgets('cliqueForaDoDialogoNaoFechaEPreservaOTexto', (tester) async {
    await abrir(tester);
    await abrirDialogo(tester);
    await tester.enterText(find.byKey(const Key('campoNome')), 'Mercado');

    await tester.tapAt(const Offset(5, 5));
    await tester.pumpAndSettle();

    expect(find.byType(AlertDialog), findsOneWidget);
    expect(find.text('Mercado'), findsOneWidget);
  });

  testWidgets('erroDoBackendFicaNoDialogo', (tester) async {
    await abrir(tester, extras: {'POST /empresas': jsonResp({'erro': 'CNPJ inválido'}, 400)});
    await abrirDialogo(tester);

    await tester.enterText(find.byKey(const Key('campoNome')), 'Mercado');
    await tester.enterText(find.byKey(const Key('campoCnpj')), '123');
    await tester.tap(find.widgetWithText(FilledButton, 'Salvar'));
    await tester.pumpAndSettle();

    expect(find.text('CNPJ inválido'), findsOneWidget);
    expect(find.byType(AlertDialog), findsOneWidget);
    expect(find.text('Mercado'), findsOneWidget);
    expect(find.text('123'), findsOneWidget);
  });

  testWidgets('sucessoFechaERecarrega', (tester) async {
    Map<String, dynamic>? corpo;
    final chamadas = await abrir(tester, extras: {
      'POST /empresas': (http.Request r) {
        corpo = jsonDecode(r.body) as Map<String, dynamic>;
        return jsonResp({'id': 2, 'nome': 'Mercado', 'cnpj': '12345678000195', 'regimeTributario': 'LUCRO_REAL'}, 201);
      },
    });
    await abrirDialogo(tester);

    await tester.enterText(find.byKey(const Key('campoNome')), 'Mercado');
    await tester.enterText(find.byKey(const Key('campoCnpj')), '12.345.678/0001-95');
    await tester.tap(find.byKey(const Key('campoRegime')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lucro Real').last);
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Salvar'));
    await tester.pumpAndSettle();

    expect(corpo, {'nome': 'Mercado', 'cnpj': '12.345.678/0001-95', 'regimeTributario': 'LUCRO_REAL'});
    expect(find.byType(AlertDialog), findsNothing);
    expect(_listagens(chamadas), hasLength(2));
  });

  testWidgets('tocarNaEmpresaAbreAsObrigacoes', (tester) async {
    final chamadas = await abrir(tester, extras: {
      'GET /catalogo-obrigacoes': [],
      'GET /empresas/1/obrigacoes': [],
    });

    await tester.tap(find.text('Padaria'));
    await tester.pumpAndSettle();

    expect(chamadas.any((r) => r.url.path == '/empresas/1/obrigacoes'), isTrue);
    expect(find.text('Ver entregas desta empresa'), findsOneWidget);
  });

  testWidgets('editarReutilizaFormularioPreenchidoEEnviaPut', (tester) async {
    Map<String, dynamic>? corpo;
    http.Request? requisicao;
    final chamadas = await abrir(tester, extras: {
      'PUT /empresas/1': (http.Request r) {
        requisicao = r;
        corpo = jsonDecode(r.body) as Map<String, dynamic>;
        return jsonResp({..._padaria, 'nome': 'Padaria Editada'});
      },
    });

    await tester.tap(find.text('Editar'));
    await tester.pumpAndSettle();

    expect(find.text('Editar empresa'), findsOneWidget);
    expect(tester.widget<TextFormField>(find.byKey(const Key('campoNome'))).controller!.text, 'Padaria');
    expect(tester.widget<TextFormField>(find.byKey(const Key('campoCnpj'))).controller!.text, '11222333000181');
    await tester.enterText(find.byKey(const Key('campoNome')), 'Padaria Editada');
    await tester.tap(find.widgetWithText(FilledButton, 'Salvar'));
    await tester.pumpAndSettle();

    expect(requisicao?.method, 'PUT');
    expect(corpo, {'nome': 'Padaria Editada', 'cnpj': '11222333000181', 'regimeTributario': 'SIMPLES'});
    expect(_listagens(chamadas), hasLength(2));
  });

  testWidgets('inativacaoExigeConfirmacaoEChamaPatch', (tester) async {
    http.Request? requisicao;
    await abrir(tester, extras: {
      'PATCH /empresas/1/ativa': (http.Request r) {
        requisicao = r;
        return jsonResp({..._padaria, 'ativa': false});
      },
    });

    await tester.tap(find.text('Inativar'));
    await tester.pumpAndSettle();
    expect(find.text('Inativar empresa?'), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, 'Inativar'));
    await tester.pumpAndSettle();

    expect(requisicao?.method, 'PATCH');
    expect(jsonDecode(requisicao!.body), {'ativa': false});
    expect(find.text('Inativar empresa?'), findsNothing);
  });

  testWidgets('erroDoBackendAoEditarPermaneceNoFormulario', (tester) async {
    await abrir(tester, extras: {
      'PUT /empresas/1': jsonResp({'erro': 'Remova as obrigações incompatíveis antes de alterar o regime: DAS'}, 422),
    });

    await tester.tap(find.text('Editar'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('campoRegime')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lucro Real').last);
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Salvar'));
    await tester.pumpAndSettle();

    expect(find.text('Remova as obrigações incompatíveis antes de alterar o regime: DAS'), findsOneWidget);
    expect(find.byType(AlertDialog), findsOneWidget);
  });

  testWidgets('filtroSolicitaInativasEIndicaEmpresaInativa', (tester) async {
    final chamadas = await abrir(tester, empresas: [
      {..._padaria, 'ativa': false},
    ]);
    expect(find.text('11.222.333/0001-81 • Inativa'), findsOneWidget);

    await tester.tap(find.byKey(const Key('mostrarInativas')));
    await tester.pumpAndSettle();

    expect(chamadas.any((r) => r.url.queryParameters['incluirInativas'] == 'true'), isTrue);
  });

  testWidgets('reativacaoExigeConfirmacaoEChamaPatch', (tester) async {
    http.Request? requisicao;
    await abrir(tester, empresas: [
      {..._padaria, 'ativa': false},
    ], extras: {
      'PATCH /empresas/1/ativa': (http.Request r) {
        requisicao = r;
        return jsonResp({..._padaria, 'ativa': true});
      },
    });

    await tester.tap(find.byKey(const Key('mostrarInativas')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Reativar'));
    await tester.pumpAndSettle();
    expect(find.text('Reativar empresa?'), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, 'Reativar'));
    await tester.pumpAndSettle();

    expect(requisicao?.method, 'PATCH');
    expect(jsonDecode(requisicao!.body), {'ativa': true});
  });
}
