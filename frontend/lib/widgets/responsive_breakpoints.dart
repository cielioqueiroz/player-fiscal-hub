class ResponsiveBreakpoints {
  static const celular = 600.0;
  static const desktop = 1024.0;

  static bool ehCelular(double largura) => largura < celular;

  static bool ehTablet(double largura) => largura >= celular && largura <= desktop;

  static bool ehDesktop(double largura) => largura > desktop;
}
