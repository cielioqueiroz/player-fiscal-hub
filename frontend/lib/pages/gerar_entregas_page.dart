import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import '../models/resumo_geracao.dart';
import '../widgets/formatadores.dart';
import '../widgets/responsive_breakpoints.dart';

const _meses = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
];

class GerarEntregasPage extends StatefulWidget {
  final ApiClient api;

  const GerarEntregasPage({super.key, required this.api});

  @override
  State<GerarEntregasPage> createState() => _GerarEntregasPageState();
}

class _GerarEntregasPageState extends State<GerarEntregasPage> {
  final _hoje = DateTime.now();
  late int _mes = _hoje.month;
  late int _ano = _hoje.year;
  ResumoGeracao? _resumo;
  bool _gerando = false;

  Future<void> _gerar() async {
    setState(() {
      _gerando = true;
      _resumo = null;
    });
    try {
      final resumo = await widget.api.gerarEntregas(mesReferencia(_ano, _mes));
      if (!mounted) return;
      setState(() => _resumo = resumo);
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.mensagem)));
    } finally {
      if (mounted) setState(() => _gerando = false);
    }
  }

  Widget _seletor<T>(String rotulo, Key chave, T valor, List<DropdownMenuItem<T>> itens, ValueChanged<T?> aoMudar) {
    return SizedBox(
      width: 200,
      child: InputDecorator(
        decoration: InputDecoration(labelText: rotulo, border: const OutlineInputBorder(), isDense: true),
        child: DropdownButtonHideUnderline(
          child: DropdownButton<T>(key: chave, value: valor, isExpanded: true, isDense: true, items: itens, onChanged: aoMudar),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final resumo = _resumo;
    return Scaffold(
      appBar: AppBar(title: const Text('Gerar entregas')),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 1024),
          child: ListView(
            padding: EdgeInsets.all(
              ResponsiveBreakpoints.ehTablet(MediaQuery.sizeOf(context).width) ? 32 : 24,
            ),
            children: [
          Wrap(
            spacing: 16,
            runSpacing: 16,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              _seletor<int>(
                'Mês',
                const Key('selectMes'),
                _mes,
                [for (var i = 0; i < _meses.length; i++) DropdownMenuItem(value: i + 1, child: Text(_meses[i]))],
                (v) => setState(() {
                  _mes = v ?? _mes;
                  _resumo = null;
                }),
              ),
              _seletor<int>(
                'Ano',
                const Key('selectAno'),
                _ano,
                [for (var a = _hoje.year - 2; a <= _hoje.year + 1; a++) DropdownMenuItem(value: a, child: Text('$a'))],
                (v) => setState(() {
                  _ano = v ?? _ano;
                  _resumo = null;
                }),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Text(descricaoCompetencias(_ano, _mes)),
          const SizedBox(height: 16),
          Align(
            alignment: Alignment.centerLeft,
            child: FilledButton(onPressed: _gerando ? null : _gerar, child: const Text('Gerar')),
          ),
          if (resumo != null) ...[
            const SizedBox(height: 24),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${resumo.criadas} ${resumo.criadas == 1 ? 'entrega criada' : 'entregas criadas'} · '
                      '${resumo.jaExistentes} ${resumo.jaExistentes == 1 ? 'já existia' : 'já existiam'}',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    if (resumo.criadas == 0) ...[
                      const SizedBox(height: 8),
                      const Text('Nada novo para gerar: todas as entregas deste período já existem.'),
                    ],
                  ],
                ),
              ),
            ),
          ],
            ],
          ),
        ),
      ),
    );
  }
}
