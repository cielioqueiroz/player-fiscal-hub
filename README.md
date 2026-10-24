# Gerenciador de Obrigações

Aplicação desenvolvida como desafio técnico para gerenciamento de obrigações de diferentes empresas.

O sistema permite cadastrar, editar e inativar empresas, vincular obrigações de um catálogo por regime tributário, gerar entregas por competência, acompanhar e alterar sua situação e anexar ou baixar documentos.

A interface Flutter Web consome uma API REST Java/Spring Boot, com persistência em MySQL e migrations Flyway. Docker Compose inicia os três serviços. A CI verifica os testes, o build web e a integração dos contêineres.

O tipo e o vencimento vêm das regras do catálogo. A atualização de uma entrega ocorre pelas transições de situação; não há exclusão de empresas ou entregas, nem reabertura de estados finais.

---

## Tecnologias utilizadas

### Backend
- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- Hibernate
- API REST

### Banco de dados
- MySQL 8.4
- Flyway

### Frontend
- Flutter
- Dart
- pacotes `http`, `file_picker` e `url_launcher`
- Flutter 3.44.0 / Dart 3.12 utilizados na validação e fixados na CI

### Infraestrutura
- Docker
- Docker Compose
- GitHub Actions: testes, análise estática, builds e verificação dos serviços

### Ferramentas de desenvolvimento
- VS Code
- Thunder Client
- Maven Wrapper

---

## Arquitetura

O fluxo principal da aplicação é:

```text
Flutter
   ↓
HTTP / JSON
   ↓
Spring Boot REST API
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
JPA / Hibernate
   ↓
MySQL
```

A aplicação foi dividida em camadas para separar responsabilidades.

### Controller

Responsável por receber as requisições HTTP e retornar as respostas da API.

Exemplos:

```text
GET /empresas
POST /entregas/gerar
PATCH /entregas/{id}/situacao
GET /documentos/{id}/download
```

### Service

Responsável pelas regras da aplicação. Nesta camada ficam, por exemplo:

- cadastrar empresas (com CNPJ validado);
- anexar e remover obrigações de uma empresa, respeitando o regime tributário;
- gerar as entregas de um mês de forma idempotente;
- mudar a situação de uma entrega (concluir, cancelar, marcar como atrasada);
- anexar documentos a uma entrega.

As regras de negócio ficam no backend. O Flutter só mostra o que a API informa (por exemplo, os botões de situação vêm de `transicoesPermitidas`).

### Repository

Responsável pela comunicação com o banco de dados através do Spring Data JPA.

```text
EmpresaRepository
EmpresaObrigacaoRepository
EntregaRepository
DocumentoRepository
```

---

## Estrutura principal do projeto

```text
desafio-player
│
├── docker-compose.yml
│
├── obrigacoes
│   ├── Dockerfile
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── java/com/desafio/obrigacoes
│       │   │   ├── config         # CORS e relógio (Clock)
│       │   │   ├── controller
│       │   │   ├── dto
│       │   │   ├── exception      # erros em { "erro": "..." }
│       │   │   ├── model
│       │   │   ├── repository
│       │   │   ├── service
│       │   │   └── validation     # validador de CNPJ
│       │   └── resources
│       │       ├── application.properties
│       │       └── db/migration   # V1, V2 e V3
│       └── test                   # unitários e integração (MySQL real)
│
└── frontend
    ├── lib
    │   ├── main.dart              # navegação principal
    │   ├── config.dart            # URL da API
    │   ├── api/                   # ApiClient (todas as chamadas HTTP)
    │   ├── models/
    │   ├── pages/                 # as 5 telas
    │   └── widgets/
    ├── test
    └── pubspec.yaml
```

---

## Modelos

### Empresa

Campos: `id`, `nome`, `cnpj`, `regimeTributario` (`SIMPLES`, `LUCRO_PRESUMIDO` ou `LUCRO_REAL`) e `ativa`. A listagem inclui empresas ativas por padrão; `incluirInativas=true` inclui também as inativas. Empresas inativas não participam de novas gerações e seu histórico continua consultável.

O CNPJ é validado no backend (dígitos verificadores), nos formatos numérico e alfanumérico, com ou sem máscara. CNPJ duplicado é recusado.

### Tipo de obrigação (catálogo)

Enum `TipoObrigacao`, exposto em `GET /catalogo-obrigacoes`:

| Tipo | Periodicidade | Regimes aceitos | Vencimento |
|---|---|---|---|
| `DAS` | mensal | Simples | dia 20 do mês seguinte à competência |
| `DCTF` | mensal | Lucro Presumido e Lucro Real | dia 15 do segundo mês seguinte |
| `ECD` | anual | todos | 31/05 do ano seguinte |

