import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';
import 'package:frontend/pages/entregas_page.dart';

import 'api_fake.dart';

void main() {
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('usa a API local no desenvolvimento', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.windows;
    expect(apiFake({}).baseUrl, 'http://localhost:8080');
  });

  testWidgets('navega entre as telas em uma janela estreita', (tester) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final api = apiFake({'GET /empresas': [], 'GET /entregas': []});

    await tester.pumpWidget(MeuApp(api: api));
    await tester.pumpAndSettle();

    expect(find.byType(NavigationBar), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.tap(find.text('Entregas').last);
    await tester.pumpAndSettle();
    expect(find.text("Nenhuma entrega. Use 'Gerar entregas'."), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.tap(find.text('Gerar entregas').last);
    await tester.pumpAndSettle();
    expect(find.text('Gerar'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('tabela permite consultar a situacao em uma janela estreita', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final api = apiFake({
      'GET /empresas': [
        {
          'id': 1,
          'nome': 'Padaria',
          'cnpj': '11222333000181',
          'regimeTributario': 'SIMPLES',
        },
      ],
      'GET /entregas': [entregaJson()],
    });

    await tester.pumpWidget(MaterialApp(home: EntregasPage(api: api)));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await tester.drag(find.text('Padaria'), const Offset(-700, 0));
    await tester.pumpAndSettle();
    expect(find.text('ABERTA').hitTestable(), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
