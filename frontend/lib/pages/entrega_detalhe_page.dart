import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import '../models/documento.dart';
import '../models/entrega.dart';
import '../models/tipo_obrigacao.dart';
import '../widgets/erro_carregamento.dart';
import '../widgets/formatadores.dart';
import '../widgets/situacao_chip.dart';
import '../widgets/responsive_breakpoints.dart';
import '../widgets/responsive_decision_dialog.dart';

const _rotulos = {
  'CONCLUIDA': 'Concluir',
  'CANCELADA': 'Cancelar',
  'ATRASADA': 'Marcar como atrasada',
};

const _limiteBytes = 10 * 1024 * 1024;
const _extensoesPermitidas = ['pdf', 'png', 'jpg', 'jpeg', 'xml', 'txt'];

typedef ArquivoEscolhido = ({String nome, int tamanho, Future<Uint8List> Function() lerBytes});

Future<ArquivoEscolhido?> escolherArquivoDoDisco() async {
  final arquivo = await FilePicker.pickFile(type: FileType.custom, allowedExtensions: _extensoesPermitidas);
  if (arquivo == null) return null;
  return (nome: arquivo.name, tamanho: await arquivo.length() ?? 0, lerBytes: arquivo.readAsBytes);
}

class EntregaDetalhePage extends StatefulWidget {
  final ApiClient api;
  final int entregaId;

  final Future<ArquivoEscolhido?> Function()? escolherArquivo;

  const EntregaDetalhePage({super.key, required this.api, required this.entregaId, this.escolherArquivo});

  @override
  State<EntregaDetalhePage> createState() => _EntregaDetalhePageState();
}

