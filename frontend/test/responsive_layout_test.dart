import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';
import 'package:frontend/pages/entregas_page.dart';
import 'package:frontend/pages/empresas_page.dart';
import 'package:frontend/pages/gerar_entregas_page.dart';
import 'package:frontend/pages/obrigacoes_empresa_page.dart';
import 'package:frontend/pages/entrega_detalhe_page.dart';
import 'package:frontend/models/empresa.dart';
import 'package:frontend/widgets/responsive_breakpoints.dart';

import 'api_fake.dart';

void main() {
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('breakpointsCobremCelularTabletEDesktop', () {
    expect(ResponsiveBreakpoints.ehCelular(599), isTrue);
    expect(ResponsiveBreakpoints.ehTablet(600), isTrue);
    expect(ResponsiveBreakpoints.ehTablet(1024), isTrue);
    expect(ResponsiveBreakpoints.ehDesktop(1025), isTrue);
  });

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

  testWidgets('card permite consultar a situacao em uma janela estreita', (
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
    expect(find.text('ABERTA').hitTestable(), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('telas principais cabem nos breakpoints com texto ampliado', (tester) async {
    final empresa = Empresa.fromJson({
      'id': 1,
      'nome': 'Padaria',
      'cnpj': '11222333000181',
      'regimeTributario': 'SIMPLES',
      'ativa': true,
    });
    final api = apiFake({
      'GET /empresas': [
        {'id': 1, 'nome': 'Padaria', 'cnpj': '11222333000181', 'regimeTributario': 'SIMPLES', 'ativa': true},
      ],
      'GET /catalogo-obrigacoes': [
        {'tipo': 'DAS', 'descricao': 'Guia mensal', 'periodicidade': 'MENSAL', 'regimesAceitos': ['SIMPLES']},
      ],
      'GET /empresas/1/obrigacoes': [],
      'GET /entregas': [entregaJson()],
      'GET /entregas/1': entregaJson(),
      'GET /entregas/1/documentos': [],
    });

    for (final largura in [360.0, 768.0, 1440.0]) {
      tester.view.physicalSize = Size(largura, 1000);
      tester.view.devicePixelRatio = 1;
      Widget app(Widget page) => MaterialApp(
        home: Builder(builder: (context) => MediaQuery(
          data: MediaQuery.of(context).copyWith(
            textScaler: TextScaler.linear(largura == 360 ? 1.3 : 1),
          ),
          child: page,
        )),
      );

      await tester.pumpWidget(app(EmpresasPage(api: api)));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'Empresas em $largura px');

      await tester.pumpWidget(app(ObrigacoesEmpresaPage(api: api, empresa: empresa)));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'Obrigações em $largura px');

      await tester.pumpWidget(app(GerarEntregasPage(api: api)));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'Gerar entregas em $largura px');

      await tester.pumpWidget(app(EntregasPage(api: api)));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'Entregas em $largura px');

      await tester.pumpWidget(app(EntregaDetalhePage(api: api, entregaId: 1)));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'Detalhe em $largura px');
    }
  });
}
