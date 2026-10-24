import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import '../models/empresa.dart';
import '../models/entrega.dart';
import '../models/pagina_entregas.dart';
import '../models/resumo_entregas.dart';
import '../models/tipo_obrigacao.dart';
import '../widgets/erro_carregamento.dart';
import '../widgets/formatadores.dart';
import '../widgets/situacao_chip.dart';
import 'entrega_detalhe_page.dart';

class EntregasPage extends StatefulWidget {
  final ApiClient api;
  final int? empresaIdInicial;

  const EntregasPage({super.key, required this.api, this.empresaIdInicial});

  @override
  State<EntregasPage> createState() => _EntregasPageState();
}

class _FiltrosEntregas {
  final int? empresaId;
  final String? tipo;
  final String? situacao;
  final String competencia;

  const _FiltrosEntregas({
    this.empresaId,
    this.tipo,
    this.situacao,
    this.competencia = '',
  });
}

class _EntregasPageState extends State<EntregasPage> {
  final _competencia = TextEditingController();
  List<Empresa> _empresas = [];
  List<Entrega> _entregas = [];
  ResumoEntregas? _resumo;
  int _pagina = 0;
  int _totalPaginas = 0;
  int _totalElementos = 0;
  bool _carregandoMais = false;
  String? _erroMais;
  String _campoOrdenacao = 'dataVencimento';
  bool _crescente = true;
  int? _empresaId;
  String? _tipo;
  String? _situacao;
  _FiltrosEntregas _aplicados = const _FiltrosEntregas();
  String? _erro;
  bool _carregando = true;

  @override
  void initState() {
    super.initState();
    _empresaId = widget.empresaIdInicial;
    _aplicados = _FiltrosEntregas(empresaId: _empresaId);
    _carregar();
  }

  @override
  void dispose() {
    _competencia.dispose();
    super.dispose();
  }

  Future<PaginaEntregas> _buscarEntregas({int pagina = 0}) => widget.api.listarEntregas(
    empresaId: _aplicados.empresaId,
    tipo: _aplicados.tipo,
    competencia: _aplicados.competencia,
    situacao: _aplicados.situacao,
    pagina: pagina,
    campoOrdenacao: _campoOrdenacao,
    crescente: _crescente,
  );

  Future<void> _aplicarFiltros() {
    _aplicados = _FiltrosEntregas(
      empresaId: _empresaId,
      tipo: _tipo,
      situacao: _situacao,
      competencia: _competencia.text.trim(),
    );
    return _recarregarLista();
  }

  int _requisicao = 0;

  Future<void> _carregar() async {
    final minha = ++_requisicao;
    setState(() {
      _carregando = true;
      _erro = null;
    });
    try {
      final resultados = await Future.wait<Object>([
        widget.api.listarEmpresas(),
        _buscarEntregas(),
        widget.api.resumirEntregas(empresaId: _aplicados.empresaId),
      ]);
      if (!mounted) return;
      final empresas = resultados[0] as List<Empresa>;
      final pagina = resultados[1] as PaginaEntregas;
      final resumo = resultados[2] as ResumoEntregas;
      setState(() {
        _empresas = empresas;
        _resumo = resumo;
        if (minha == _requisicao) {
          _entregas = pagina.conteudo;
          _pagina = pagina.pagina;
          _totalPaginas = pagina.totalPaginas;
          _totalElementos = pagina.totalElementos;
          _carregando = false;
        }
      });
    } on ApiException catch (e) {
      if (!mounted || minha != _requisicao) return;
      setState(() {
        _erro = e.mensagem;
        _carregando = false;
      });
    }
  }

