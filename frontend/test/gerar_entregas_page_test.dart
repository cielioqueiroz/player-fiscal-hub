import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/pages/gerar_entregas_page.dart';
import 'package:frontend/widgets/formatadores.dart';
import 'package:http/http.dart' as http;

import 'api_fake.dart';

Future<List<http.Request>> abrir(WidgetTester tester, Map<String, Object?> rotas) async {
  usarTelaGrande(tester);
  final chamadas = <http.Request>[];
  await tester.pumpWidget(MaterialApp(home: GerarEntregasPage(api: apiFake(rotas, chamadas: chamadas))));
  await tester.pumpAndSettle();
  return chamadas;
}

Map<String, dynamic> _resumo(int criadas, int jaExistentes) =>
    {'mesReferencia': '2026-09', 'criadas': criadas, 'jaExistentes': jaExistentes};

Iterable<http.Request> _envios(List<http.Request> chamadas) =>
    chamadas.where((r) => r.method == 'POST' && r.url.path == '/entregas/gerar');

Future<void> _gerar(WidgetTester tester) async {
  await tester.tap(find.text('Gerar'));
  await tester.pumpAndSettle();
}

void main() {
  final agora = DateTime.now();

  testWidgets('textoUsaMesAtual', (tester) async {
    await abrir(tester, {});

    expect(find.text(descricaoCompetencias(agora.year, agora.month)), findsOneWidget);
  });

  testWidgets('gerarMostraResumo', (tester) async {
    Map<String, dynamic>? corpo;
    await abrir(tester, {
      'POST /entregas/gerar': (http.Request r) {
        corpo = jsonDecode(r.body) as Map<String, dynamic>;
        return _resumo(6, 0);
      },
    });

    await _gerar(tester);

    expect(corpo, {'mesReferencia': mesReferencia(agora.year, agora.month)});
    expect(find.text('6 entregas criadas · 0 já existiam'), findsOneWidget);
  });

  testWidgets('nadaNovoParaGerar', (tester) async {
    await abrir(tester, {'POST /entregas/gerar': _resumo(0, 6)});

    await _gerar(tester);

    expect(find.text('0 entregas criadas · 6 já existiam'), findsOneWidget);
    expect(find.text('Nada novo para gerar: todas as entregas deste período já existem.'), findsOneWidget);
  });

  testWidgets('singularParaUmaEntrega', (tester) async {
    await abrir(tester, {'POST /entregas/gerar': _resumo(1, 1)});

    await _gerar(tester);

    expect(find.text('1 entrega criada · 1 já existia'), findsOneWidget);
  });

  testWidgets('erroVaiParaSnackBar', (tester) async {
    await abrir(tester, {
      'POST /entregas/gerar': jsonResp({'erro': 'mesReferencia deve ter ano entre 2000 e 2999'}, 400),
    });

    await _gerar(tester);

    expect(find.text('mesReferencia deve ter ano entre 2000 e 2999'), findsOneWidget);
  });

  testWidgets('trocarOMesLimpaOResultadoDoMesAnterior', (tester) async {
    await abrir(tester, {'POST /entregas/gerar': _resumo(6, 0)});
    await _gerar(tester);
    expect(find.text('6 entregas criadas · 0 já existiam'), findsOneWidget);

    await tester.tap(find.byKey(const Key('selectMes')));
    await tester.pumpAndSettle();
    await tester.tap(find.text(agora.month == 1 ? 'Fevereiro' : 'Janeiro').last);
    await tester.pumpAndSettle();

    expect(find.textContaining('entregas criadas'), findsNothing);
  });

  testWidgets('trocarOMesAtualizaOTextoEOEnvio', (tester) async {
    Map<String, dynamic>? corpo;
    await abrir(tester, {
      'POST /entregas/gerar': (http.Request r) {
        corpo = jsonDecode(r.body) as Map<String, dynamic>;
        return _resumo(3, 0);
      },
    });

    await tester.tap(find.byKey(const Key('selectMes')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Janeiro').last);
    await tester.pumpAndSettle();
    expect(find.text(descricaoCompetencias(agora.year, 1)), findsOneWidget);

    await _gerar(tester);

    expect(corpo, {'mesReferencia': mesReferencia(agora.year, 1)});
  });

  testWidgets('botaoFicaDesabilitadoDuranteARequisicao', (tester) async {
    final resposta = Completer<Map<String, dynamic>>();
    final chamadas = await abrir(tester, {'POST /entregas/gerar': resposta.future});

    await tester.tap(find.text('Gerar'));
    await tester.pump();
    expect(tester.widget<FilledButton>(find.byType(FilledButton)).onPressed, isNull);
    await tester.tap(find.text('Gerar'), warnIfMissed: false);
    await tester.pump();

    resposta.complete(_resumo(6, 0));
    await tester.pumpAndSettle();

    expect(_envios(chamadas), hasLength(1));
    expect(tester.widget<FilledButton>(find.byType(FilledButton)).onPressed, isNotNull);
  });
}
