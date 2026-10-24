import 'package:flutter/material.dart';

import 'responsive_breakpoints.dart';

class ResponsiveDecisionDialog extends StatelessWidget {
  final String titulo;
  final Widget conteudo;
  final String confirmar;
  final String cancelar;

  const ResponsiveDecisionDialog({
    super.key,
    required this.titulo,
    required this.conteudo,
    required this.confirmar,
    this.cancelar = 'Cancelar',
  });

  @override
  Widget build(BuildContext context) {
    final acoes = [
      TextButton(onPressed: () => Navigator.pop(context, false), child: Text(cancelar)),
      FilledButton(onPressed: () => Navigator.pop(context, true), child: Text(confirmar)),
    ];
    if (ResponsiveBreakpoints.ehCelular(MediaQuery.sizeOf(context).width)) {
      return Dialog.fullscreen(
        child: Scaffold(
          appBar: AppBar(title: Text(titulo)),
          body: SafeArea(child: SingleChildScrollView(padding: const EdgeInsets.all(20), child: conteudo)),
          bottomNavigationBar: SafeArea(child: Padding(
            padding: const EdgeInsets.all(12),
            child: Wrap(alignment: WrapAlignment.end, spacing: 8, children: acoes),
          )),
        ),
      );
    }
    return AlertDialog(title: Text(titulo), content: conteudo, actions: acoes);
  }
}
