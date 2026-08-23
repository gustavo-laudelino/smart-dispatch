# Smart Dispatch — Handoff

Última atualização: 2026-08-23

Este arquivo é o documento de **continuidade técnica** do projeto. A intenção é que um novo agente/PM consiga abrir o repositório, ler `CLAUDE.md` + este arquivo, e entender onde estamos e como chegamos aqui — sem depender do histórico de chat anterior. `CLAUDE.md` é "como trabalhar neste projeto" (permanente); este arquivo é "tudo que o próximo agente precisa saber sobre o estado atual" (muda com frequência).

---

## 1. Identidade e arquitetura do repositório

**Repositório:** `gustavo-laudelino/smart-dispatch`
**Branch principal:** `main`

**O que é:** plataforma de despacho técnico que ajuda a escolher o técnico mais adequado para cada atendimento, considerando distância, carga atual e distribuição recente de trabalho — objetivo central é reduzir quilômetros percorridos e equilibrar a carga entre técnicos.

**Arquitetura backend:** `controller` → `service` → `repository` (Spring Data JPA) → `model`, com `dto` para request/response e `config`/`exception` como camadas transversais. Grande parte das operações é isolada por `contratoId` (rotas no padrão `/contratos/{contratoId}/...`).

**Inventário atual (confirmado por listagem de arquivos, não por leitura método a método):**

- **Services (14):** `AuthService`, `AutorizacaoService`, `BaseOperacionalService`, `ChamadoService`, `ComentarioChamadoService`, `ContratoService`, `DistanciaService`, `HistoricoChamadoService`, `OrdemServicoService`, `SugestaoTecnicoService`, `TecnicoService`, `TokenService`, `UnidadeService`, `UsuarioService`.
- **Controllers (11):** `AuthController`, `BaseOperacionalController`, `ChamadoController`, `ChamadoFeedController`, `ComentarioChamadoController`, `ContratoController`, `HistoricoChamadoController`, `OrdemServicoController`, `TecnicoController`, `UnidadeController`, `UsuarioController`.
- **Repositories (9):** `BaseOperacionalRepository`, `ChamadoRepository`, `ComentarioChamadoRepository`, `ContratoRepository`, `HistoricoChamadoRepository`, `OrdemServicoRepository`, `TecnicoRepository`, `UnidadeRepository`, `UsuarioRepository`.
- **Configs (4):** `SecurityConfig`, `CorsConfig`, `JwtConfig`, `OpenApiConfig`.

---

## 2. Roadmap / fases já concluídas

- **ETAPA 1 — gestão de usuários:** concluída.
- **ETAPA 2 — auditoria/autorização:** concluída.
- **ETAPA 3 — regras de Chamado:** concluída.
- **ETAPA 4 — regras de Ordem de Serviço:** concluída.
- **ETAPA 5 — matriz final de segurança:** concluída.
- **ETAPA 6 — testes/build/Swagger/CI:** **EM ANDAMENTO.**
  - Testes unitários: **CONCLUÍDOS.**
  - Testes de integração:
    - **Fase A — infraestrutura: CONCLUÍDA.**
    - **Fase B — repositories: PRÓXIMO PASSO.**

A ETAPA 6 como um todo **não** está concluída — só a parte unitária e a infraestrutura de integração (Fase A).

---

## 3. Marco de testes unitários

```
164 execuções
0 failures
0 errors
BUILD SUCCESS
```

Sem `ApplicationContext` do Spring, sem banco, sem HTTP em nenhum dos 164.

**Services — 154 execuções em 14 classes:**

| Classe | Execuções |
|---|---|
| AuthServiceTest | 5 |
| AutorizacaoServiceTest | 23 |
| BaseOperacionalServiceTest | 6 |
| ChamadoServiceTest | 17 |
| ComentarioChamadoServiceTest | 10 |
| ContratoServiceTest | 6 |
| DistanciaServiceTest | 5 |
| HistoricoChamadoServiceTest | 7 |
| OrdemServicoServiceTest | 23 |
| SugestaoTecnicoServiceTest | 7 |
| TecnicoServiceTest | 6 |
| TokenServiceTest | 1 |
| UnidadeServiceTest | 6 |
| UsuarioServiceTest | 32 |

**Configs — 10 execuções em 4 classes:**

| Classe | Execuções |
|---|---|
| SecurityConfigTest | 6 |
| CorsConfigTest | 1 |
| JwtConfigTest | 2 |
| OpenApiConfigTest | 1 |

Isto **não** é "100% de cobertura de código". O marco é: todas as 14 classes de service possuem suíte própria, e as 4 classes de configuração com comportamento unitariamente relevante também estão protegidas. Controllers e repositories **não** têm teste unitário — deliberadamente (ver seção 24).

---

## 4. Modelo de trabalho adotado

**ChatGPT** — PM / analista / arquiteto / reviewer. Responsável por: analisar arquitetura, ler código atual, definir escopo, decidir quais comportamentos valem teste, entregar especificação fechada ao Claude, revisar o código real produzido, aprovar ou pedir correção, decidir a próxima unidade de trabalho.

