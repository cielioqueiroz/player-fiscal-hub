import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';
import 'package:http/http.dart' as http;

import 'api_fake.dart';

void main() {
  testWidgets('abre em Empresas e navega entre as telas', (tester) async {
    usarTelaGrande(tester);
    final chamadas = <http.Request>[];
    final api = apiFake({'GET /empresas': [], 'GET /entregas': []}, chamadas: chamadas);

    await tester.pumpWidget(MeuApp(api: api));
    await tester.pumpAndSettle();

    expect(find.byType(NavigationRail), findsOneWidget);
    expect(find.text('Nenhuma empresa cadastrada. Use "Nova empresa".'), findsOneWidget);

    await tester.tap(find.text('Entregas'));
    await tester.pumpAndSettle();
    expect(chamadas.any((r) => r.url.path == '/entregas'), isTrue);
    expect(find.text("Nenhuma entrega. Use 'Gerar entregas'."), findsOneWidget);

    await tester.tap(find.text('Gerar entregas').first);
    await tester.pumpAndSettle();
    expect(find.text('Gerar'), findsOneWidget);
  });

  testWidgets('usa os textos do Material em português do Brasil', (tester) async {
    await tester.pumpWidget(MeuApp(api: apiFake({'GET /empresas': []})));
    await tester.pumpAndSettle();

    final contexto = tester.element(find.byType(NavigationRail));
    expect(MaterialLocalizations.of(contexto).cancelButtonLabel, 'Cancelar');
  });

  testWidgets('voltar para Entregas recarrega a lista', (tester) async {
    usarTelaGrande(tester);
    final chamadas = <http.Request>[];
    final api = apiFake({'GET /empresas': [], 'GET /entregas': []}, chamadas: chamadas);

    await tester.pumpWidget(MeuApp(api: api));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Entregas'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Gerar entregas').first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Entregas'));
    await tester.pumpAndSettle();

    expect(chamadas.where((r) => r.url.path == '/entregas'), hasLength(2));
  });
}