Uma empresa só pode anexar tipos compatíveis com o seu regime.

### Entrega

É a obrigação de uma empresa em uma competência (mês `2026-05` ou ano `2025`). Campos: `empresa`, `tipo`, `competencia`, `dataVencimento`, `situacao`, `dataConclusao`, `motivoCancelamento` e `versao`.

Só pode existir uma entrega por empresa, tipo e competência. Por isso gerar duas vezes o mesmo mês não duplica nada.

### Situação da entrega

```text
ABERTA     → ATRASADA, CONCLUIDA ou CANCELADA
ATRASADA   → CONCLUIDA ou CANCELADA
CONCLUIDA  → (final)
CANCELADA  → (final)
```

- `ATRASADA` só é permitida depois do vencimento.
- Concluir exige `dataConclusao` (não pode ser futura); cancelar exige `motivoCancelamento` (até 500 caracteres).
- Estados finais não aceitam alteração nem novos documentos.
- Duas pessoas alterando a mesma entrega ao mesmo tempo: a segunda recebe `409` (controle otimista por `@Version`).

### Documento

Arquivo anexado a uma entrega (guia paga, recibo etc.), guardado no banco em `LONGBLOB`. A listagem não carrega os bytes.

---

## Relacionamento entre entidades

```text
Empresa ──< EmpresaObrigacao   (quais tipos a empresa acompanha)
Empresa ──< Entrega ──< Documento
```

---

## Banco de dados

O banco utilizado é MySQL 8.4, no Docker Compose (container `obrigacoes-mysql`).

```text
Banco: obrigacoes
Porta: 3306
```

---

## Flyway

O Flyway versiona a estrutura do banco. Migrations:

```text
V1__criar_tabelas.sql                       empresa e obrigacao (modelo inicial)
V2__catalogo_obrigacoes_e_entregas.sql      regime tributário, vínculos, entregas e documentos
V3__remover_obrigacao_legada.sql            remove a tabela obrigacao do modelo antigo
V4__adicionar_ativa_empresa.sql             indica se a empresa está ativa
```

O Hibernate apenas valida o schema:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Dessa forma, a estrutura do banco é controlada pelo Flyway e não criada automaticamente pelo Hibernate.

---

## Endpoints

Erros sempre voltam no formato `{ "erro": "mensagem" }`.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/` | Verifica se a API está no ar |
| `GET` / `POST` | `/empresas` | Lista empresas ativas / cadastra empresa (CNPJ validado, com regime tributário) |
| `GET` | `/empresas?incluirInativas=true` | Lista empresas ativas e inativas |
| `GET` | `/empresas/{id}` | Busca empresa por ID |
| `PUT` | `/empresas/{id}` | Edita empresa; devolve `422` se obrigações anexadas forem incompatíveis com o novo regime |
| `PATCH` | `/empresas/{id}/ativa` | Ativa ou inativa uma empresa |
| `GET` | `/catalogo-obrigacoes` | Tipos de obrigação (DAS, DCTF, ECD) e regimes aceitos |
| `GET` / `POST` | `/empresas/{empresaId}/obrigacoes` | Lista / anexa obrigação à empresa |
| `DELETE` | `/empresas/{empresaId}/obrigacoes/{tipo}` | Remove o vínculo |
| `POST` | `/entregas/gerar` | Gera as entregas de um mês de referência (idempotente) |
| `GET` | `/entregas?empresaId=&tipo=&competencia=&situacao=` | Lista entregas com filtros |
| `GET` | `/entregas/{id}` | Detalhe da entrega |
| `PATCH` | `/entregas/{id}/situacao` | Conclui, cancela ou marca como atrasada |
| `GET` / `POST` | `/entregas/{id}/documentos` | Lista / anexa documento (`multipart`, campo `arquivo`, até 10 MB) |
| `GET` | `/documentos/{id}/download` | Baixa o documento |

Exemplos:

```text
POST /empresas
{ "nome": "Padaria Doce Pão", "cnpj": "11222333000181", "regimeTributario": "SIMPLES" }

POST /entregas/gerar
{ "mesReferencia": "2026-09" }

PATCH /entregas/1/situacao
{ "situacao": "CONCLUIDA", "dataConclusao": "2026-09-18" }