**Claude Code** — executor técnico. Responsável por: ler somente o necessário quando a especificação já está fechada, implementar localmente, executar testes, reportar divergências, não ampliar escopo, não alterar produção para satisfazer teste, não commitar/dar push salvo ordem explícita.

**Usuário** — checkpoint humano. Responsável por: validar decisões, controlar commit/push, realizar validações manuais quando necessário.

**Fluxo usado com sucesso durante os 164 unitários:**

```
PM analisa → PM fecha especificação → Claude implementa → Claude executa teste específico
→ Claude executa a suíte completa → Claude reporta → arquivo real é revisado pelo PM
→ usuário commita → próxima unidade
```

Essa divisão foi uma decisão consciente para reduzir trabalho duplicado (o PM não paga o custo de reler tudo a cada rodada) e consumo de contexto (Claude não refaz mapeamento já fechado). Quando a especificação do PM já está fechada, Claude não deve refazer o mapeamento (ver `CLAUDE.md`, Modo A).

---

## 5. Usuário / Técnico / Perfis

**Perfis:** `ADMIN`, `CTO`, `TECNICO`, `TECNICO_INTERNO`.

- **ADMIN e CTO** — perfis gestores; não exigem `Tecnico` operacional.
- **TECNICO e TECNICO_INTERNO** — exigem `Usuario` + exatamente um vínculo `Tecnico` operacional.

**Transições (implementadas em `UsuarioService.atualizar`):**

- **técnico → gestor:** o `Tecnico` histórico fica inativo (`ativo=false`); o vínculo **não é apagado**; `Usuario` continua ativo, só o perfil muda.
- **gestor → técnico:** se não existe `Tecnico` histórico, cria um novo; se existe um histórico (mesmo inativo), **reutiliza** — não cria um segundo vínculo. `Tecnico.ativo` recebe o `Usuario.ativo` atual no momento da transição.
- **mesmo perfil → mesmo perfil:** idempotente — se técnico/técnico interno mantém o mesmo perfil e não informa novo contrato/base, o vínculo e a base existentes são preservados sem chamar `BaseOperacionalService`.

**Atividade — dois campos distintos, não intercambiáveis:**

- `Usuario.ativo` controla acesso/login (`AuthService.autenticar` bloqueia login com `FORBIDDEN "Usuário inativo."` se falso).
- `Tecnico.ativo` controla participação operacional (bloqueia atribuição de OS e check-in se falso).

---

## 6. Autenticação / JWT

**Endpoint:** `POST /auth/login` (`AuthController` → `AuthService.autenticar`).

**Comportamento confirmado no código (`AuthService`):**

- e-mail e senha obrigatórios — ausência/blank em qualquer um → `BAD_REQUEST` **"E-mail e senha são obrigatórios."**
- lookup usa `usuarioRepository.findByEmailIgnoreCase(email.trim())` — o e-mail é *trimado* mas **não** convertido para minúsculas manualmente (o `IgnoreCase` da query cuida disso);
- e-mail não encontrado → `UNAUTHORIZED` **"E-mail ou senha inválidos."** (mensagem genérica, não revela se o e-mail existe);
- usuário encontrado mas inativo → `FORBIDDEN` **"Usuário inativo."** (checado **antes** de comparar a senha);
- senha incorreta (`PasswordEncoder.matches`) → `UNAUTHORIZED` **"E-mail ou senha inválidos."** (mesma mensagem genérica do e-mail não encontrado — não diferencia as duas causas);
- sucesso → gera JWT via `TokenService` e devolve `LoginResponse` com token, tipo `"Bearer"`, tempo de expiração, dados do usuário e perfil.

**TokenService / JWT (confirmado em `JwtConfig` + `TokenService` + `application.properties`):**

- issuer: `smart-dispatch`;
- validade: `app.jwt.expiration-seconds=43200` segundos (12h);
- subject: e-mail do usuário;
- claims custom: `usuarioId`, `nome`, `perfil`;
- assinatura HMAC SHA-256 (`MacAlgorithm.HS256`);
- secret configurado via `app.jwt.secret=${JWT_SECRET}`, esperado em **Base64** (`JwtConfig.jwtSecretKey` faz `Base64.getDecoder().decode(secret)`);
- encoder/decoder via Nimbus (`NimbusJwtEncoder`/`NimbusJwtDecoder`).

**Decisão arquitetural crítica (repetida aqui de propósito, é a mais importante do projeto):** o JWT fornece **identidade**, não autorização. A claim `perfil` dentro do token **não** é fonte de verdade — ver seção 7.

---

## 7. SecurityConfig / Authorization

**`JwtAuthenticationConverter` (bean customizado em `SecurityConfig`):**

- lê a claim `usuarioId` do JWT;
- se a claim **não existir** → authorities vazias (nenhuma `ROLE_*` concedida, sem consultar o banco);
- se existir, consulta `UsuarioRepository.findById(usuarioId)`;
- se o usuário **não existir mais** no banco → authorities vazias;
- se existir → authority única `"ROLE_" + usuario.getPerfil().name()`, lida do **perfil atual no banco**, nunca da claim `perfil` do token.

