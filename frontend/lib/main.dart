import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'api/api_client.dart';
import 'pages/empresas_page.dart';
import 'pages/entregas_page.dart';
import 'pages/gerar_entregas_page.dart';
import 'widgets/responsive_breakpoints.dart';

void main() {
  runApp(MeuApp(api: ApiClient()));
}

class MeuApp extends StatelessWidget {
  final ApiClient api;

  const MeuApp({super.key, required this.api});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Obrigações',
      theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
      locale: const Locale('pt', 'BR'),
      supportedLocales: const [Locale('pt', 'BR')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      home: _Casca(api: api),
    );
  }
}

class _Casca extends StatefulWidget {
  final ApiClient api;

  const _Casca({required this.api});

  @override
  State<_Casca> createState() => _CascaState();
}

class _CascaState extends State<_Casca> {
  int _indice = 0;

  Widget _tela() => switch (_indice) {
    0 => EmpresasPage(api: widget.api),
    1 => GerarEntregasPage(api: widget.api),
    _ => EntregasPage(api: widget.api),
  };

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        if (ResponsiveBreakpoints.ehCelular(constraints.maxWidth)) {
          return Scaffold(
            body: _tela(),
            bottomNavigationBar: NavigationBar(
              selectedIndex: _indice,
              onDestinationSelected: (i) => setState(() => _indice = i),
              destinations: const [
                NavigationDestination(
                  icon: Icon(Icons.business),
                  label: 'Empresas',
                ),
                NavigationDestination(
                  icon: Icon(Icons.playlist_add),
                  label: 'Gerar entregas',
                ),
                NavigationDestination(
                  icon: Icon(Icons.assignment),
                  label: 'Entregas',
                ),
              ],
            ),
          );
        }
        return Scaffold(
          body: Row(
            children: [
              NavigationRail(
                selectedIndex: _indice,
                labelType: NavigationRailLabelType.all,
                onDestinationSelected: (i) => setState(() => _indice = i),
                destinations: const [
                  NavigationRailDestination(
                    icon: Icon(Icons.business),
                    label: Text('Empresas'),
                  ),
                  NavigationRailDestination(
                    icon: Icon(Icons.playlist_add),
                    label: Text('Gerar entregas'),
                  ),
                  NavigationRailDestination(
                    icon: Icon(Icons.assignment),
                    label: Text('Entregas'),
                  ),
                ],
              ),
              const VerticalDivider(width: 1),
              Expanded(
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 1440),
                    child: _tela(),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
