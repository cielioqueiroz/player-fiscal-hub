import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/config.dart';

void main() {
  test('usaUrlConfiguradaEmAndroid', () {
    expect(
      resolverApiBaseUrl(android: true, apiUrl: 'https://api.example.com'),
      'https://api.example.com',
    );
  });

  test('usaEmuladorAndroidSemUrlConfigurada', () {
    expect(resolverApiBaseUrl(android: true), 'http://10.0.2.2:8080');
  });

  test('outrasPlataformasUsamLocalhostSemUrlConfigurada', () {
    expect(resolverApiBaseUrl(android: false), 'http://localhost:8080');
  });
}
