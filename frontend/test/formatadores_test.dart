import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/widgets/formatadores.dart';

void main() {
  test('cnpj sem máscara ganha máscara', () => expect(formatarCnpj('11222333000181'), '11.222.333/0001-81'));
  test('cnpj já mascarado não muda', () => expect(formatarCnpj('11.222.333/0001-81'), '11.222.333/0001-81'));
  test('cnpj alfanumérico ganha máscara', () => expect(formatarCnpj('12ABC34501DE35'), '12.ABC.345/01DE-35'));
  test('cnpj com tamanho errado volta como veio', () => expect(formatarCnpj('123'), '123'));
  test('data ISO', () => expect(formatarData('2026-06-20'), '20/06/2026'));
  test('data e hora usa só a data', () => expect(formatarData('2026-06-20T14:03:11.123'), '20/06/2026'));
  test('245 KB', () => expect(formatarTamanho(250880), '245 KB'));
  test('menos de 1 KB', () => expect(formatarTamanho(512), '512 B'));
  test('MB com uma casa', () => expect(formatarTamanho(1572864), '1,5 MB'));
  test('dataIso com zeros', () => expect(dataIso(DateTime(2026, 6, 5)), '2026-06-05'));
  test('mesReferencia com zeros', () => expect(mesReferencia(2026, 9), '2026-09'));
  test(
      'texto das competências',
      () => expect(descricaoCompetencias(2026, 9),
          'Mensais (DAS, DCTF): competência 09/2026 · Anual (ECD): competência 2025'));
  test(
      'janeiro',
      () => expect(descricaoCompetencias(2026, 1),
          'Mensais (DAS, DCTF): competência 01/2026 · Anual (ECD): competência 2025'));
  test('nomes dos regimes', () {
    expect(nomeRegime('SIMPLES'), 'Simples Nacional');
    expect(nomeRegime('LUCRO_PRESUMIDO'), 'Lucro Presumido');
    expect(nomeRegime('LUCRO_REAL'), 'Lucro Real');
  });
}