**Consequência prática:** uma mudança de perfil no banco é refletida imediatamente, mesmo para um token antigo ainda dentro da validade — o token nunca "trava" o perfil.

**Descoberta feita durante os testes unitários:** a versão atual do Spring Security (compatível com Spring Boot 4.0.6) adiciona automaticamente uma authority de infraestrutura `FactorGrantedAuthority [authority=FACTOR_BEARER, ...]` a todo `JwtAuthenticationToken`, independente do conversor customizado. `FACTOR_BEARER` **não** tem prefixo `ROLE_`, não afeta nenhuma expressão `hasRole`/`hasAnyRole` do projeto, e não é uma authority de negócio — é só um marcador de "autenticado via bearer". `SecurityConfigTest` filtra `ROLE_*` explicitamente para isolar o comportamento controlado pela aplicação desse ruído de framework. Produção **não** foi alterada por causa disso.

**`SecurityFilterChain` atual (confirmado em `SecurityConfig`):**

- CSRF: disabled;
- CORS: habilitado via `.cors(withDefaults())` (delega para `CorsConfig`, ver seção 19);
- HTTP Basic: disabled;
- Form login: disabled;
- Sessão: `SessionCreationPolicy.STATELESS`;
- Resource Server OAuth2 com JWT, usando o `JwtAuthenticationConverter` customizado acima.

**Endpoints `permitAll()` atuais (lista exata do código):**

```
/auth/login
/swagger-ui/**
/swagger-ui.html
/v3/api-docs/**
/error
```

Todo o resto exige autenticação (`anyRequest().authenticated()`).

`@EnableMethodSecurity` está ativo na classe, habilitando `@PreAuthorize` nos controllers.

---

## 8. AutorizacaoService — regras importantes

Bean nomeado `"autorizacaoService"` (usado em expressões `@PreAuthorize` via `@autorizacaoService.metodo(...)`).

- **`podeAlterarStatusChamado(authentication, contratoId, request)`**: `ADMIN`/`CTO` têm bypass total (retorna `true` sem checar mais nada, mesmo para `FINALIZADO`); senão exige `tecnicoPertenceAoContrato`; se `request` ou `request.getStatus()` forem `null`, retorna `true` (a validação de payload é deixada para a service — ver `ChamadoService.atualizarStatus`, que rejeita `null` com `BAD_REQUEST`); só nega explicitamente quando o técnico tenta setar `FINALIZADO`.
- **`tecnicoPertenceAoContrato(authentication, contratoId)`**: `false` se a authentication não for `JwtAuthenticationToken` ou não tiver claim `usuarioId`; senão delega para `TecnicoRepository.existsByUsuarioIdAndBaseOperacionalContratoId`. **Não checa `Tecnico.ativo`** — só existência do vínculo com o contrato.
- **`tecnicoAtribuidoAOrdemServico(...)`**: mesmo padrão de early-return para authentication inválida; a query exclui apenas `ChamadoStatusNot(StatusChamado.FINALIZADO)` — um chamado `CANCELADO` continua permitindo a checagem de atribuição normalmente (comportamento atual, não corrigido, não assumido como bug).
- **`resolverContratoPermitido(authentication, contratoIdSolicitado)`**: gestor recebe o `contratoIdSolicitado` sem resolver vínculo; técnico sem vínculo nenhum → `FORBIDDEN "Usuário não possui vínculo com um contrato"`; técnico pedindo um contrato diferente do seu → `FORBIDDEN "Usuário não possui acesso a este contrato"`.

**Nuance de camadas a não esquecer:** o bloqueio "técnico não pode finalizar chamado" vive **só** em `AutorizacaoService` (usado no `@PreAuthorize` do controller). `ChamadoService.atualizarStatus`, isolado, **não** impede um técnico de setar `FINALIZADO` — ele só bloqueia alterações a um chamado que **já está** `FINALIZADO`. Isso já foi confirmado nos testes unitários (`ChamadoServiceTest`) e é comportamento atual, não bug — mas é fácil de interpretar errado se alguém testar `ChamadoService` isoladamente sem saber disso.

---

## 9. Chamado — contrato comportamental

