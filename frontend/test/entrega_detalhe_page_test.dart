import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/pages/entrega_detalhe_page.dart';
import 'package:frontend/widgets/formatadores.dart';
import 'package:http/http.dart' as http;

import 'api_fake.dart';

Future<List<http.Request>> abrir(
  WidgetTester tester, {
  Map<String, dynamic> entrega = const {},
  Object? documentos = const [],
  Map<String, Object?> extras = const {},
  Future<ArquivoEscolhido?> Function()? escolherArquivo,
}) async {
  usarTelaGrande(tester);
  final chamadas = <http.Request>[];
  final api = apiFake({
    'GET /entregas/1': entregaJson(entrega),
    'GET /entregas/1/documentos': documentos,
    'GET /catalogo-obrigacoes': [
      {
        'tipo': 'DAS',
        'descricao': 'Documento de Arrecadação do Simples Nacional',
        'periodicidade': 'MENSAL',
        'regimesAceitos': ['SIMPLES'],
      },
    ],
    ...extras,
  }, chamadas: chamadas);
  await tester.pumpWidget(MaterialApp(
    home: EntregaDetalhePage(api: api, entregaId: 1, escolherArquivo: escolherArquivo),
  ));
  await tester.pumpAndSettle();
  return chamadas;
}

bool Function() _confirmarHabilitado(WidgetTester tester) => () =>
    tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Confirmar')).onPressed != null;

