import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import '../models/empresa.dart';
import '../models/obrigacao_empresa.dart';
import '../models/tipo_obrigacao.dart';
import '../widgets/erro_carregamento.dart';
import '../widgets/formatadores.dart';
import '../widgets/responsive_breakpoints.dart';
import '../widgets/responsive_decision_dialog.dart';
import 'entregas_page.dart';

class ObrigacoesEmpresaPage extends StatefulWidget {
  final ApiClient api;
  final Empresa empresa;

  const ObrigacoesEmpresaPage({super.key, required this.api, required this.empresa});

  @override
  State<ObrigacoesEmpresaPage> createState() => _ObrigacoesEmpresaPageState();
}

class _ObrigacoesEmpresaPageState extends State<ObrigacoesEmpresaPage> {
  List<TipoObrigacao> _catalogo = [];
  List<ObrigacaoEmpresa> _vinculos = [];
  String? _erro;
  bool _carregando = true;
  bool _ocupado = false;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    setState(() {
      _carregando = true;
      _erro = null;
    });
    try {
      final resultados = await Future.wait<Object>([
        widget.api.catalogo(),
        widget.api.obrigacoesDaEmpresa(widget.empresa.id),
      ]);
      if (!mounted) return;
      setState(() {
        _catalogo = resultados[0] as List<TipoObrigacao>;
        _vinculos = resultados[1] as List<ObrigacaoEmpresa>;
        _carregando = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _erro = e.mensagem;
        _carregando = false;
      });
    }
  }

  void _avisar(String mensagem) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(mensagem)));
  }

  Future<void> _executar(Future<void> Function() acao) async {
    setState(() => _ocupado = true);
    try {
      try {
        await acao();
      } on ApiException catch (e) {
        _avisar(e.mensagem);
      }
      final vinculos = await widget.api.obrigacoesDaEmpresa(widget.empresa.id);
      if (!mounted) return;
      setState(() => _vinculos = vinculos);
    } on ApiException catch (e) {
      _avisar(e.mensagem);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  Future<void> _remover(String tipo) async {
    final confirmou = await showDialog<bool>(
      context: context,
      builder: (_) => ResponsiveDecisionDialog(
        titulo: 'Remover $tipo?',
        conteudo: Text('A obrigação $tipo deixa de ser gerada para ${widget.empresa.nome}.'),
        confirmar: 'Confirmar',
        cancelar: 'Voltar',
      ),
    );
    if (confirmou != true || !mounted) return;
    await _executar(() => widget.api.removerObrigacao(widget.empresa.id, tipo));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.empresa.nome)),
      body: Center(
        child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 1440), child: _corpo()),
      ),
    );
  }

  Widget _corpo() {
    if (_carregando) return const Center(child: CircularProgressIndicator());
    if (_erro != null) return ErroCarregamento(mensagem: _erro!, onTentarNovamente: _carregar);
    final empresa = widget.empresa;
    return LayoutBuilder(builder: (context, constraints) {
      final tablet = ResponsiveBreakpoints.ehTablet(constraints.maxWidth);
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('CNPJ ${formatarCnpj(empresa.cnpj)} · Regime: ${nomeRegime(empresa.regimeTributario)}'),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton.icon(
              icon: const Icon(Icons.assignment),
              label: const Text('Ver entregas desta empresa'),
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => EntregasPage(api: widget.api, empresaIdInicial: empresa.id)),
              ),
            ),
          ),
          const SizedBox(height: 8),
          if (tablet)
            GridView.count(
              crossAxisCount: 2,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              childAspectRatio: 1.35,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              children: [for (final tipo in _catalogo) _cartao(tipo)],
            )
          else
            for (final tipo in _catalogo) _cartao(tipo),
        ],
      );
    },
    );
  }

  Widget _cartao(TipoObrigacao tipo) {
    final vinculo = _vinculos.where((v) => v.tipo == tipo.tipo).firstOrNull;
    final compativel = tipo.regimesAceitos.contains(widget.empresa.regimeTributario);
    final estilo = Theme.of(context).textTheme;

    final Widget acao;
    final Widget detalhe;
    if (vinculo != null) {
      detalhe = Row(children: [
        const Icon(Icons.check_circle, color: Colors.green, size: 18),
        const SizedBox(width: 6),
        Text('Anexada em ${formatarData(vinculo.criadoEm)}'),
      ]);
      acao = OutlinedButton(onPressed: _ocupado ? null : () => _remover(tipo.tipo), child: const Text('Remover'));
    } else if (!compativel) {
      detalhe = Text(
        'Não se aplica ao regime ${nomeRegime(widget.empresa.regimeTributario)}',
        style: TextStyle(color: Colors.grey.shade700, fontStyle: FontStyle.italic),
      );
      acao = const SizedBox.shrink();
    } else {
      detalhe = Text(tipo.periodicidade == 'MENSAL' ? 'Mensal' : 'Anual');
      acao = FilledButton(
        onPressed: _ocupado ? null : () => _executar(() => widget.api.anexarObrigacao(widget.empresa.id, tipo.tipo)),
        child: const Text('Anexar'),
      );
    }

    return Card(
      color: vinculo == null && !compativel ? Colors.grey.shade200 : null,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: LayoutBuilder(builder: (context, constraints) {
          final informacoes = Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(tipo.tipo, style: estilo.titleMedium),
              Text(tipo.descricao),
              const SizedBox(height: 6),
              detalhe,
            ],
          );
          if (ResponsiveBreakpoints.ehCelular(constraints.maxWidth)) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [informacoes, Align(alignment: Alignment.centerRight, child: acao)],
            );
          }
          return Row(children: [Expanded(child: informacoes), acao]);
        }),
      ),
    );
  }
}
