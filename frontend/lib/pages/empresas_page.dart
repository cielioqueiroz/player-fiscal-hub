import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import '../models/empresa.dart';
import '../widgets/erro_carregamento.dart';
import '../widgets/formatadores.dart';
import '../widgets/responsive_breakpoints.dart';
import 'obrigacoes_empresa_page.dart';

const _regimes = ['SIMPLES', 'LUCRO_PRESUMIDO', 'LUCRO_REAL'];

class EmpresasPage extends StatefulWidget {
  final ApiClient api;

  const EmpresasPage({super.key, required this.api});

  @override
  State<EmpresasPage> createState() => _EmpresasPageState();
}

class _EmpresasPageState extends State<EmpresasPage> {
  List<Empresa> _empresas = [];
  String? _erro;
  bool _carregando = true;
  bool _incluirInativas = false;

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
      final empresas = await widget.api.listarEmpresas(incluirInativas: _incluirInativas);
      if (!mounted) return;
      setState(() {
        _empresas = empresas;
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

  Future<void> _nova() async {
    final cadastrou = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (_) => _EmpresaDialog(api: widget.api),
    );
    if (cadastrou == true && mounted) _carregar();
  }

  Future<void> _editar(Empresa empresa) async {
    final editou = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (_) => _EmpresaDialog(api: widget.api, empresa: empresa),
    );
    if (editou == true && mounted) _carregar();
  }