- **Criação:** valida que a unidade pertence ao contrato; `numeroChamado` único **por contrato** (não global); status inicial sempre `ABERTO`; registra histórico `CHAMADO_CRIADO`.
- **Listagem:** escopada por contrato; ordenada por `dataAbertura` decrescente. Existe também um feed paginado (`listarFeed`) usado por `ChamadoFeedController`, com `contratoId` opcional (gestor pode ver todos) e direção configurável (qualquer valor diferente de `"asc"` cai em `desc`).
- **Busca/edição:** sempre escopadas por `contratoId` + `chamadoId` (`findByIdAndUnidadeContratoId`) — nunca `findById` simples.
- **Chamado `FINALIZADO`:** bloqueia edição e mudança de status para não-gestores (`FORBIDDEN`); gestor (`ADMIN`/`CTO`) pode editar e pode até "reabrir" mudando o status, o que zera `dataFinalizacao`.
- **Status automáticos** (`ABERTO`, `ATRIBUIDO`, `EM_ATENDIMENTO`) não podem ser setados manualmente via `PATCH /status` — `BAD_REQUEST` independente do papel do usuário. Eles são recalculados por `OrdemServicoService` conforme o estado das ordens de serviço vinculadas.
- **Idempotência:** setar o mesmo status já atual é um no-op — não salva, não registra histórico.
- **Atualização de dados:** só registra histórico (`DADOS_CHAMADO_ALTERADOS`) se algo realmente mudou (comparação campo a campo); número de chamado duplicado no mesmo contrato é `CONFLICT`.

---

## 10. Ordem de Serviço — contrato comportamental

- **Criação:** bloqueada se o chamado estiver `FINALIZADO` ou `CANCELADO` (`CONFLICT`, mensagens distintas); `numeroOrdemServico` tem unicidade **global**, não por contrato — decisão MVP conhecida, não corrigir sem decisão explícita; técnico é opcional na criação, mas se informado passa por `TecnicoService.buscarEntidadePorId`, que já barra técnico inativo; unidade de atendimento default é a unidade do próprio chamado se não informada.
- **Atualização:** livre antes do check-in (pode trocar técnico e unidade); **travada** depois do check-in — tentar mudar técnico ou unidade vira `CONFLICT`.
- **Check-in:** exige OS não encerrada, sem check-in já ativo, técnico atribuído e ativo. A busca por outra OS ativa do mesmo técnico (`findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull`) **não filtra por contrato** — um técnico pode ter uma OS ativa em outro contrato e isso bloqueia (ou, com confirmação explícita `encerrarCheckInAnterior=true`, encerra automaticamente essa OS anterior antes de iniciar a nova). Comportamento MVP conhecido, não é bug.
- Ao fazer check-in, o chamado vai direto para `EM_ATENDIMENTO` (set direto, **sem** passar pelo recálculo geral).
- **Check-out:** exige check-in existente e ainda não encerrado; ao concluir, recalcula o status do chamado (esse caminho **sim** passa pelo recálculo).
- **Prioridade do recálculo de status do chamado** (`recalcularStatusOperacionalDoChamado`, ordem fixa e testada):
  1. existe atendimento ativo (check-in sem check-out) → `EM_ATENDIMENTO`;
  2. existe OS atribuída a um técnico mas ainda não iniciada → `ATRIBUIDO`;
  3. existe OS sem técnico → `ABERTO`;
  4. nenhuma das anteriores → `AGUARDANDO_ANALISE`.

---

## 11. Tecnico / BaseOperacional / Unidade / Contrato

**Tecnico:** buscas/listagens sempre escopadas por base/contrato; relaciona `Usuario` (1:1) e `BaseOperacional` (N:1); `buscarEntidadePorId(contratoId, tecnicoId)` barra técnico inativo com `CONFLICT "Não é possível atribuir uma ordem de serviço a um técnico inativo"`.

**BaseOperacional:** pertence a um `Contrato`; criação associa o contrato buscado por `ContratoService`; busca/update/delete escopados por `contratoId` + `baseId`; **update preserva o contrato original** — mesmo que os "novos dados" enviados tragam outro contrato, ele é ignorado.

**Unidade:** mesmo padrão de `BaseOperacional` — pertence a um `Contrato`, escopo por `contratoId` + `unidadeId`, update preserva o contrato original.

**Contrato:** CRUD simples (criar, listar, buscar, atualizar, excluir). A implementação atual não possui validações adicionais de negócio para nome/cidade/SLA. Não adicionar essas validações incidentalmente; qualquer nova regra deve vir de tarefa/decisão explícita.

---

## 12. SugestaoTecnicoService — algoritmo de ranking

Registrado em detalhe porque é fácil de reescrever por acidente sem perceber que já está testado.

**Pesos atuais do score** (`PESO_*` em `SugestaoTecnicoService`):

```
score = (distânciaKm × 4.0) + (OS ativas × 2.0) + (atribuições hoje × 1.5) + (atendimentos últimos 15 dias × 1.0)
```

Quanto **menor** o score, melhor a indicação.

- **Janela histórica:** `DIAS_BALANCEAMENTO = 15` dias, calculada a partir de `LocalDateTime.now()`; "hoje" é `LocalDate.now().atStartOfDay()` até a meia-noite seguinte.
- **Distância:** se o técnico tem OS ativas (excluindo a própria OS alvo), a distância usa a **unidade de atendimento mais próxima** dentre essas OS ativas (a menor distância calculada por `DistanciaService.calcularEmKm`, não a base do técnico); só cai no **fallback para `BaseOperacional`** do técnico quando ele não tem nenhuma OS ativa.
- A **OS alvo** (a que está sendo atribuída) é sempre excluída tanto da contagem de "OS ativas" quanto da escolha de âncora de distância.
- **Ordenação:** por score bruto (não arredondado), crescente.
- **Nível de indicação** — calculado pela **diferença** entre o score do candidato e o score do **melhor** candidato (não pelo valor absoluto do score):
  - diferença `≤ 5.0` → `ALTA` (3 estrelas);
  - diferença `≤ 15.0` → `MODERADA` (2 estrelas);
  - diferença `> 15.0` → `LEVE` (1 estrela).