PATCH /empresas/1/ativa
{ "ativa": false }
```


---

## Docker

O projeto utiliza Docker para executar o banco, o backend e o frontend web.

O arquivo:

```text
docker-compose.yml
```

sobe os serviços:

```text
obrigacoes-mysql
obrigacoes-backend
obrigacoes-frontend
```

O backend utiliza um `Dockerfile` próprio localizado em:

```text
obrigacoes/Dockerfile
```

---

## Como executar o projeto

### Pré-requisitos

Para iniciar a aplicação completa, é necessário ter apenas Docker e Docker Compose. Não é necessário instalar Java, Maven ou Flutter na máquina para esse caminho.

```text
Docker
Docker Compose
```

Para conferir:

```bash
docker --version
docker compose version
```

---

### Subir banco, backend e frontend

Na pasta:

```text
raiz do projeto (a pasta que contém docker-compose.yml)
```

execute:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d --build
```

O MySQL exige uma senha para a conta administrativa `root` e outra para o usuário da aplicação (`obrigacoes`), usada pelo backend e pelo healthcheck. Elas não são login de usuário na interface. Como este é um ambiente de demonstração local, use valores descartáveis de teste; não precisa usar senhas pessoais ou de produção. Configure-os no `.env` antes da primeira inicialização. Preserve um `.env` já existente: mudar essas variáveis depois que o volume MySQL foi inicializado não altera as contas dentro do banco. O `.env` fica fora do Git; nunca envie esse arquivo ao repositório.

Depois confira:

```bash
docker compose ps
```

Os serviços devem aparecer como ativos:

```text
obrigacoes-backend
obrigacoes-mysql
obrigacoes-frontend
```

A interface fica em **http://localhost:3000** e a API em **http://localhost:8080**. As portas locais utilizadas são 3000, 8080 e 3306.

O MySQL possui um healthcheck que executa `SELECT 1` por TCP, com o usuário e o banco da aplicação. O backend depende de `service_healthy`, portanto aguarda essa consulta funcionar antes de iniciar. A API ainda pode levar alguns segundos para terminar a inicialização do Spring Boot.

Para conferir a API:

```text
http://localhost:8080
```

Exemplo:

```text
http://localhost:8080/empresas
```

Para parar:

```bash
docker compose down
```

O volume `mysql_data` preserva os cadastros entre reinicializações. `docker compose up` não limpa os dados nem reabre entregas. O parâmetro `--volumes` em `down` apaga o banco e não faz parte do procedimento normal de parada.

---

## Desenvolvimento local do Flutter (opcional)

Este caminho exige Flutter 3.44.0 ou versão compatível com Dart 3.12 instalado na máquina. O frontend web já é iniciado pelo Compose; os comandos abaixo servem para desenvolver com recarga automática.

Entre na pasta:

```bash
cd frontend
```

Instale as dependências:

```bash
flutter pub get
```

Execute no Chrome:

```bash
flutter run -d chrome
```

O Flutter faz uma requisição para:

```text
http://localhost:8080/empresas
```

e exibe as telas de empresas, obrigações, geração de entregas e documentos.

---

## Frontend implementado

Flutter com `StatefulWidget` e `setState` (sem gerenciador de estado externo). Em telas largas, o menu é lateral; em telas estreitas, a navegação fica na parte inferior. A tabela de entregas permite rolagem horizontal. Há três destinos e cinco telas:

| Tela | O que faz |
|---|---|
| Empresas | lista, cadastra, edita e inativa/reativa (mensagens do backend aparecem no formulário) |
| Obrigações da empresa | mostra os 3 tipos do catálogo: anexados, anexáveis e os que não se aplicam ao regime |
| Gerar entregas | escolhe mês e ano e gera as entregas (idempotente) |
| Entregas | lista com filtros (empresa, tipo, situação, competência) e destaque das vencidas |
| Detalhe da entrega | concluir, cancelar ou marcar como atrasada; anexar e baixar documentos |

Todas as chamadas HTTP passam por um único `ApiClient`, que traduz o corpo de erro `{ "erro": "..." }` em uma mensagem para a tela. Cada tela trata os estados de carregando, erro (com "Tentar novamente"), vazio e com dados.

Para apontar para outra API:

```bash
flutter build web --dart-define=API_URL=https://minha-api
```

---

## CORS

O Flutter Web chama a API direto do navegador. A liberação fica em `CorsConfig`, para origens `http://localhost:*` e `http://127.0.0.1:*`, com os métodos `GET`, `POST`, `PUT`, `PATCH`, `DELETE` e `OPTIONS`.

Em produção, o ideal seria restringir as origens ao domínio real do frontend.

---

## Testes

Backend, a partir da raiz do projeto (Linux/macOS):

```bash
cd obrigacoes
./mvnw -B -ntp verify
```

Frontend, em outro terminal aberto na raiz do projeto:

```bash
cd frontend
flutter pub get
flutter test
flutter analyze
```

Os testes de integração precisam do Docker Desktop ligado. Não usam H2, porque a geração de entregas usa SQL específico do MySQL.

