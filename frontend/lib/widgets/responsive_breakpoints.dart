class ResponsiveBreakpoints {
  static const celular = 600.0;
  static const tablet = 1024.0;

  static bool ehCelular(double largura) => largura < celular;
  static bool ehTablet(double largura) => largura >= celular && largura <= tablet;
  static bool ehDesktop(double largura) => largura > tablet;
}