  Future<void> _recarregarLista() async {
    final minha = ++_requisicao;
    setState(() {
      _carregando = true;
      _erro = null;
    });
    try {
      final resultados = await Future.wait<Object>([
        _buscarEntregas(),
        widget.api.resumirEntregas(empresaId: _aplicados.empresaId),
      ]);
      if (!mounted || minha != _requisicao) return;
      setState(() {
        final pagina = resultados[0] as PaginaEntregas;
        _entregas = pagina.conteudo;
        _pagina = pagina.pagina;
        _totalPaginas = pagina.totalPaginas;
        _totalElementos = pagina.totalElementos;
        _resumo = resultados[1] as ResumoEntregas;
        _carregando = false;
      });
    } on ApiException catch (e) {
      if (!mounted || minha != _requisicao) return;
      setState(() {
        _erro = e.mensagem;
        _carregando = false;
      });
    }
  }

  Future<void> _alterarOrdenacao({String? campo, bool? crescente}) async {
    setState(() {
      _campoOrdenacao = campo ?? _campoOrdenacao;
      _crescente = crescente ?? _crescente;
      _pagina = 0;
    });
    await _recarregarLista();
  }

  Future<void> _carregarMais() async {
    if (_carregandoMais || _carregando || _pagina + 1 >= _totalPaginas) return;
    setState(() {
      _carregandoMais = true;
      _erroMais = null;
    });
    try {
      final pagina = await _buscarEntregas(pagina: _pagina + 1);
      if (!mounted) return;
      setState(() {
        _entregas = [..._entregas, ...pagina.conteudo];
        _pagina = pagina.pagina;
        _totalPaginas = pagina.totalPaginas;
        _totalElementos = pagina.totalElementos;
        _carregandoMais = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _erroMais = e.mensagem;
        _carregandoMais = false;
      });
    }
  }

  Future<void> _irParaPagina(int pagina) async {
    if (pagina < 0 || pagina >= _totalPaginas) return;
    final minha = ++_requisicao;
    setState(() {
      _carregando = true;
      _erro = null;
    });
    try {
      final resultado = await _buscarEntregas(pagina: pagina);
      if (!mounted || minha != _requisicao) return;
      setState(() {
        _entregas = resultado.conteudo;
        _pagina = resultado.pagina;
        _totalPaginas = resultado.totalPaginas;
        _totalElementos = resultado.totalElementos;
        _carregando = false;
      });
    } on ApiException catch (e) {
      if (!mounted || minha != _requisicao) return;
      setState(() {
        _erro = e.mensagem;
        _carregando = false;
      });
    }
  }

  void _limpar() {
    _competencia.clear();
    setState(() {
      _empresaId = null;
      _tipo = null;
      _situacao = null;
    });
    _aplicarFiltros();
  }