- **Arredondamento:** `pontuacao` e `distanciaKm` expostos no response são arredondados para 2 casas decimais (`Math.round(valor * 100) / 100.0`); a ordenação e o cálculo de diferença usam os valores **brutos**, não arredondados.

Não reescrever este algoritmo sem tarefa explícita — está coberto por 7 testes unitários que fixam esses pesos, janelas e limites.

---

## 13. Comentários e Histórico

**`ComentarioChamadoService`:**

- texto obrigatório, não-blank, e com no máximo 2000 caracteres **depois** do `trim()`;
- chamado `FINALIZADO` bloqueia novos comentários para não-gestores (`FORBIDDEN`);
- autor vem da claim `usuarioId` do JWT — exige `JwtAuthenticationToken` (`UNAUTHORIZED` senão);
- vínculo com uma Ordem de Serviço é opcional;
- listagem ordenada por `dataCriacao` ascendente, escopada pelo chamado.

**Hardening conhecido, não corrigido (dívida deliberada):** `ComentarioChamadoService.criar` extrai a claim `usuarioId` e chama `.longValue()` **sem checar null antes** — diferente de `AutorizacaoService` e `UsuarioService.alterarPropriaSenha`, que fazem esse null-check explícito. Um JWT válido mas sem a claim `usuarioId` causaria `NullPointerException` aqui, não um `UNAUTHORIZED` tratado. Identificado durante os testes unitários, deliberadamente não corrigido dentro de um lote de testes não relacionado a essa mudança.

**`HistoricoChamadoService`:**

- `registrar(...)` valida chamado, tipo de evento e descrição não vazios (lança `IllegalArgumentException`, não `ResponseStatusException` — é chamado internamente por outros services, não por um controller);
- vínculo com Ordem de Serviço é opcional;
- `dataEvento` é definida pela entidade via `@PrePersist` (JPA), **não** pelo service — por isso os testes unitários (repository mockado) não fazem asserção sobre esse campo;
- listagem escopada por `chamadoId` + `contratoId`, ordenada por `dataEvento` ascendente.

---

## 14. Inventário de repositories e foco de integração

Todos os 9 repositories atuais: `BaseOperacionalRepository`, `ChamadoRepository`, `ComentarioChamadoRepository`, `ContratoRepository`, `HistoricoChamadoRepository`, `OrdemServicoRepository`, `TecnicoRepository`, `UnidadeRepository`, `UsuarioRepository` — confirmado por listagem de arquivos.

**Por que `OrdemServicoRepository`, `ChamadoRepository` e `TecnicoRepository` são prioridade para integração:**

- concentram as queries derivadas mais complexas do projeto (múltiplos `And` encadeados, ex.: `existsByIdAndTecnicoUsuarioIdAndChamadoIdAndChamadoUnidadeContratoIdAndChamadoStatusNot`, `countByTecnicoIdAndChamadoUnidadeContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan`);
- concentram o escopo por contrato (`...UnidadeContratoId`, `...BaseOperacionalContratoId`) que é a base de todo o isolamento multi-tenant do sistema;
- alimentam diretamente `SugestaoTecnicoService` (janelas temporais) e `AutorizacaoService` (checagens de vínculo/atribuição);
- Mockito não prova que essas queries derivadas realmente geram o SQL esperado pelo Spring Data — só um teste contra banco real prova isso.

---

## 15. Inventário de controllers

Todos os 11 controllers atuais: `AuthController`, `BaseOperacionalController`, `ChamadoController`, `ChamadoFeedController`, `ComentarioChamadoController`, `ContratoController`, `HistoricoChamadoController`, `OrdemServicoController`, `TecnicoController`, `UnidadeController`, `UsuarioController` — confirmado por listagem de arquivos.

Nenhum controller documentado endpoint por endpoint aqui — isso é trabalho da fase de integração. **Nenhum controller tem teste ainda** (nem unitário nem de integração) — a validação de mapping HTTP, serialização, status code e `@PreAuthorize` está planejada exclusivamente para a fase de integração/web (ver seção 22-23).

---

## 16. Configuração da aplicação

Snapshot de `src/main/resources/application.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:5173}

app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-seconds=43200
```

`ddl-auto=update` — não há migrations (Flyway/Liquibase) no projeto atualmente. Essa configuração da aplicação normal **não foi alterada** pela Fase A.

**Profile de teste (`src/test/resources/application-test.properties`) — criado na Fase A:**

