import 'package:flutter/material.dart';

class SituacaoChip extends StatelessWidget {
  final String situacao;

  const SituacaoChip({super.key, required this.situacao});

  Color get _cor => switch (situacao) {
        'ABERTA' => Colors.blue.shade700,
        'ATRASADA' => Colors.orange.shade800,
        'CONCLUIDA' => Colors.green.shade700,
        'CANCELADA' => Colors.grey.shade600,
        _ => Colors.grey.shade600,
      };

  @override
  Widget build(BuildContext context) {
    return Chip(
      label: Text(situacao, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
      backgroundColor: _cor,
      side: BorderSide.none,
      visualDensity: VisualDensity.compact,
    );
  }
}