class _EntregaDetalhePageState extends State<EntregaDetalhePage> {
  Entrega? _entrega;
  String? _descricaoTipo;
  List<Documento> _documentos = [];
  String? _erro;
  bool _carregando = true;
  bool _ocupado = false;
  bool _enviando = false;

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
        widget.api.buscarEntrega(widget.entregaId),
        widget.api.catalogo().catchError((_) => <TipoObrigacao>[]),
        widget.api.listarDocumentos(widget.entregaId),
      ]);
      if (!mounted) return;
      final entrega = resultados[0] as Entrega;
      final catalogo = resultados[1] as List<TipoObrigacao>;
      setState(() {
        _entrega = entrega;
        _documentos = resultados[2] as List<Documento>;
        _descricaoTipo = catalogo.where((t) => t.tipo == entrega.tipo).firstOrNull?.descricao;
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

  Future<void> _recarregarEntrega() async {
    try {
      final entrega = await widget.api.buscarEntrega(widget.entregaId);
      if (!mounted) return;
      setState(() => _entrega = entrega);
    } on ApiException catch (e) {
      _avisar(e.mensagem);
    }
  }

  void _avisar(String mensagem) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(mensagem)));
  }

  Future<void> _alterar(String destino) async {
    switch (destino) {
      case 'CONCLUIDA':
        final agora = DateTime.now();
        final hoje = DateTime(agora.year, agora.month, agora.day);
        final data = await showDatePicker(
          context: context,
          initialDate: hoje,
          firstDate: DateTime(2000),
          lastDate: hoje,
          helpText: 'Data de conclusão',
        );
        if (data == null || !mounted) return;
        await _enviar(destino, dataConclusao: dataIso(data));
      case 'CANCELADA':
        final motivo = await showDialog<String>(context: context, builder: (_) => const _MotivoDialog());
        if (motivo == null || !mounted) return;
        await _enviar(destino, motivoCancelamento: motivo);
      default:
        final confirmou = await showDialog<bool>(
          context: context,
          builder: (_) => const ResponsiveDecisionDialog(
            titulo: 'Marcar como atrasada?',
            conteudo: Text('A entrega passará para a situação atrasada.'),
            confirmar: 'Confirmar',
            cancelar: 'Voltar',
          ),
        );
        if (confirmou != true || !mounted) return;
        await _enviar(destino);
    }
  }

  Future<void> _enviar(String destino, {String? dataConclusao, String? motivoCancelamento}) async {
    setState(() => _ocupado = true);
    try {
      final nova = await widget.api.alterarSituacao(
        widget.entregaId,
        destino,
        dataConclusao: dataConclusao,
        motivoCancelamento: motivoCancelamento,
      );
      if (!mounted) return;
      setState(() => _entrega = nova);
    } on ApiException catch (e) {
      _avisar(e.mensagem);
      if (e.status == 409 || e.status == 422) await _recarregarEntrega();
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  Future<void> _anexar() async {
    final arquivo = await (widget.escolherArquivo ?? escolherArquivoDoDisco)();
    if (arquivo == null || !mounted) return;
    if (arquivo.tamanho > _limiteBytes) return _avisar('O arquivo excede o limite de 10 MB');

    setState(() => _enviando = true);
    try {
      final bytes = await arquivo.lerBytes();
      if (bytes.length > _limiteBytes) return _avisar('O arquivo excede o limite de 10 MB');
      await widget.api.anexarDocumento(widget.entregaId, arquivo.nome, bytes);
      final documentos = await widget.api.listarDocumentos(widget.entregaId);
      if (!mounted) return;
      setState(() => _documentos = documentos);
    } on ApiException catch (e) {
      _avisar(e.mensagem);
      if (e.status == 422) await _recarregarEntrega();
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Entrega #${widget.entregaId}')),
      body: Center(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 960), child: _corpo()),
      ),
    );
  }

  Widget _corpo() {
    if (_carregando) return const Center(child: CircularProgressIndicator());
    if (_erro != null) return ErroCarregamento(mensagem: _erro!, onTentarNovamente: _carregar);
    final entrega = _entrega!;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _cabecalho(entrega),
        const SizedBox(height: 16),
        _acoes(entrega),
        const SizedBox(height: 24),
        _secaoDocumentos(entrega),
      ],
    );
  }

  Widget _secaoDocumentos(Entrega e) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text('Documentos', style: Theme.of(context).textTheme.titleMedium),
            const Spacer(),
            if (!e.isFinal)
              _enviando
                  ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
                  : OutlinedButton.icon(
                      onPressed: _anexar,
                      icon: const Icon(Icons.attach_file),
                      label: const Text('Anexar documento'),
                    ),
          ],
        ),
        const SizedBox(height: 8),
        if (_documentos.isEmpty)
          const Text('Nenhum documento anexado.')
        else
          for (final d in _documentos)
            ListTile(
              leading: const Icon(Icons.description_outlined),
              title: Text(d.nomeArquivo),
              subtitle: Text('${formatarTamanho(d.tamanhoBytes)} · enviado em ${formatarData(d.enviadoEm)}'),
              trailing: TextButton.icon(
                onPressed: () => launchUrl(widget.api.urlDownload(d.id), webOnlyWindowName: '_blank'),
                icon: const Icon(Icons.download),
                label: const Text('Baixar'),
              ),
            ),
      ],
    );
  }

  Widget _cabecalho(Entrega e) {
    final estilo = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(e.empresaNome, style: estilo.titleLarge),
            Text('CNPJ ${formatarCnpj(e.empresaCnpj)}'),
            const SizedBox(height: 12),
            Text(_descricaoTipo == null ? e.tipo : '${e.tipo} · $_descricaoTipo', style: estilo.titleMedium),
            Text('Competência: ${e.competencia}'),
            Row(
              children: [
                Text('Vencimento: ${formatarData(e.dataVencimento)}'),
                if (e.vencida) ...[
                  const SizedBox(width: 8),
                  const Icon(Icons.warning_amber, color: Colors.red, size: 18),
                  const Text('Vencida', style: TextStyle(color: Colors.red)),
                ],
              ],
            ),
            const SizedBox(height: 8),
            Row(children: [const Text('Situação: '), SituacaoChip(situacao: e.situacao)]),
            if (e.dataConclusao != null) Text('Concluída em ${formatarData(e.dataConclusao!)}'),
            if (e.motivoCancelamento != null) Text('Motivo do cancelamento: ${e.motivoCancelamento}'),
          ],
        ),
      ),
    );
  }

  Widget _acoes(Entrega e) {
    if (e.transicoesPermitidas.isEmpty) {
      return const Text('Entrega finalizada: não pode mais ser alterada.');
    }
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final destino in e.transicoesPermitidas)
          FilledButton(
            onPressed: _ocupado ? null : () => _alterar(destino),
            child: Text(_rotulos[destino] ?? destino),
          ),
      ],
    );
  }
}

class _MotivoDialog extends StatefulWidget {
  const _MotivoDialog();

  @override
  State<_MotivoDialog> createState() => _MotivoDialogState();
}

class _MotivoDialogState extends State<_MotivoDialog> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final motivo = _controller.text.trim();
    if (ResponsiveBreakpoints.ehCelular(MediaQuery.sizeOf(context).width)) {
      return Dialog.fullscreen(
        child: Scaffold(
          appBar: AppBar(title: const Text('Cancelar entrega')),
          body: SafeArea(child: Padding(padding: const EdgeInsets.all(20), child: _campo(context))),
          bottomNavigationBar: SafeArea(child: Padding(
            padding: const EdgeInsets.all(12),
            child: Wrap(alignment: WrapAlignment.end, spacing: 8, children: _acoes(context, motivo)),
          )),
        ),
      );
    }
    return AlertDialog(
      title: const Text('Cancelar entrega'),
      content: _campo(context),
      actions: _acoes(context, motivo),
    );
  }

  Widget _campo(BuildContext context) => TextField(
        controller: _controller,
        maxLength: 500,
        autofocus: true,
        decoration: const InputDecoration(labelText: 'Motivo'),
        onChanged: (_) => setState(() {}),
      );

  List<Widget> _acoes(BuildContext context, String motivo) => [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Voltar')),
        FilledButton(
          onPressed: motivo.isEmpty ? null : () => Navigator.pop(context, motivo),
          child: const Text('Confirmar'),
        ),
      ];
  }
}
