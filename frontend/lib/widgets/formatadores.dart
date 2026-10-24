String formatarCnpj(String cnpj) {
  final limpo = cnpj.replaceAll(RegExp(r'[^0-9A-Za-z]'), '').toUpperCase();
  if (limpo.length != 14) return cnpj;
  return '${limpo.substring(0, 2)}.${limpo.substring(2, 5)}.${limpo.substring(5, 8)}'
      '/${limpo.substring(8, 12)}-${limpo.substring(12)}';
}

String formatarData(String iso) {
  final partes = iso.substring(0, 10).split('-');
  return '${partes[2]}/${partes[1]}/${partes[0]}';
}

String formatarTamanho(int bytes) {
  if (bytes < 1024) return '$bytes B';
  if (bytes < 1024 * 1024) return '${(bytes / 1024).round()} KB';
  return '${(bytes / (1024 * 1024)).toStringAsFixed(1).replaceAll('.', ',')} MB';
}

String _dois(int n) => n.toString().padLeft(2, '0');

String dataIso(DateTime d) => '${d.year}-${_dois(d.month)}-${_dois(d.day)}';

String mesReferencia(int ano, int mes) => '$ano-${_dois(mes)}';

String descricaoCompetencias(int ano, int mes) =>
    'Mensais (DAS, DCTF): competência ${_dois(mes)}/$ano · Anual (ECD): competência ${ano - 1}';

String nomeRegime(String regime) => switch (regime) {
      'SIMPLES' => 'Simples Nacional',
      'LUCRO_PRESUMIDO' => 'Lucro Presumido',
      'LUCRO_REAL' => 'Lucro Real',
      _ => regime,
    };
