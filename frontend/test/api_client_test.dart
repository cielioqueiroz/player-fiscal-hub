import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/api/api_client.dart';
import 'package:frontend/api/api_exception.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'api_fake.dart' show entregaJson;

const _empresaJson = {
  'id': 1,
  'nome': 'Declaração Ltda',
  'cnpj': '11222333000181',
  'regimeTributario': 'SIMPLES',
};

ApiClient _cliente(Future<http.Response> Function(http.Request) handler) =>
    ApiClient(baseUrl: 'http://localhost:8080', client: MockClient(handler));

http.Response _json(Object? corpo, int status) => http.Response.bytes(
  utf8.encode(jsonEncode(corpo)),
  status,
  headers: {'content-type': 'application/json'},
);

void main() {
  test('erroDoBackendViraApiException', () async {
    final api = _cliente((_) async => _json({'erro': 'CNPJ inválido'}, 400));
    await expectLater(
      api.cadastrarEmpresa('X', '1', 'SIMPLES'),
      throwsA(
        isA<ApiException>()
            .having((e) => e.status, 'status', 400)
            .having((e) => e.mensagem, 'mensagem', 'CNPJ inválido'),
      ),
    );
  });

  test('resumoDeEntregasAplicaFiltroPorEmpresa', () async {
    http.Request? requisicao;
    final api = _cliente((r) async {
      requisicao = r;
      return _json({
        'total': 4,
        'abertas': 2,
        'atrasadas': 1,
        'concluidas': 1,
        'canceladas': 0,
        'venceEmAte7Dias': 1,
        'vencidasSemBaixa': 1,
      }, 200);
    });

    final resumo = await api.resumirEntregas(empresaId: 5);

    expect(requisicao?.url.path, '/entregas/resumo');
    expect(requisicao?.url.queryParameters, {'empresaId': '5'});
    expect(resumo.total, 4);
    expect(resumo.vencidasSemBaixa, 1);
  });

  test('corpoDeErroSemJson', () async {
    final api = _cliente((_) async => http.Response('<html>', 500));
    await expectLater(
      api.listarEmpresas(),
      throwsA(
        isA<ApiException>().having((e) => e.mensagem, 'mensagem', 'Erro 500'),
      ),
    );
  });

  test('semConexaoViraApiException', () async {
    final api = _cliente(
      (_) async => throw http.ClientException('XMLHttpRequest error'),
    );
    await expectLater(
      api.listarEmpresas(),
      throwsA(
        isA<ApiException>()
            .having((e) => e.status, 'status', 0)
            .having(
              (e) => e.mensagem,
              'mensagem',
              'Não foi possível conectar à API (http://localhost:8080)',
            ),
      ),
    );
  });

  test('respostaNaoJsonViraApiException', () async {
    final api = _cliente((_) async => http.Response('<html>', 200));
    await expectLater(
      api.listarEmpresas(),
      throwsA(isA<ApiException>().having((e) => e.status, 'status', 0)),
    );
  });

  test('contratoDiferenteViraApiException', () async {
    final api = _cliente(
      (_) async => _json([
        {'id': 'nao-e-numero'},
      ], 200),
    );
    await expectLater(api.listarEmpresas(), throwsA(isA<ApiException>()));
  });

  test('acentosSemCharsetChegamCorretos', () async {
    final api = _cliente(
      (_) async => http.Response.bytes(
        utf8.encode(jsonEncode([_empresaJson])),
        200,
        headers: {'content-type': 'application/json'},
      ),
    );
    final empresas = await api.listarEmpresas();
    expect(empresas.first.nome, 'Declaração Ltda');
  });

  test('cadastrarEmpresaEnviaRegimeTributario', () async {
    late http.Request req;
    final api = _cliente((r) async {
      req = r;
      return _json(_empresaJson, 201);
    });
    await api.cadastrarEmpresa('X', '1', 'SIMPLES');
    expect(req.method, 'POST');
    expect(req.url.path, '/empresas');
    expect(req.headers['content-type'], startsWith('application/json'));
    expect(jsonDecode(req.body), {
      'nome': 'X',
      'cnpj': '1',
      'regimeTributario': 'SIMPLES',
    });
  });

  test('listarEntregasSoEnviaFiltrosPreenchidos', () async {
    late http.Request req;
    final api = _cliente((r) async {
      req = r;
      return _json({
        'conteudo': [],
        'pagina': 0,
        'tamanho': 20,
        'totalElementos': 0,
        'totalPaginas': 0,
      }, 200);
    });
    await api.listarEntregas(
      empresaId: 3,
      competencia: '  ',
      situacao: 'ABERTA',
    );
    expect(req.url.queryParameters, {
      'empresaId': '3',
      'situacao': 'ABERTA',
      'page': '0',
      'size': '20',
      'sort': 'dataVencimento,asc',
    });
  });

  test('alterarSituacaoOmiteCamposNulos', () async {
    late http.Request req;
    final api = _cliente((r) async {
      req = r;
      return _json(entregaJson(), 200);
    });
    await api.alterarSituacao(5, 'ATRASADA');
    expect(req.method, 'PATCH');
    expect(req.url.path, '/entregas/5/situacao');
    expect(jsonDecode(req.body), {'situacao': 'ATRASADA'});
  });

  test('removerObrigacaoAceita204SemCorpo', () async {
    final api = _cliente((_) async => http.Response('', 204));
    await api.removerObrigacao(1, 'DAS');
  });

  test('anexarDocumentoEnviaTipoPelaExtensao', () async {
    late http.Request req;
    final api = _cliente((r) async {
      req = r;
      return _json({
        'id': 1,
        'nomeArquivo': 'GUIA.PDF',
        'contentType': 'application/pdf',
        'tamanhoBytes': 3,
        'enviadoEm': '2026-06-18T10:00:00',
      }, 201);
    });
    await api.anexarDocumento(1, 'GUIA.PDF', Uint8List.fromList([1, 2, 3]));
    expect(req.url.path, '/entregas/1/documentos');
    final corpo = req.body.toLowerCase();
    expect(corpo, contains('name="arquivo"'));
    expect(corpo, contains('filename="guia.pdf"'));
    expect(corpo, contains('content-type: application/pdf'));
  });

  test('tipoPorExtensaoIgnoraMaiusculas', () {
    expect(
      [
        'a.pdf',
        'A.PNG',
        'x.jpg',
        'x.JPEG',
        'n.xml',
        't.txt',
        'sem_extensao',
      ].map((n) => tipoPorExtensao(n).mimeType).toList(),
      [
        'application/pdf',
        'image/png',
        'image/jpeg',
        'image/jpeg',
        'application/xml',
        'text/plain',
        'application/octet-stream',
      ],
    );
  });
}
