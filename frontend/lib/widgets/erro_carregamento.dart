import 'package:flutter/material.dart';

class ErroCarregamento extends StatelessWidget {
  final String mensagem;
  final VoidCallback onTentarNovamente;

  const ErroCarregamento({super.key, required this.mensagem, required this.onTentarNovamente});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline, size: 40, color: Theme.of(context).colorScheme.error),
            const SizedBox(height: 12),
            Text(mensagem, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            FilledButton.tonal(onPressed: onTentarNovamente, child: const Text('Tentar novamente')),
          ],
        ),
      ),
    );
  }
}
