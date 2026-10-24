class Empresa {
  final int id;
  final String nome;
  final String cnpj;
  final String regimeTributario;
  final bool ativa;

  const Empresa({
    required this.id,
    required this.nome,
    required this.cnpj,
    required this.regimeTributario,
    this.ativa = true,
  });

  factory Empresa.fromJson(Map<String, dynamic> json) => Empresa(
        id: json['id'] as int,
        nome: json['nome'] as String,
        cnpj: json['cnpj'] as String,
        regimeTributario: json['regimeTributario'] as String,
        ativa: json['ativa'] as bool? ?? true,
      );
}