  Future<void> _abrir(Entrega e) async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => EntregaDetalhePage(api: widget.api, entregaId: e.id),
      ),
    );
    if (!mounted) return;
    _recarregarLista();
  }

  bool get _temFiltro =>
      _aplicados.empresaId != null ||
      _aplicados.tipo != null ||
      _aplicados.situacao != null ||
      _aplicados.competencia.isNotEmpty;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Entregas')),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_resumo != null) _cardsResumo(_resumo!),
          Padding(padding: const EdgeInsets.all(16), child: _filtros()),
          const Divider(height: 1),
          Expanded(child: _corpo()),
        ],
      ),
    );
  }

  Widget _cardsResumo(ResumoEntregas resumo) => Padding(
    padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
    child: Wrap(
      spacing: 10,
      runSpacing: 8,
      children: [
        _cardSituacao('Abertas', resumo.abertas, 'ABERTA'),
        _cardSituacao('Atrasadas', resumo.atrasadas, 'ATRASADA'),
        _cardSituacao('Concluídas', resumo.concluidas, 'CONCLUIDA'),
        _cardSituacao('Canceladas', resumo.canceladas, 'CANCELADA'),
        _cardNumero('Vencem em até 7 dias', resumo.venceEmAte7Dias),
        _cardNumero('Vencidas sem baixa', resumo.vencidasSemBaixa),
      ],
    ),
  );

  Widget _cardSituacao(String titulo, int quantidade, String situacao) => SizedBox(
    width: 155,
    child: Card(
      child: InkWell(
        key: Key('resumo$situacao'),
        borderRadius: BorderRadius.circular(12),
        onTap: () {
          setState(() => _situacao = situacao);
          _aplicarFiltros();
        },
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(titulo),
            Text('$quantidade', style: Theme.of(context).textTheme.headlineSmall),
          ]),
        ),
      ),
    ),
  );

  Widget _cardNumero(String titulo, int quantidade) => SizedBox(
    width: 155,
    child: Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(titulo),
          Text('$quantidade', style: Theme.of(context).textTheme.headlineSmall),
        ]),
      ),
    ),
  );

  Widget _filtros() {
    final empresaSelecionada = _empresas.any((e) => e.id == _empresaId)
        ? _empresaId
        : null;
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        _filtro<int?>(
          rotulo: 'Empresa',
          chave: const Key('filtroEmpresa'),
          valor: empresaSelecionada,
          itens: [
            const DropdownMenuItem(value: null, child: Text('Todas')),
            for (final e in _empresas)
              DropdownMenuItem(value: e.id, child: Text(e.nome)),
          ],
          aoMudar: (v) => setState(() => _empresaId = v),
        ),
        _filtro<String?>(
          rotulo: 'Tipo',
          chave: const Key('filtroTipo'),
          valor: _tipo,
          itens: [
            const DropdownMenuItem(value: null, child: Text('Todos')),
            for (final t in tiposObrigacao)
              DropdownMenuItem(value: t, child: Text(t)),
          ],
          aoMudar: (v) => setState(() => _tipo = v),
        ),
        _filtro<String?>(
          rotulo: 'Situação',
          chave: const Key('filtroSituacao'),
          valor: _situacao,
          itens: [
            const DropdownMenuItem(value: null, child: Text('Todas')),
            for (final s in situacoesEntrega)
              DropdownMenuItem(value: s, child: Text(s)),
          ],
          aoMudar: (v) => setState(() => _situacao = v),
        ),
        SizedBox(
          width: 190,
          child: TextField(
            controller: _competencia,
            decoration: const InputDecoration(
              labelText: 'Competência',
              hintText: '2026-09 ou 2025',
              border: OutlineInputBorder(),
              isDense: true,
            ),
            onSubmitted: (_) => _aplicarFiltros(),
          ),
        ),
        _filtro<String>(
          rotulo: 'Ordenar por',
          chave: const Key('ordenacaoCampo'),
          valor: _campoOrdenacao,
          itens: const [
            DropdownMenuItem(value: 'dataVencimento', child: Text('Vencimento')),
            DropdownMenuItem(value: 'competencia', child: Text('Competência')),
            DropdownMenuItem(value: 'tipo', child: Text('Tipo')),
            DropdownMenuItem(value: 'situacao', child: Text('Situação')),
            DropdownMenuItem(value: 'empresa', child: Text('Empresa')),
          ],
          aoMudar: (v) => _alterarOrdenacao(campo: v),
        ),
        OutlinedButton.icon(
          key: const Key('ordenacaoDirecao'),
          onPressed: () => _alterarOrdenacao(crescente: !_crescente),
          icon: Icon(_crescente ? Icons.arrow_upward : Icons.arrow_downward),
          label: Text(_crescente ? 'Crescente' : 'Decrescente'),
        ),
        FilledButton.tonal(
          onPressed: _aplicarFiltros,
          child: const Text('Filtrar'),
        ),
        TextButton(onPressed: _limpar, child: const Text('Limpar')),
      ],
    );
  }

  Widget _filtro<T>({
    required String rotulo,
    required Key chave,
    required T valor,
    required List<DropdownMenuItem<T>> itens,
    required ValueChanged<T?> aoMudar,
  }) {
    return SizedBox(
      width: 200,
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: rotulo,
          border: const OutlineInputBorder(),
          isDense: true,
        ),
        child: DropdownButtonHideUnderline(
          child: DropdownButton<T>(
            key: chave,
            value: valor,
            isExpanded: true,
            isDense: true,
            items: itens,
            onChanged: aoMudar,
          ),
        ),
      ),
    );
  }

  Widget _corpo() {
    if (_carregando) return const Center(child: CircularProgressIndicator());
    if (_erro != null) {
      return ErroCarregamento(mensagem: _erro!, onTentarNovamente: _carregar);
    }
    if (_entregas.isEmpty) {
      return Center(
        child: Text(
          _temFiltro
              ? 'Nenhuma entrega encontrada com esses filtros.'
              : "Nenhuma entrega. Use 'Gerar entregas'.",
        ),
      );
    }
    return LayoutBuilder(
      builder: (context, constraints) {
        final mobile = constraints.maxWidth < 600;
        if (mobile) {
          return NotificationListener<ScrollNotification>(
            onNotification: (notification) {
              if (notification.metrics.extentAfter < 240) _carregarMais();
              return false;
            },
            child: ListView.builder(
              itemCount: _entregas.length + (_carregandoMais || _erroMais != null ? 1 : 0),
              itemBuilder: (context, index) {
                if (index == _entregas.length) {
                  if (_erroMais != null) {
                    return TextButton(onPressed: _carregarMais, child: Text('Tentar novamente: $_erroMais'));
                  }
                  return const Center(child: Padding(padding: EdgeInsets.all(16), child: CircularProgressIndicator()));
                }
                final entrega = _entregas[index];
                return Card(
                  margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
                  child: ListTile(
                    title: Text('${entrega.empresaNome} • ${entrega.tipo}'),
                    subtitle: Text('Competência ${entrega.competencia} • Vence ${formatarData(entrega.dataVencimento)}'),
                    trailing: SituacaoChip(situacao: entrega.situacao),
                    onTap: () => _abrir(entrega),
                  ),
                );
              },
            ),
          );
        }
        return Column(
          children: [
            Expanded(child: SingleChildScrollView(
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: ConstrainedBox(
                  constraints: BoxConstraints(minWidth: constraints.maxWidth),
                  child: DataTable(
              showCheckboxColumn: false,
              columns: const [
                DataColumn(label: Text('Empresa')),
                DataColumn(label: Text('Tipo')),
                DataColumn(label: Text('Competência')),
                DataColumn(label: Text('Vencimento')),
                DataColumn(label: Text('Situação')),
              ],
              rows: [
                for (final e in _entregas)
                  DataRow(
                    onSelectChanged: (_) => _abrir(e),
                    cells: [
                      DataCell(Text(e.empresaNome)),
                      DataCell(Text(e.tipo)),
                      DataCell(Text(e.competencia)),
                      DataCell(_vencimento(e)),
                      DataCell(SituacaoChip(situacao: e.situacao)),
                    ],
                  ),
              ],
                  ),
                ),
              ),
            )),
            Padding(
              padding: const EdgeInsets.all(8),
              child: Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                Text('$_totalElementos registros'),
                IconButton(onPressed: _pagina > 0 ? () => _irParaPagina(_pagina - 1) : null,
                    tooltip: 'Página anterior', icon: const Icon(Icons.chevron_left)),
                Text('${_pagina + 1} de ${_totalPaginas == 0 ? 1 : _totalPaginas}'),
                IconButton(onPressed: _pagina + 1 < _totalPaginas ? () => _irParaPagina(_pagina + 1) : null,
                    tooltip: 'Próxima página', icon: const Icon(Icons.chevron_right)),
              ]),
            ),
          ],
        );
      },
    );
  }

  Widget _vencimento(Entrega e) {
    final data = formatarData(e.dataVencimento);
    if (!e.vencida) return Text(data);
    const vermelho = TextStyle(color: Colors.red);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Icon(Icons.warning_amber, color: Colors.red, size: 16),
        const SizedBox(width: 4),
        Text(data, style: vermelho),
        const SizedBox(width: 6),
        const Text('Vencida', style: vermelho),
      ],
    );
  }
}
