# Gerenciador de Obrigações

Aplicação para cadastrar empresas e suas obrigações fiscais, gerar entregas por competência e acompanhar prazos, documentos e situações. O frontend é Flutter para Web e Android; a API usa Java 21, Spring Boot e MySQL. O projeto sobe localmente com Docker Compose.

## Requisitos do desafio e localização

| Requisito | Onde está no projeto |
|---|---|
| Cadastro, edição, ativação e validação de empresas | `obrigacoes/src/main/java/com/desafio/obrigacoes/service/EmpresaService.java`, `controller/EmpresaController.java`; telas em `frontend/lib/pages/` |
| Obrigações por regime e geração idempotente | `EmpresaObrigacaoService`, `GeracaoEntregaService`, `EntregaRepository` |
| Situações, atualização automática e resumo | `AtualizacaoAtrasadasService`, `ResumoEntregasResponse`, `frontend/lib/pages/entregas_page.dart` |
| Paginação e ordenação | `EntregaController`, `PaginaResponse`, `frontend/lib/api/` |
| Documentos com validação de conteúdo | `DocumentoService`, `frontend/lib/pages/entrega_detalhe_page.dart` |
| Web responsiva e Android | `frontend/lib/widgets/responsive_breakpoints.dart`, `frontend/android/` |
| OpenAPI, healthcheck e demonstração | `OpenApiConfig`, `application.properties`, `DemoDataSeeder`, `docker-compose.yml` |
| Migrações do banco | `obrigacoes/src/main/resources/db/migration/` (V1 a V4) |
| Testes e builds automatizados | `obrigacoes/src/test/`, `frontend/test/`, `.github/workflows/ci.yml` |

## Tecnologias e arquitetura

- Backend: Java 21, Spring Boot, Spring MVC, Spring Data JPA, Hibernate, Flyway, Actuator e springdoc-openapi.
- Banco: MySQL 8.4. O Hibernate valida o esquema; o Flyway é responsável pelas migrations.
- Interface: Flutter 3.44.0 / Dart, com `http`, `file_picker` e `url_launcher`.
- Infraestrutura: Docker Compose, imagens multi-stage e GitHub Actions.

O Flutter chama a API REST em JSON. Controllers recebem as requisições, services aplicam regras de negócio, repositories usam JPA e MySQL persiste os dados. Arquivos são armazenados no banco como LONGBLOB. Os testes de integração usam MySQL via Testcontainers.

```text
obrigacoes/
  src/main/java/com/desafio/obrigacoes/{config,controller,dto,exception,model,repository,service,validation}
  src/main/resources/db/migration/       migrations Flyway V1–V4
  src/test/                               testes unitários e de integração
frontend/
  android/                                projeto Android nativo Flutter
  lib/{api,models,pages,widgets}/         cliente, modelos, telas e componentes
  test/                                   testes Flutter
.github/workflows/ci.yml                 validação automatizada
docker-compose.yml                       MySQL, API e frontend Web
```

## Regras principais

O catálogo define DAS (Simples Nacional, vencimento dia 20 do mês seguinte), DCTF (Lucro Presumido/Real, dia 15 do segundo mês seguinte) e ECD (todos os regimes, 31 de maio do ano seguinte). Uma empresa só pode vincular tipos compatíveis com seu regime. A geração é idempotente por empresa, tipo e competência e ignora empresas inativas.

As entregas percorrem `ABERTA → ATRASADA`, `ABERTA → CONCLUIDA/CANCELADA` e `ATRASADA → CONCLUIDA/CANCELADA`. Estados concluídos e cancelados são finais. O histórico de empresas inativas continua consultável.

## API

Todos os erros de regra retornam JSON no formato `{ "erro": "mensagem" }`. A interface OpenAPI está em [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html); a especificação JSON fica em `/v3/api-docs`. Healthcheck: `/actuator/health`.

