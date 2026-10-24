class Empresa {
  final int id;
  final String nome;
  final String cnpj;
  final String regimeTributario;

  const Empresa({
    required this.id,
    required this.nome,
    required this.cnpj,
    required this.regimeTributario,
  });

  factory Empresa.fromJson(Map<String, dynamic> json) => Empresa(
        id: json['id'] as int,
        nome: json['nome'] as String,
        cnpj: json['cnpj'] as String,
        regimeTributario: json['regimeTributario'] as String,
      );
}