- datasource de teste usa exclusivamente `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` — nunca as variáveis `DB_*` da aplicação normal, justamente para não atingir `smart_dispatch` acidentalmente;
- default local (sem env var setada): `jdbc:postgresql://localhost:5432/smart_dispatch_test`;
- JWT de teste usa `TEST_JWT_SECRET`, separado do `JWT_SECRET` da aplicação normal;
- `spring.jpa.hibernate.ddl-auto=create-drop`, isolado neste profile — o `update` da aplicação normal permanece intocado.

---

## 17. POM / infraestrutura de teste atual

Dependências relevantes hoje (`pom.xml`): `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `postgresql` (runtime), `springdoc-openapi-starter-webmvc-ui` 3.0.3, `spring-boot-starter-test` (scope test — traz JUnit 5, Mockito, AssertJ) e `spring-boot-starter-data-jpa-test` (scope test).

`spring-boot-starter-data-jpa-test` foi adicionado na Fase A: no Spring Boot 4.0.6, a infraestrutura de `@DataJpaTest` **não** vem mais dentro de `spring-boot-starter-test`/`spring-boot-test-autoconfigure` — foi extraída para esse starter próprio, necessário para repository integration (Fase B).

**Confirmado que atualmente NÃO usamos:** H2, Testcontainers, `spring-security-test` explícito.

**Decisão vigente (Fase A, concluída):**

- repository integration usará PostgreSQL real;
- banco local dedicado: `smart_dispatch_test`;
- CI reutiliza o PostgreSQL 16 efêmero já existente no workflow;
- sem H2;
- sem Testcontainers nesta fase;
- profile/configuração de teste dedicado (`application-test.properties`, seção 16);
- `create-drop` somente no ambiente de teste;
- isolamento entre testes via transação/rollback do slice de teste (`@DataJpaTest`), como de costume no Spring.

---

## 18. CI — estado atual (pós Fase A)

Lido de `.github/workflows/ci.yml`.

**Job backend:**

- `ubuntu-latest`;
- serviço `postgres:16` com `POSTGRES_DB=smart_dispatch_test`, `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=postgres`, porta `5432`;
- env do **job** (disponível para o processo Maven): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`, `TEST_JWT_SECRET`, `CORS_ALLOWED_ORIGINS`;
- step: `mvn --batch-mode test`.

**Job frontend:** Node 22, `npm ci`, lint, build.

`DB_URL` também aponta, no CI, para o mesmo banco efêmero `smart_dispatch_test` — fallback seguro dentro daquele ambiente descartável, já que o job não roda a aplicação normal, só testes.

`JWT_SECRET` foi movido do `env` exclusivo do container Postgres (`jobs.backend.services.postgres.env`) para `jobs.backend.env`, ficando disponível ao processo Maven — o risco antes registrado nesta seção (variável inacessível ao contexto Spring) está resolvido.

---

## 19. CORS

- Origem default do frontend local: **`http://localhost:5173`** (não apenas `localhost:5173` — a URL completa com esquema é o valor real usado tanto no `application.properties` quanto no CI).
- `SecurityConfig` usa `.cors(withDefaults())`, delegando a configuração real para `CorsConfig` (via `WebMvcConfigurer.addCorsMappings`).
- `CorsConfig` aplica o mapping em `/**`.
- Métodos permitidos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- Headers permitidos: `*`.
- Origens vêm de `app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS` no ambiente), com split por vírgula, trim e descarte de entradas vazias — comportamento protegido por `CorsConfigTest`.

---

## 20. Dívidas / limitações / hardening conhecidos

1. **`ComentarioChamadoService`** — JWT válido sem claim `usuarioId` pode gerar `NullPointerException` em `usuarioId.longValue()` (seção 13). Confirmado no código, não corrigido.
2. **Revogação de JWT** — inativar um `Usuario` não revoga imediatamente um JWT já emitido; o acesso persiste até a expiração natural do token (até 12h). Decisão MVP.
3. **Senha inicial/reset `"cto"`** — hardcoded em `UsuarioService` para criação e reset de senha. Decisão MVP conhecida.
4. **Busca de OS ativa sem isolamento por contrato** (`OrdemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull`) — um técnico pode ter uma OS ativa "vazando" de outro contrato nesse fluxo específico de check-in. Decisão/limitação MVP, não bug.
5. **Autenticação definitiva do frontend** — ainda não implementada; segue no roadmap.

Nenhum item desta lista deve ser corrigido automaticamente — cada um exige decisão explícita antes de qualquer alteração de produção ou de CI.

---

## 21. Frontend — estado relevante

- React 19, TypeScript, Vite.
- Autenticação definitiva ainda é roadmap — um teste temporário com token hardcoded foi usado no passado e já foi revertido; não faz parte da implementação atual.
- Frontend **não** é foco da fase atual (backend/testes). Não alterar durante a integração do backend sem solicitação explícita.

---

## 22. Infraestrutura de integração — decisões da Fase A

O objetivo é testar o que mocks não conseguem provar: queries reais do Spring Data, `SecurityFilterChain` real, serialização HTTP, `@PreAuthorize` de fato aplicado.

