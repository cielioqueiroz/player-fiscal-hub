import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/api/api_client.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

http.Response jsonResp(Object? corpo, [int status = 200]) => http.Response.bytes(
      utf8.encode(jsonEncode(corpo)),
      status,
      headers: {'content-type': 'application/json; charset=utf-8'},
    );

ApiClient apiFake(Map<String, Object?> rotas, {List<http.Request>? chamadas}) {
  return ApiClient(
    client: MockClient((req) async {
      chamadas?.add(req);
      final chave = '${req.method} ${req.url.path}';
      if (!rotas.containsKey(chave)) {
        return jsonResp({'erro': 'rota não simulada: $chave'}, 404);
      }
      var valor = rotas[chave];
      if (valor is Function) valor = valor(req);
      if (valor is Future) valor = await valor;
      return valor is http.Response ? valor : jsonResp(valor);
    }),
  );
}

Map<String, dynamic> entregaJson([Map<String, dynamic> altera = const {}]) => {
      'id': 1,
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
      'vencida': false,
      'transicoesPermitidas': ['ATRASADA', 'CONCLUIDA', 'CANCELADA'],
      ...altera,
    };

void usarTelaGrande(WidgetTester tester) {
  tester.view.physicalSize = const Size(1400, 1000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
}