| Método | Rota | Uso |
|---|---|---|
| `GET` | `/empresas?incluirInativas=false` | Lista empresas; por padrão somente ativas. `true` inclui inativas. |
| `GET` | `/empresas/{id}` | Consulta empresa. |
| `POST` | `/empresas` | Cadastra empresa. |
| `PUT` | `/empresas/{id}` | Edita empresa; valida CNPJ, duplicidade e compatibilidade dos vínculos. |
| `PATCH` | `/empresas/{id}/ativa` | Ativa ou inativa empresa. |
| `GET` | `/catalogo-obrigacoes` | Lista tipos e regimes aceitos. |
| `GET`, `POST` | `/empresas/{empresaId}/obrigacoes` | Consulta ou vincula obrigação. |
| `DELETE` | `/empresas/{empresaId}/obrigacoes/{tipo}` | Remove vínculo. |
| `POST` | `/entregas/gerar` | Gera por mês de referência (`AAAA-MM`). |
| `GET` | `/entregas` | Lista com filtros `empresaId`, `tipo`, `competencia`, `situacao`, além de paginação e ordenação. |
| `GET` | `/entregas/resumo?empresaId={id}` | Contagens gerais ou de uma empresa. |
| `GET` | `/entregas/{id}` | Consulta entrega. |
| `PATCH` | `/entregas/{id}/situacao` | Atualiza situação permitida. |
| `GET`, `POST` | `/entregas/{id}/documentos` | Lista ou anexa arquivo multipart no campo `arquivo` (máximo 10 MB). |
| `DELETE` | `/documentos/{id}` | Exclui arquivo; retorna `204`. Estados finais impedem a exclusão. |
| `GET` | `/documentos/{id}/download` | Baixa documento. |
| `GET` | `/actuator/health` | Estado de saúde da API. |

Exemplos de chamadas:

```http
POST /empresas
Content-Type: application/json

{"nome":"Padaria Aurora","cnpj":"11222333000181","regimeTributario":"SIMPLES"}
```

```http
PUT /empresas/1
Content-Type: application/json

{"nome":"Padaria Aurora","cnpj":"11222333000181","regimeTributario":"SIMPLES"}
```

```http
PATCH /empresas/1/ativa
Content-Type: application/json

{"ativa":false}
```

```http
POST /entregas/gerar
Content-Type: application/json

{"mesReferencia":"2026-09"}
```

```http
GET /entregas?page=0&size=20&sort=dataVencimento,asc&situacao=ABERTA
```

Resposta paginada; os parâmetros de consulta de página e tamanho usam os nomes Spring `page` e `size`:

```json
{"conteudo":[],"pagina":0,"tamanho":20,"totalElementos":0,"totalPaginas":0}
```

O padrão é 20 itens por página e o limite é 100. Campos ordenáveis: `dataVencimento`, `competencia`, `tipo`, `situacao` e `empresa` (nome). A ordenação por `id` é adicionada para desempate estável. Campo de ordenação não permitido retorna `400`.

```http
GET /entregas/resumo
```

```json
{"total":15,"abertas":6,"atrasadas":2,"concluidas":5,"canceladas":2,"venceEmAte7Dias":3,"vencidasSemBaixa":1}
```

Para atualizar situação, use por exemplo `PATCH /entregas/1/situacao` com `{"situacao":"CONCLUIDA","dataConclusao":"2026-09-18"}`. Cancelamento requer `motivoCancelamento`. Envie documentos com `multipart/form-data`, campo `arquivo`; o conteúdo é conferido pela assinatura/formato real além do MIME declarado.

## Executar localmente

### Docker Compose

Pré-requisitos: Docker e Docker Compose. Na raiz:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Configure `MYSQL_ROOT_PASSWORD` e `MYSQL_PASSWORD` em `.env` com senhas locais descartáveis. Opcionalmente defina `SPRING_PROFILES_ACTIVE=demo` para preencher um banco vazio com empresas de regimes diferentes e entregas em todas as situações. O perfil demo não altera banco que já contém dados. Depois rode:

```bash
docker compose up -d --build
docker compose ps
```