void main() {
  testWidgets('mostraUmBotaoPorTransicao', (tester) async {
    await abrir(tester, entrega: {'transicoesPermitidas': ['CONCLUIDA', 'CANCELADA']});

    expect(find.text('Concluir'), findsOneWidget);
    expect(find.text('Cancelar'), findsOneWidget);
    expect(find.text('Marcar como atrasada'), findsNothing);
  });

  testWidgets('entregaFinalNaoTemBotoes', (tester) async {
    await abrir(tester, entrega: {
      'situacao': 'CONCLUIDA',
      'dataConclusao': '2026-06-18',
      'transicoesPermitidas': [],
    });

    expect(find.text('Entrega finalizada: não pode mais ser alterada.'), findsOneWidget);
    expect(find.text('Concluir'), findsNothing);
    expect(find.textContaining('18/06/2026'), findsOneWidget);
  });

  testWidgets('canceladaMostraMotivo', (tester) async {
    await abrir(tester, entrega: {
      'situacao': 'CANCELADA',
      'motivoCancelamento': 'Empresa encerrada',
      'transicoesPermitidas': [],
    });

    expect(find.textContaining('Empresa encerrada'), findsOneWidget);
  });

  testWidgets('cabecalhoMostraEmpresaCnpjTipoEVencimentoFormatados', (tester) async {
    await abrir(tester);

    expect(find.text('Padaria'), findsOneWidget);
    expect(find.textContaining('11.222.333/0001-81'), findsOneWidget);
    expect(find.textContaining('2026-05'), findsOneWidget);
    expect(find.textContaining('Documento de Arrecadação do Simples Nacional'), findsOneWidget);
    expect(find.textContaining('20/06/2026'), findsOneWidget);
  });

  testWidgets('cancelarSemMotivoDesabilitaConfirmar', (tester) async {
    await abrir(tester);

    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();
    expect(_confirmarHabilitado(tester)(), isFalse);

    await tester.enterText(find.byType(TextField), 'x');
    await tester.pump();
    expect(_confirmarHabilitado(tester)(), isTrue);
  });

  testWidgets('cancelarEnviaMotivoSemEspacosNasPontas', (tester) async {
    Map<String, dynamic>? corpo;
    await abrir(tester, extras: {
      'PATCH /entregas/1/situacao': (http.Request r) {
        corpo = jsonDecode(r.body) as Map<String, dynamic>;
        return entregaJson({'situacao': 'CANCELADA', 'transicoesPermitidas': []});
      },
    });

    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '  Empresa encerrada ');
    await tester.pump();
    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar'));
    await tester.pumpAndSettle();

    expect(corpo, {'situacao': 'CANCELADA', 'motivoCancelamento': 'Empresa encerrada'});
  });

  testWidgets('concluirEnviaADataDeHojeEscolhidaNoCalendario', (tester) async {
    Map<String, dynamic>? corpo;
    await abrir(tester, extras: {
      'PATCH /entregas/1/situacao': (http.Request r) {
        corpo = jsonDecode(r.body) as Map<String, dynamic>;
        return entregaJson({'situacao': 'CONCLUIDA', 'transicoesPermitidas': []});
      },
    });

    await tester.tap(find.text('Concluir'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();

    expect(corpo, {'situacao': 'CONCLUIDA', 'dataConclusao': dataIso(DateTime.now())});
  });

  testWidgets('sucessoAtualizaComRespostaDoPatch', (tester) async {
    await abrir(tester, extras: {
      'PATCH /entregas/1/situacao': entregaJson({
        'situacao': 'ATRASADA',
        'transicoesPermitidas': ['CONCLUIDA', 'CANCELADA'],
      }),
    });

    await tester.tap(find.text('Marcar como atrasada'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar'));
    await tester.pumpAndSettle();

    expect(find.text('Marcar como atrasada'), findsNothing);
    expect(find.text('ATRASADA'), findsOneWidget);
  });

  testWidgets('entregaFinalizadaPorOutraPessoaDevolve422ERecarrega', (tester) async {
    var leituras = 0;
    await abrir(tester, extras: {
      'GET /entregas/1': (http.Request _) => ++leituras == 1
          ? entregaJson()
          : entregaJson({'situacao': 'CONCLUIDA', 'transicoesPermitidas': []}),
      'PATCH /entregas/1/situacao': jsonResp({'erro': 'Não é possível alterar uma entrega CONCLUIDA'}, 422),
    });
    expect(find.text('Anexar documento'), findsOneWidget);

    await tester.tap(find.text('Marcar como atrasada'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar'));
    await tester.pumpAndSettle();

    expect(find.text('Não é possível alterar uma entrega CONCLUIDA'), findsOneWidget);
    expect(find.text('Entrega finalizada: não pode mais ser alterada.'), findsOneWidget);
    expect(find.text('Anexar documento'), findsNothing);
  });

  testWidgets('conflito409RecarregaEntrega', (tester) async {
    const mensagem = 'A entrega foi alterada por outra pessoa. Recarregue e tente novamente.';
    final chamadas = await abrir(tester, extras: {
      'PATCH /entregas/1/situacao': jsonResp({'erro': mensagem}, 409),
    });

    await tester.tap(find.text('Marcar como atrasada'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar'));
    await tester.pumpAndSettle();

    expect(find.text(mensagem), findsOneWidget);
    expect(chamadas.where((r) => r.method == 'GET' && r.url.path == '/entregas/1'), hasLength(2));
  });

  testWidgets('erroAoCarregarMostraTentarNovamente', (tester) async {
    await abrir(tester, extras: {
      'GET /entregas/1': jsonResp({'erro': 'Erro interno. Tente novamente.'}, 500),
    });

    expect(find.text('Erro interno. Tente novamente.'), findsOneWidget);
    expect(find.text('Tentar novamente'), findsOneWidget);
  });

  group('documentos', () {
    final guia = {
      'id': 5,
      'nomeArquivo': 'guia.pdf',
      'contentType': 'application/pdf',
      'tamanhoBytes': 250880,
      'enviadoEm': '2026-06-18T10:00:00',
    };

    ArquivoEscolhido pdf(int tamanho) =>
        (nome: 'guia.pdf', tamanho: tamanho, lerBytes: () async => Uint8List(tamanho));

    testWidgets('listaDocumentosFormatada', (tester) async {
      await abrir(tester, documentos: [guia]);

      expect(find.text('guia.pdf'), findsOneWidget);
      expect(find.textContaining('245 KB'), findsOneWidget);
      expect(find.textContaining('18/06/2026'), findsOneWidget);
      expect(find.text('Baixar'), findsOneWidget);
    });

    testWidgets('semDocumentos', (tester) async {
      await abrir(tester);

      expect(find.text('Nenhum documento anexado.'), findsOneWidget);
    });

    testWidgets('entregaFinalNaoMostraAnexar', (tester) async {
      await abrir(tester, entrega: {'situacao': 'CONCLUIDA', 'transicoesPermitidas': []});

      expect(find.text('Anexar documento'), findsNothing);
    });

    testWidgets('entregaAbertaMostraAnexar', (tester) async {
      await abrir(tester);

      expect(find.text('Anexar documento'), findsOneWidget);
    });

    testWidgets('arquivoMaiorQue10MbAvisaSemChamarApi', (tester) async {
      final chamadas = await abrir(tester, escolherArquivo: () async => pdf(10 * 1024 * 1024 + 1));

      await tester.tap(find.text('Anexar documento'));
      await tester.pumpAndSettle();

      expect(find.text('O arquivo excede o limite de 10 MB'), findsOneWidget);
      expect(chamadas.where((r) => r.method == 'POST'), isEmpty);
    });

    testWidgets('selecaoCanceladaNaoFazNada', (tester) async {
      final chamadas = await abrir(tester, escolherArquivo: () async => null);

      await tester.tap(find.text('Anexar documento'));
      await tester.pumpAndSettle();

      expect(chamadas.where((r) => r.method == 'POST'), isEmpty);
    });

    testWidgets('anexarEnviaERecarregaALista', (tester) async {
      var leituras = 0;
      final chamadas = await abrir(
        tester,
        documentos: (http.Request _) => ++leituras == 1 ? [] : [guia],
        extras: {'POST /entregas/1/documentos': jsonResp(guia, 201)},
        escolherArquivo: () async => pdf(1024),
      );
      expect(find.text('guia.pdf'), findsNothing);

      await tester.tap(find.text('Anexar documento'));
      await tester.pumpAndSettle();

      expect(chamadas.where((r) => r.method == 'POST' && r.url.path == '/entregas/1/documentos'), hasLength(1));
      expect(find.text('guia.pdf'), findsOneWidget);
    });

    testWidgets('erroDoUploadAparece', (tester) async {
      await abrir(
        tester,
        extras: {
          'POST /entregas/1/documentos': jsonResp({'erro': 'Tipo de arquivo não permitido: application/x-msdownload'}, 400),
        },
        escolherArquivo: () async => pdf(10),
      );

      await tester.tap(find.text('Anexar documento'));
      await tester.pumpAndSettle();

      expect(find.text('Tipo de arquivo não permitido: application/x-msdownload'), findsOneWidget);
    });
  });
}
