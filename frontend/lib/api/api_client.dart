import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';

import '../config.dart';
import '../models/documento.dart';
import '../models/empresa.dart';
import '../models/entrega.dart';
import '../models/obrigacao_empresa.dart';
import '../models/pagina_entregas.dart';
import '../models/resumo_geracao.dart';
import '../models/resumo_entregas.dart';
import '../models/tipo_obrigacao.dart';
import 'api_exception.dart';

MediaType tipoPorExtensao(String nomeArquivo) {
  final ponto = nomeArquivo.lastIndexOf('.');
  final extensao = ponto < 0
      ? ''
      : nomeArquivo.substring(ponto + 1).toLowerCase();
  return switch (extensao) {
    'pdf' => MediaType('application', 'pdf'),
    'png' => MediaType('image', 'png'),
    'jpg' || 'jpeg' => MediaType('image', 'jpeg'),
    'xml' => MediaType('application', 'xml'),
    'txt' => MediaType('text', 'plain'),
    _ => MediaType('application', 'octet-stream'),
  };
}

List<T> Function(dynamic) _lista<T>(T Function(Map<String, dynamic>) de) =>
    (dados) =>
        (dados as List).map((e) => de(e as Map<String, dynamic>)).toList();

T Function(dynamic) _objeto<T>(T Function(Map<String, dynamic>) de) =>
    (dados) => de(dados as Map<String, dynamic>);

void _nada(dynamic _) {}

class ApiClient {
  final http.Client _client;
  final String baseUrl;

  ApiClient({http.Client? client, String? baseUrl})
    : _client = client ?? http.Client(),
      baseUrl = baseUrl ?? apiBaseUrl;

  static const _json = {'Content-Type': 'application/json'};

  dynamic _tratar(http.Response r) {
    if (r.statusCode >= 200 && r.statusCode < 300) {
      return r.bodyBytes.isEmpty ? null : jsonDecode(utf8.decode(r.bodyBytes));
    }
    String mensagem = 'Erro ${r.statusCode}';
    try {
      mensagem = jsonDecode(utf8.decode(r.bodyBytes))['erro'] ?? mensagem;
    } catch (_) {}
    throw ApiException(r.statusCode, mensagem);
  }

  Future<T> _chamar<T>(
    Future<http.Response> Function() requisicao,
    T Function(dynamic dados) converter,
  ) async {
    try {
      return converter(_tratar(await requisicao()));
    } on http.ClientException {
      throw ApiException(0, 'Não foi possível conectar à API ($baseUrl)');
    } on FormatException {
      throw ApiException(0, 'Resposta inesperada da API');
    } on TypeError {
      throw ApiException(0, 'Resposta inesperada da API');
    }
  }

  Uri _uri(String caminho, [Map<String, String>? query]) => Uri.parse(
    '$baseUrl$caminho',
  ).replace(queryParameters: query == null || query.isEmpty ? null : query);

  Future<List<Empresa>> listarEmpresas({bool incluirInativas = false}) =>
      _chamar(
        () => _client.get(_uri('/empresas', {
          if (incluirInativas) 'incluirInativas': 'true',
        })),
        _lista(Empresa.fromJson),
      );

  Future<Empresa> cadastrarEmpresa(String nome, String cnpj, String regime) {
    final corpo = jsonEncode({
      'nome': nome,
      'cnpj': cnpj,
      'regimeTributario': regime,
    });
    return _chamar(
      () => _client.post(_uri('/empresas'), headers: _json, body: corpo),
      _objeto(Empresa.fromJson),
    );
  }

  Future<Empresa> editarEmpresa(int id, String nome, String cnpj, String regime) {
    final corpo = jsonEncode({
      'nome': nome,
      'cnpj': cnpj,
      'regimeTributario': regime,
    });
    return _chamar(
      () => _client.put(_uri('/empresas/$id'), headers: _json, body: corpo),
      _objeto(Empresa.fromJson),
    );
  }

  Future<Empresa> alterarAtivaEmpresa(int id, bool ativa) => _chamar(
        () => _client.patch(
          _uri('/empresas/$id/ativa'),
          headers: _json,
          body: jsonEncode({'ativa': ativa}),
        ),
        _objeto(Empresa.fromJson),
      );