Frontend: [http://localhost:3000](http://localhost:3000). API: [http://localhost:8080](http://localhost:8080). Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). O backend aguarda o healthcheck do MySQL e o frontend aguarda o healthcheck do backend.

Para parar sem apagar dados:

```bash
docker compose down
```

O volume `mysql_data` mantém o banco. Não use `docker compose down --volumes` na parada normal, pois isso remove os dados.

### Flutter Web

Com Flutter 3.44.0 e Dart 3.12:

```bash
cd frontend
flutter pub get
flutter run -d chrome
```

Por padrão a API é `http://localhost:8080`. Para usar outra URL: `flutter run -d chrome --dart-define=API_URL=https://api.exemplo.com` ou `flutter build web --release --dart-define=API_URL=https://api.exemplo.com`.

### Android

Abra `frontend/` no Android Studio ou execute `flutter run` com um emulador Android ativo. O emulador padrão acessa a API local em `http://10.0.2.2:8080`; passe `--dart-define=API_URL=https://...` para outra API. HTTP sem TLS é permitido somente no build debug; o build release exige HTTPS.

O job Android da CI gera `app-debug.apk` e publica o artefato `frontend-android-debug` por sete dias. Baixe o APK na execução do workflow em GitHub Actions e instale com:

```bash
adb install app-debug.apk
```

O build do APK valida a compilação; não executa nem representa teste em emulador.

## Interface responsiva

Os breakpoints centralizados em `frontend/lib/widgets/responsive_breakpoints.dart` são: celular abaixo de 600 px, tablet de 600 a 1024 px e desktop acima de 1024 px. Celulares usam navegação inferior, cards de entregas e filtros em painel inferior; formulários e diálogos se adaptam à tela. Tablets organizam conteúdo em duas colunas quando adequado e mantêm filtros visíveis. Desktops usam navegação lateral, tabela completa, controles de página e conteúdo com largura máxima.

## Atualização automática de entregas atrasadas

Ao concluir a inicialização, e diariamente às 00:05 no fuso `America/Sao_Paulo`, a aplicação executa uma atualização em lote: entregas `ABERTA` com vencimento anterior à data atual passam para `ATRASADA`. A data vem do `Clock` injetado e o log informa quantos registros foram alterados. Para desligar, defina `OBRIGACOES_ATUALIZACAO_ATRASADAS_HABILITADA=false` ou a propriedade `obrigacoes.atualizacao-atrasadas.habilitada=false`. A ação manual de mudança de situação continua disponível. Para múltiplas instâncias, ainda é necessário um lock distribuído.

## Docker e cache de build

O backend é compilado em uma etapa Maven/Temurin 21 e executa em `eclipse-temurin:21-jre-alpine`. O frontend é compilado em uma etapa Ubuntu com Flutter e servido pela imagem final `nginx:alpine`. Maven, JDK, Flutter e fontes permanecem nas etapas de build, fora das imagens finais de execução. O cache local do BuildKit acelera builds e é separado do conteúdo dessas imagens. Tamanhos de imagem não são informados porque não foram medidos nesta documentação.

## Testes e CI

Backend, a partir da pasta `obrigacoes`:

```bash
./mvnw -B -ntp verify
```

Os testes de integração usam Testcontainers e precisam de Docker. Flutter, a partir de `frontend`:

```bash
flutter pub get
flutter analyze
flutter test
flutter build web --release
flutter build apk --debug
```

A CI executa testes Java com MySQL, análise e testes Flutter, build Web e APK Android debug, e inicia o Compose verificando healthchecks, API, OpenAPI e frontend.

## Escopo ainda não implementado

- Autenticação e autorização de usuários.
- Deploy e configuração para produção.
- Build e validação iOS, que exigem macOS/Xcode, e publicação nas lojas.
- Armazenamento fora do banco e transferência de arquivos em streaming.
- Restrição de CORS ao domínio de produção.
- Análise antivírus nos arquivos recebidos.
- Lock distribuído para coordenar o agendamento de atrasadas entre múltiplas instâncias.
