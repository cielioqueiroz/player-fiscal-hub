import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import '../models/empresa.dart';
import '../models/entrega.dart';
import '../models/tipo_obrigacao.dart';
import '../widgets/erro_carregamento.dart';
import '../widgets/formatadores.dart';
import '../widgets/situacao_chip.dart';
import '../widgets/responsive_breakpoints.dart';
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

  Future<List<Entrega>> _buscarEntregas() => widget.api.listarEntregas(
    empresaId: _aplicados.empresaId,
    tipo: _aplicados.tipo,
    competencia: _aplicados.competencia,
    situacao: _aplicados.situacao,
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
      ]);
      if (!mounted) return;
      final empresas = resultados[0] as List<Empresa>;
      final entregas = resultados[1] as List<Entrega>;
      setState(() {
        _empresas = empresas;
        if (minha == _requisicao) {
          _entregas = entregas;
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
      final entregas = await _buscarEntregas();
      if (!mounted || minha != _requisicao) return;
      setState(() {
        _entregas = entregas;
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
      body: LayoutBuilder(
        builder: (context, constraints) {
          final celular = ResponsiveBreakpoints.ehCelular(constraints.maxWidth);
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (celular)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: OutlinedButton.icon(
                    key: const Key('abrirFiltros'),
                    onPressed: _abrirFiltros,
                    icon: const Icon(Icons.filter_list),
                    label: const Text('Filtros'),
                  ),
                )
              else
                Padding(padding: const EdgeInsets.all(16), child: _filtros()),
              const Divider(height: 1),
              Expanded(child: _corpo()),
            ],
          );
        },
      ),
    );
  }

  Future<void> _abrirFiltros() => showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (modalContext) => SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          16,
          16,
          16,
          MediaQuery.viewInsetsOf(modalContext).bottom + 16,
        ),
        child: FractionallySizedBox(
          heightFactor: 0.85,
          child: Column(
            children: [
              Expanded(child: SingleChildScrollView(child: _filtros(mostrarAcoes: false))),
              Row(
                children: [
                  TextButton(onPressed: _limpar, child: const Text('Limpar')),
                  const Spacer(),
                  FilledButton(
                    onPressed: () {
                      _aplicarFiltros();
                      Navigator.pop(modalContext);
                    },
                    child: const Text('Filtrar'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    ),
  );

  Widget _filtros({bool mostrarAcoes = true}) {
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
        if (mostrarAcoes) ...[
          FilledButton.tonal(
            onPressed: _aplicarFiltros,
            child: const Text('Filtrar'),
          ),
          TextButton(onPressed: _limpar, child: const Text('Limpar')),
        ],
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
        if (!ResponsiveBreakpoints.ehDesktop(constraints.maxWidth)) {
          if (ResponsiveBreakpoints.ehCelular(constraints.maxWidth)) {
            return ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: _entregas.length,
              itemBuilder: (context, index) => _cartaoEntrega(_entregas[index]),
            );
          }
          return GridView.builder(
            padding: const EdgeInsets.all(12),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              childAspectRatio: 1.6,
            ),
            itemCount: _entregas.length,
            itemBuilder: (context, index) => _cartaoEntrega(_entregas[index]),
          );
        }
        return SingleChildScrollView(
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
        );
      },
    );
  }

  Widget _cartaoEntrega(Entrega entrega) => Card(
    child: InkWell(
      onTap: () => _abrir(entrega),
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              entrega.empresaNome,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            Text('${entrega.tipo} · ${entrega.competencia}'),
            Text('Vencimento: ${formatarData(entrega.dataVencimento)}'),
            Align(
              alignment: Alignment.centerLeft,
              child: SituacaoChip(situacao: entrega.situacao),
            ),
          ],
        ),
      ),
    ),
  );

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