  Future<List<TipoObrigacao>> catalogo() => _chamar(
    () => _client.get(_uri('/catalogo-obrigacoes')),
    _lista(TipoObrigacao.fromJson),
  );

  Future<List<ObrigacaoEmpresa>> obrigacoesDaEmpresa(int empresaId) => _chamar(
    () => _client.get(_uri('/empresas/$empresaId/obrigacoes')),
    _lista(ObrigacaoEmpresa.fromJson),
  );

  Future<void> anexarObrigacao(int empresaId, String tipo) => _chamar(
    () => _client.post(
      _uri('/empresas/$empresaId/obrigacoes'),
      headers: _json,
      body: jsonEncode({'tipo': tipo}),
    ),
    _nada,
  );

  Future<void> removerObrigacao(int empresaId, String tipo) => _chamar(
    () => _client.delete(_uri('/empresas/$empresaId/obrigacoes/$tipo')),
    _nada,
  );

  Future<ResumoGeracao> gerarEntregas(String mesReferencia) => _chamar(
    () => _client.post(
      _uri('/entregas/gerar'),
      headers: _json,
      body: jsonEncode({'mesReferencia': mesReferencia}),
    ),
    _objeto(ResumoGeracao.fromJson),
  );

  Future<PaginaEntregas> listarEntregas({
    int? empresaId,
    String? tipo,
    String? competencia,
    String? situacao,
    int pagina = 0,
    int tamanho = 20,
    String campoOrdenacao = 'dataVencimento',
    bool crescente = true,
  }) {
    final filtros = <String, String>{
      if (empresaId != null) 'empresaId': '$empresaId',
      'tipo': ?tipo,
      if (competencia != null && competencia.trim().isNotEmpty)
        'competencia': competencia.trim(),
      'situacao': ?situacao,
      'page': '$pagina',
      'size': '$tamanho',
      'sort': '$campoOrdenacao,${crescente ? 'asc' : 'desc'}',
    };
    return _chamar(
      () => _client.get(_uri('/entregas', filtros)),
      _objeto(PaginaEntregas.fromJson),
    );
  }

  Future<ResumoEntregas> resumirEntregas({int? empresaId}) => _chamar(
    () => _client.get(_uri('/entregas/resumo', {
      if (empresaId != null) 'empresaId': '$empresaId',
    })),
    _objeto(ResumoEntregas.fromJson),
  );

  Future<Entrega> buscarEntrega(int id) => _chamar(
    () => _client.get(_uri('/entregas/$id')),
    _objeto(Entrega.fromJson),
  );

  Future<Entrega> alterarSituacao(
    int id,
    String situacao, {
    String? dataConclusao,
    String? motivoCancelamento,
  }) {
    final corpo = jsonEncode({
      'situacao': situacao,
      'dataConclusao': ?dataConclusao,
      'motivoCancelamento': ?motivoCancelamento,
    });
    return _chamar(
      () => _client.patch(
        _uri('/entregas/$id/situacao'),
        headers: _json,
        body: corpo,
      ),
      _objeto(Entrega.fromJson),
    );
  }

  Future<List<Documento>> listarDocumentos(int entregaId) => _chamar(
    () => _client.get(_uri('/entregas/$entregaId/documentos')),
    _lista(Documento.fromJson),
  );

  Future<void> excluirDocumento(int documentoId) => _chamar(
    () => _client.delete(_uri('/documentos/$documentoId')),
    _nada,
  );

  Future<void> anexarDocumento(
    int entregaId,
    String nomeArquivo,
    Uint8List bytes,
  ) {
    final request =
        http.MultipartRequest('POST', _uri('/entregas/$entregaId/documentos'))
          ..files.add(
            http.MultipartFile.fromBytes(
              'arquivo',
              bytes,
              filename: nomeArquivo,
              contentType: tipoPorExtensao(nomeArquivo),
            ),
          );
    return _chamar(
      () async => http.Response.fromStream(await _client.send(request)),
      _nada,
    );
  }

  Uri urlDownload(int documentoId) =>
      Uri.parse('$baseUrl/documentos/$documentoId/download');
}