**Decisões já aprovadas e implementadas:**

1. banco real: PostgreSQL;
2. banco local dedicado: `smart_dispatch_test`;
3. CI: PostgreSQL 16 efêmero já existente no GitHub Actions;
4. sem H2;
5. sem Testcontainers nesta fase;
6. profile/configuração dedicado: `application-test.properties`;
7. variáveis `TEST_*` separadas das variáveis da aplicação normal;
8. schema de teste: `ddl-auto=create-drop` somente no ambiente de teste;
9. repository integration deve usar `@DataJpaTest` com PostgreSQL real;
10. isolamento normal por transação/rollback do slice de teste;
11. `JWT_SECRET`/`TEST_JWT_SECRET` já disponíveis corretamente no CI;
12. `spring-boot-starter-data-jpa-test` já adicionado com scope test.

**Fase A concluída, revisada e commitada.** Não reabrir essas decisões durante a Fase B sem motivo técnico concreto ou tarefa explícita.

---

## 23. Ordem recomendada de integração

- **Fase A — infraestrutura: CONCLUÍDA.**
- **Fase B — repositories de maior risco:** `OrdemServicoRepository`, `ChamadoRepository`, `TecnicoRepository` (ver seção 14 para o porquê). Validar queries derivadas, escopo por contrato, filtros e janelas temporais contra banco real. Depois avaliar custo/benefício dos demais repositories.
- **Fase C — HTTP/Security:** `SecurityFilterChain` real, JWT, mappings, status HTTP, serialização, `@PreAuthorize`, matriz de acesso. Não criar `ControllerTest` trivial que só chama o método Java diretamente (isso não testa nada que valha a pena).
- **Fase D — fluxos críticos integrados:** login, acesso autenticado, chamado, ordem de serviço, check-in/check-out, autorização por perfil/contrato.
- **Fase E — fechamento:** suíte completa, CI verde, Swagger acessível, smoke tests necessários — só então considerar a ETAPA 6 encerrada.

---

## 24. Testes que não devem ser criados por impulso

- não reabrir a fase de testes unitários sem motivo concreto;
- não criar `ControllerTest` que só chama método Java e verifica delegação ao service;
- não mockar repository para "provar" uma query Spring Data — isso não prova nada;
- não testar getter/setter;
- não perseguir cobertura artificial;
- não adicionar dezenas de testes de repository sem priorização (ver seção 14);
- não mudar produção para facilitar teste;
- não adicionar H2, Testcontainers ou novas dependências de teste sem necessidade técnica e tarefa explícita — H2 e Testcontainers não fazem parte da estratégia atual aprovada (seção 22); `spring-security-test` ainda não é uma decisão definitiva para toda a fase futura, sua necessidade será analisada quando chegarmos à camada HTTP/security (Fase C).

---

## 25. Histórico de decisões importantes (por quê, não só o quê)

- Mocks foram usados para toda dependência **externa à unidade** sob teste — nunca para a própria classe testada.
- Quando uma especificação de teste parecia desatualizada frente ao comportamento real do código (ex.: `FACTOR_BEARER`, tipo do header `alg` em `JwtConfigTest`, data fixa expirada), o **comportamento real da produção prevaleceu** — o teste foi adaptado, a produção nunca foi tocada para "confirmar" a especificação.
- Ao descobrir `FACTOR_BEARER` (seção 7), a decisão foi documentar e filtrar no teste, não investigar/alterar a configuração de segurança — não havia indício de que fosse um problema real de autorização.
- `JwtConfigTest` usa encoder/decoder **reais** (Nimbus) para um teste de round-trip completo, em vez de mockar — o objetivo era provar que os beans realmente produzem um JWT válido e mutuamente compatível, não só que os métodos são chamados.
- Os testes unitários deliberadamente **não** testam se uma query Spring Data gera o SQL certo — isso é reservado para a fase de integração, onde um banco real pode provar isso.
- Controllers deliberadamente ficaram fora da fase unitária — testar `@PreAuthorize`, mapping HTTP e serialização por instanciação direta do controller (sem Spring) não prova nada sobre segurança real.

---

## 26. Padrão de trabalho do PM

Heurísticas de engenharia, não burocracia absoluta — aplicar julgamento técnico conforme complexidade e risco da tarefa.