  Future<void> _alterarAtiva(Empresa empresa) async {
    final ativa = !empresa.ativa;
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(ativa ? 'Reativar empresa?' : 'Inativar empresa?'),
        content: Text(ativa
            ? 'A empresa voltará a participar da geração de entregas.'
            : 'A empresa deixará de participar de novas gerações. O histórico continuará disponível.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: Text(ativa ? 'Reativar' : 'Inativar')),
        ],
      ),
    );
    if (confirmado != true || !mounted) return;
    try {
      await widget.api.alterarAtivaEmpresa(empresa.id, ativa);
      if (mounted) _carregar();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.mensagem)));
    }
  }

  void _abrir(Empresa e) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => ObrigacoesEmpresaPage(api: widget.api, empresa: e)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Empresas')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _nova,
        icon: const Icon(Icons.add),
        label: const Text('Nova empresa'),
      ),
      body: _corpo(),
    );
  }

  Widget _corpo() {
    if (_carregando) return const Center(child: CircularProgressIndicator());
    if (_erro != null) return ErroCarregamento(mensagem: _erro!, onTentarNovamente: _carregar);
    return Column(
      children: [
        SwitchListTile(
          key: const Key('mostrarInativas'),
          title: const Text('Mostrar inativas'),
          value: _incluirInativas,
          onChanged: (valor) {
            setState(() => _incluirInativas = valor);
            _carregar();
          },
        ),
        Expanded(
          child: _empresas.isEmpty
              ? const Center(child: Text('Nenhuma empresa cadastrada. Use "Nova empresa".'))
              : LayoutBuilder(
                  builder: (context, constraints) {
                    if (constraints.maxWidth >= ResponsiveBreakpoints.celular &&
                        !ResponsiveBreakpoints.ehDesktop(constraints.maxWidth)) {
                      return GridView.builder(
                        padding: const EdgeInsets.fromLTRB(12, 8, 12, 88),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          crossAxisSpacing: 12,
                          mainAxisSpacing: 4,
                          childAspectRatio: 1.25,
                        ),
                        itemCount: _empresas.length,
                        itemBuilder: (context, index) => _cartaoEmpresa(_empresas[index], celular: false),
                      );
                    }
                    return ListView.separated(
                      padding: const EdgeInsets.only(bottom: 88),
                      itemCount: _empresas.length,
                      separatorBuilder: (_, _) => const Divider(height: 1),
                      itemBuilder: (context, index) => _cartaoEmpresa(
                        _empresas[index],
                        celular: ResponsiveBreakpoints.ehCelular(constraints.maxWidth),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _cartaoEmpresa(Empresa empresa, {required bool celular}) => Card(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ListTile(
          title: Text(empresa.nome, maxLines: 2, overflow: TextOverflow.ellipsis),
          subtitle: Text('${formatarCnpj(empresa.cnpj)}${empresa.ativa ? '' : ' • Inativa'}'),
          trailing: celular ? null : Chip(label: Text(nomeRegime(empresa.regimeTributario))),
          onTap: () => _abrir(empresa),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 0, 8, 8),
          child: Wrap(
            spacing: 4,
            runSpacing: 4,
            children: [
              if (celular) Chip(label: Text(nomeRegime(empresa.regimeTributario))),
              TextButton(onPressed: () => _editar(empresa), child: const Text('Editar')),
              TextButton(
                onPressed: () => _alterarAtiva(empresa),
                child: Text(empresa.ativa ? 'Inativar' : 'Reativar'),
              ),
            ],
          ),
        ),
      ],
    ),
  );
}

class _EmpresaDialog extends StatefulWidget {
  final ApiClient api;
  final Empresa? empresa;

  const _EmpresaDialog({required this.api, this.empresa});

  @override
  State<_EmpresaDialog> createState() => _EmpresaDialogState();
}

class _EmpresaDialogState extends State<_EmpresaDialog> {
  final _formulario = GlobalKey<FormState>();
  final _nome = TextEditingController();
  final _cnpj = TextEditingController();
  late String _regime;
  String? _erro;
  bool _salvando = false;

  @override
  void initState() {
    super.initState();
    _regime = widget.empresa?.regimeTributario ?? _regimes.first;
    _nome.text = widget.empresa?.nome ?? '';
    _cnpj.text = widget.empresa?.cnpj ?? '';
  }

  @override
  void dispose() {
    _nome.dispose();
    _cnpj.dispose();
    super.dispose();
  }

  String? _obrigatorio(String? valor, String mensagem) =>
      valor == null || valor.trim().isEmpty ? mensagem : null;

  Future<void> _salvar() async {
    if (!_formulario.currentState!.validate()) return;
    setState(() {
      _salvando = true;
      _erro = null;
    });
    try {
      final empresa = widget.empresa;
      if (empresa == null) {
        await widget.api.cadastrarEmpresa(_nome.text.trim(), _cnpj.text.trim(), _regime);
      } else {
        await widget.api.editarEmpresa(empresa.id, _nome.text.trim(), _cnpj.text.trim(), _regime);
      }
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _erro = e.mensagem;
        _salvando = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final titulo = widget.empresa == null ? 'Nova empresa' : 'Editar empresa';
    final botoes = [
      TextButton(
        onPressed: _salvando ? null : () => Navigator.pop(context, false),
        child: const Text('Voltar'),
      ),
      FilledButton(onPressed: _salvando ? null : _salvar, child: const Text('Salvar')),
    ];
    if (ResponsiveBreakpoints.ehCelular(MediaQuery.sizeOf(context).width)) {
      return Dialog.fullscreen(
        child: Scaffold(
          appBar: AppBar(title: Text(titulo)),
          body: SafeArea(child: SingleChildScrollView(padding: const EdgeInsets.all(20), child: _campos())),
          bottomNavigationBar: SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(mainAxisAlignment: MainAxisAlignment.end, children: botoes),
            ),
          ),
        ),
      );
    }
    return AlertDialog(title: Text(titulo), content: SizedBox(width: 420, child: _campos()), actions: botoes);
  }

  Widget _campos() => Form(
          key: _formulario,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                key: const Key('campoNome'),
                controller: _nome,
                decoration: const InputDecoration(labelText: 'Nome'),
                validator: (v) => _obrigatorio(v, 'Informe o nome'),
              ),
              TextFormField(
                key: const Key('campoCnpj'),
                controller: _cnpj,
                decoration: const InputDecoration(labelText: 'CNPJ'),
                validator: (v) => _obrigatorio(v, 'Informe o CNPJ'),
              ),
              DropdownButtonFormField<String>(
                key: const Key('campoRegime'),
                initialValue: _regime,
                decoration: const InputDecoration(labelText: 'Regime tributário'),
                items: [for (final r in _regimes) DropdownMenuItem(value: r, child: Text(nomeRegime(r)))],
                onChanged: (v) => setState(() => _regime = v ?? _regime),
              ),
              if (_erro != null)
                Padding(
                  padding: const EdgeInsets.only(top: 12),
                  child: Text(_erro!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
                ),
            ],
          ),
        );
}