No PowerShell do Windows, para testar o backend:

```powershell
Set-Location obrigacoes
.\mvnw.cmd -B -ntp verify
```

`verify` executa os testes unitários e de integração e gera o pacote Java. Testcontainers cria bancos temporários separados do banco utilizado pela aplicação.

## Integração contínua

O workflow `.github/workflows/ci.yml` é acionado em `push`, pull requests e manualmente pela aba Actions do GitHub.

| Job | Verificação | Artefato |
|---|---|---|
| Backend | Java 21, Maven `verify`, testes com MySQL real/Testcontainers | relatórios Surefire |
| Frontend | Flutter 3.44.0, `analyze`, testes e build web | frontend web compilado |
| Compose | build dos contêineres, espera do MySQL, consultas HTTP da API e frontend | logs em caso de falha |

Os artefatos ficam disponíveis na execução da CI por sete dias. O build Docker do backend usa `-DskipTests` porque os testes são executados no job próprio, que bloqueia o job de integração se falhar.

A CI passa a executar no GitHub quando estes arquivos forem enviados ao repositório. Não há publicação automática em servidor de produção.

---

## Decisões do projeto

### Desenvolvimento incremental

O projeto começou com uma lista em memória utilizando `ArrayList`.

Isso permitiu implementar e entender primeiro o CRUD da API.

Depois, a camada em memória foi substituída por:

```text
Spring Data JPA
+
Hibernate
+
MySQL
```

### Separação em camadas

Foi adotada a estrutura:

```text
Controller
↓
Service
↓
Repository
```

para separar responsabilidades e deixar o código mais organizado.

### Flyway para migrations

As tabelas não são criadas automaticamente pelo Hibernate.

O Flyway controla a evolução do banco através de migrations versionadas.

### Docker

O banco, o backend e o frontend (servido pelo nginx, em `http://localhost:3000`) podem ser iniciados juntos utilizando Docker Compose, reduzindo a necessidade de configurações manuais no ambiente.

---

## Documentos da entrega

Cada entrega aceita vários documentos (a guia DAS paga, o recibo da DCTF etc.), enquanto não estiver `CONCLUIDA` nem `CANCELADA`.

| Método | Rota | O que faz |
|---|---|---|
| `POST` | `/entregas/{id}/documentos` | envia um arquivo (`multipart/form-data`, campo `arquivo`) |
| `GET` | `/entregas/{id}/documentos` | lista os documentos, sem os bytes |
| `GET` | `/documentos/{id}/download` | baixa o arquivo |

Limite de **10 MB** por arquivo. Tipos aceitos: PDF, PNG, JPEG, XML e texto simples.

**Limitações conhecidas**

- Os arquivos ficam no banco, em `LONGBLOB`. Por isso o envio ocupa cerca de 20 a 30 MB de memória a cada arquivo de 10 MB (o `byte[]` mais o buffer do driver), e o download carrega o arquivo inteiro. Para a escala deste projeto isso é aceitável. Se virar problema, o caminho é enviar e baixar em *streaming*, ou guardar os arquivos fora do banco (disco ou armazenamento de objetos).
- O tipo do arquivo é o que o cliente declara; o conteúdo real não é inspecionado e não há antivírus.
- Não há exclusão de documentos.

---

## Escopo não implementado

Ficaram fora do escopo desta versão:

- autenticação e autorização;
- exclusão de documentos;
- job automático que marca entregas como `ATRASADA` (hoje é uma ação manual, permitida só depois do vencimento);
- envio e download de arquivos em *streaming*, ou armazenamento fora do banco;
- paginação das listas;
- publicação mobile em loja e validação completa em dispositivo físico;
- build e validação iOS em macOS/Xcode;
- deploy em ambiente de produção.

---

## Possíveis melhorias futuras

```text
- marcar entregas atrasadas automaticamente
- paginação e ordenação na lista de Entregas
- excluir documentos (bloqueado em estado final, como o envio)
- inspecionar o conteúdo real dos arquivos enviados
- restringir o CORS ao domínio de produção
```

---

## Resultado atual

A versão atual demonstra:

```text
Java / Spring Boot e API REST
MySQL, Spring Data JPA e Flyway (V1, V2, V3, V4)
Regras de negócio no backend: CNPJ, catálogo por regime, entregas idempotentes, transições de situação
Controle de concorrência (optimistic locking)
Upload e download de documentos
Testes de integração com MySQL real (Testcontainers)
Docker e Docker Compose (banco, backend e frontend)
Flutter Web responsivo, com navegação adaptada à largura da janela
CI com testes, análise estática, build web e verificação do Docker Compose
```