1. **Fechar previamente todas as decisões técnicas que puder.** Evitar "preferencialmente", "se achar melhor", "pode usar X ou Y", "escolha uma abordagem" quando o PM já puder decidir. Objetivo: reduzir graus de liberdade do executor.
2. **Verificar se a tarefa está pronta para execução antes de enviar ao Claude Code.** Quando aplicável, a especificação deve definir: objetivo técnico; arquivo(s) exato(s); cenário(s); Arrange; Act; Assert; comandos de validação; resultado esperado; limites; condições de STOP; alterações permitidas; alterações proibidas. Se ainda houver decisão arquitetural importante aberta, permanecer em análise.
3. **O `git status` inicial é o BASELINE da rodada.** Distinguir alterações já existentes, alterações permitidas nesta rodada e alterações novas produzidas nesta rodada. A revisão final compara estado inicial versus estado final.
4. **Encerrar uma subfase em commit antes de iniciar outra responsabilidade**, quando ela já estiver implementada, revisada e aprovada. Objetivo: histórico limpo, rollback simples, evitar mistura desnecessária entre infraestrutura, produção, testes e documentação.
5. **Cada rodada deve ter um objetivo técnico principal.** Evitar testar várias responsabilidades incidentalmente em uma primeira implementação.
6. **Ao iniciar uma nova camada ou infraestrutura de testes, provar primeiro o menor cenário útil possível.** Só depois avançar para cenários complexos.
7. **Verificar se o teste realmente atravessa a camada que afirma provar.** Exemplos: teste unitário = unidade isolada + mocks das dependências; repository integration = Spring Data JPA + SQL real + banco real; HTTP/security = Spring MVC + `SecurityFilterChain` + autenticação/autorização reais.
8. **Evitar assertions baseadas em detalhes acidentais da implementação.** Priorizar comportamento observável. Em persistência, preferir validar id, campos, relacionamentos e resultado da query, em vez de exigir identidade da mesma instância Java quando isso não fizer parte do comportamento.
9. **Se o executor ainda precisar tomar uma decisão arquitetural que o PM poderia ter fechado previamente, a tarefa provavelmente ainda não está pronta.** Nesse caso, continuar em análise.
10. **O prompt para Claude deve ser suficientemente fechado** para que, no Modo A do `CLAUDE.md`, ele atue como executor e não como segundo arquiteto.

---

## 27. Convenções da fase de integração

Heurísticas, não regras mecânicas.

1. O primeiro teste de uma nova infraestrutura deve ser mínimo e controlado.
2. Repository integration deve provar PostgreSQL real.
3. Persistir apenas as entidades mínimas necessárias para satisfazer os mappings JPA do cenário.
4. Não criar cleanup manual quando transação/rollback do slice de teste já garantir isolamento adequado.
5. Quando o objetivo for provar leitura real do banco e isso acrescentar valor ao cenário, considerar `save` → `flush` → `clear` do persistence context → `query`, para reduzir a possibilidade de o first-level cache do Hibernate mascarar a leitura real. **Não** é regra obrigatória para todos os testes — usar somente quando acrescentar valor ao comportamento validado.
6. Começar por queries simples para provar a infraestrutura. Depois avançar para queries de maior risco.
7. Não transformar o primeiro teste de integração em busca por coverage.
8. Na futura camada HTTP/security, priorizar o fluxo real relevante ao Smart Dispatch. Para cenários críticos de segurança, deve ser possível validar: Bearer JWT → `JwtDecoder` → `SecurityFilterChain` → `usuarioId` → banco → perfil atual → `ROLE` → `@PreAuthorize`. `@WithMockUser` pode ser usado em cenários específicos, mas não deve substituir esse fluxo quando ele próprio for o comportamento sob teste.

---

## 28. Próximo passo exato

A Fase A está concluída. O próximo trabalho é iniciar a Fase B de forma controlada.

**Primeira rodada da Fase B:** criar SOMENTE `src/test/java/br/com/smartdispatch/repository/OrdemServicoRepositoryTest.java`.

**Primeiro objetivo:**

- provar que `@DataJpaTest` sobe;
- usar PostgreSQL real de teste;
- persistir entidades JPA reais;
- executar uma query real simples do `OrdemServicoRepository`;
- confirmar isolamento/rollback;
- não iniciar ainda queries complexas;
- não perseguir coverage.

A primeira classe/teste deverá ser especificada pelo PM antes da implementação, conforme as heurísticas das seções 26 e 27.

---

## 29. Bootstrap para próxima sessão

> Antes de implementar:
> 1. leia `CLAUDE.md`;
> 2. leia este `HANDOFF.md` inteiro;
> 3. confirme o marco de 164 testes unitários verdes;
> 4. confirme que a Fase A de integração está concluída;
> 5. confirme que a ETAPA 6 continua aberta;
> 6. identifique a Fase B como próxima fase;
> 7. respeite as heurísticas das seções 26 e 27;
> 8. não reabra decisões da Fase A sem divergência real no código;
> 9. verifique `git status`/baseline antes de qualquer rodada;
> 10. continue a partir do próximo trabalho ainda não concluído (seção 28).

---

## 30. Regra de manutenção deste arquivo

Este `HANDOFF.md` deve continuar sendo atualizado quando:

- uma fase terminar;
- uma decisão arquitetural relevante for tomada;
- uma dívida importante surgir;
- a estratégia de testes mudar;
- o contexto/chat for transferido.

Não atualizar a cada alteração pequena. Quando uma informação deixar de ser válida, **atualizar ou remover** — não acumular histórico obsoleto como se ainda fosse estado atual. Decisões históricas ainda relevantes podem permanecer marcadas explicitamente como "decisão histórica" em vez de removidas.
