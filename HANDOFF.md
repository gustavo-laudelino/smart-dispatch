# Smart Dispatch — Handoff

Última atualização: 2026-08-31

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
- **ETAPA 6 — testes/build/Swagger/CI:** **CONCLUÍDA.**
  - Testes unitários: **CONCLUÍDOS.**
  - Testes de integração:
    - **Fase A — infraestrutura: CONCLUÍDA.**
    - **Fase B — repositories (JPA/Spring Data): CONCLUÍDA.**
    - **Fase C — HTTP/Security: CONCLUÍDA.**
    - **Fase D — fluxos críticos integrados: CONCLUÍDA.**
    - **Fase E — fechamento (OpenAPI, build final, auditoria de gaps): CONCLUÍDA.**

O ciclo planejado até a ETAPA 6 está **concluído por inteiro** (ver seção 27 para o marco final de testes e seções 24-26 para o detalhamento de cada fase). Próximas evoluções do projeto devem ser definidas em uma nova rodada de planejamento antes de qualquer implementação — este documento não antecipa uma ETAPA 7.

**Importante: a conclusão da ETAPA 6 não é o encerramento do Smart Dispatch.** O produto continua em desenvolvimento. O que a ETAPA 6 fecha é a estratégia de testes/build/validação do estado atual do backend — ainda haverá refinamento de regras de negócio, continuidade do frontend, e integração frontend/backend. Ver seção 32 para o detalhamento dessa distinção.

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

Isto **não** é "100% de cobertura de código". O marco é: todas as 14 classes de service possuem suíte própria, e as 4 classes de configuração com comportamento unitariamente relevante também estão protegidas. Controllers e repositories **não** têm teste unitário — deliberadamente (ver seção 28).

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

**Fase B — CONCLUÍDA.** Integração executada contra PostgreSQL real de teste (`smart_dispatch_test`), com `@DataJpaTest` configurado e funcional. Padrão de fixture usado em todas as classes: `persist` → `flush` → guardar IDs → `clear` → repository real; isolamento por rollback automático de cada teste (sem cleanup manual). Todos os 8 repositories com queries próprias estão cobertos: `OrdemServicoRepository`, `ChamadoRepository`, `TecnicoRepository`, `BaseOperacionalRepository`, `UnidadeRepository`, `UsuarioRepository`, `HistoricoChamadoRepository`, `ComentarioChamadoRepository`. `ContratoRepository` permanece sem classe de integração própria — não declara nenhuma query derivada, só herda `JpaRepository`. Suíte final no fechamento da fase: **231 testes, 0 failures, 0 errors**. Convenção adotada: um commit por classe de integração concluída.

---

## 15. Inventário de controllers

Todos os 11 controllers atuais: `AuthController`, `BaseOperacionalController`, `ChamadoController`, `ChamadoFeedController`, `ComentarioChamadoController`, `ContratoController`, `HistoricoChamadoController`, `OrdemServicoController`, `TecnicoController`, `UnidadeController`, `UsuarioController` — confirmado por listagem de arquivos.

Nenhum controller documentado endpoint por endpoint aqui. Todos os 11 controllers têm cobertura de integração HTTP real desde a Fase C (ver seção 24) — mapping HTTP, serialização, status code e `@PreAuthorize` validados via `MockMvc` contra `SecurityFilterChain`/JWT/banco reais, sem mocks de segurança. Controllers continuam **sem** teste unitário — decisão deliberada preservada (ver seção 28): validar `@PreAuthorize` e mapping HTTP por instanciação direta do controller, sem Spring, não prova nada sobre segurança real; a Fase C supriu exatamente essa lacuna com infraestrutura de integração de verdade.

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

Dependências relevantes hoje (`pom.xml`): `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `postgresql` (runtime), `springdoc-openapi-starter-webmvc-ui` 3.0.3, `spring-boot-starter-test` (scope test — traz JUnit 5, Mockito, AssertJ), `spring-boot-starter-data-jpa-test` (scope test) e `spring-boot-starter-webmvc-test` (scope test).

`spring-boot-starter-data-jpa-test` foi adicionado na Fase A: no Spring Boot 4.0.6, a infraestrutura de `@DataJpaTest` **não** vem mais dentro de `spring-boot-starter-test`/`spring-boot-test-autoconfigure` — foi extraída para esse starter próprio, necessário para repository integration (Fase B).

`spring-boot-starter-webmvc-test` foi adicionado na Fase C, pelo mesmo motivo: `@AutoConfigureMockMvc` também foi extraído de `spring-boot-starter-test`/`spring-boot-test-autoconfigure` nessa versão do Boot e passou a exigir esse starter próprio. Confirmado via `mvn dependency:tree` e inspeção direta dos jars antes de adicionar — sem esse starter, `@AutoConfigureMockMvc` simplesmente não existe no classpath.

**Confirmado que atualmente NÃO usamos:** H2, Testcontainers, `spring-security-test` explícito (decisão mantida — a Fase C não precisou dele; ver seção 28).

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
6. **OpenAPI / login sem override de security** — a operação `POST /auth/login` no documento `/v3/api-docs` gerado não recebe o `security: []` esperado da anotação `@Operation(security = {})` nesta versão do springdoc (o campo `security` fica ausente na operação, o que semanticamente herda o requisito global `bearerAuth`). Comportamento real de runtime está correto (`/auth/login` é `permitAll()` de fato); é uma imprecisão de documentação, não uma falha de segurança. **IMPORTANTE**, não bloqueante. Detalhes na seção 26.
7. **`npm audit` do frontend** — 3 vulnerabilidades foram reportadas pelo npm nas dependências do frontend durante a Fase E. Não bloquearam `npm ci`/`lint`/`build`; nenhuma correção foi feita. Dívida técnica registrada para avaliação futura — severidade real não afirmada sem rodar/registrar um `npm audit` detalhado.

Nenhum item desta lista deve ser corrigido automaticamente — cada um exige decisão explícita antes de qualquer alteração de produção ou de CI.

---

## 21. Frontend — estado relevante

- React 19, TypeScript, Vite.
- Autenticação definitiva ainda é roadmap — um teste temporário com token hardcoded foi usado no passado e já foi revertido; não faz parte da implementação atual.
- Frontend **não** foi alterado durante toda a ETAPA 6 (backend/testes). Continua fora de escopo até solicitação explícita.
- `npm ci`, `npm run lint` e `npm run build` verificados com sucesso na Fase E (seção 26) — consistente com o job `frontend` do CI (seção 18).

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

**Fase A concluída, revisada e commitada.** Não reabrir essas decisões sem motivo técnico concreto ou tarefa explícita.

---

## 23. Ordem recomendada de integração

- **Fase A — infraestrutura: CONCLUÍDA.**
- **Fase B — repositories: CONCLUÍDA.** Os 8 repositories com queries próprias (ver seção 14) têm classe de integração contra PostgreSQL real; `ContratoRepository` ficou de fora por não declarar query própria.
- **Fase C — HTTP/Security: CONCLUÍDA.** `SecurityFilterChain` real, JWT, mappings, status HTTP, serialização, `@PreAuthorize`, matriz de acesso — os 11 controllers cobertos. Detalhamento completo na seção 24.
- **Fase D — fluxos críticos integrados: CONCLUÍDA.** Login real, acesso autenticado, chamado, ordem de serviço, check-in/check-out, encerramento automático, alteração de perfil/vínculo, ciclo de senha — 6 jornadas de ponta a ponta. Detalhamento completo na seção 25.
- **Fase E — fechamento: CONCLUÍDA.** Suíte completa verde, `clean verify` verde, OpenAPI/Swagger validado via HTTP real, frontend (`npm ci`/`lint`/`build`) verificado, auditoria final de gaps sem achado crítico. Detalhamento completo na seção 26. **A ETAPA 6 está encerrada** (ver seção 27 para o marco final de testes).

---

## 24. Fase C — HTTP/Security (fechamento)

**CONCLUÍDA.** 109 testes de integração HTTP real (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + PostgreSQL real de teste), distribuídos em 11 classes:

| Classe | Testes |
|---|---|
| `HttpSecurityIntegrationTest` | 7 |
| `UsuarioHttpSecurityIntegrationTest` | 12 |
| `TecnicoHttpSecurityIntegrationTest` | 5 |
| `ChamadoHttpSecurityIntegrationTest` | 10 |
| `OrdemServicoHttpSecurityIntegrationTest` | 20 |
| `ContratoHttpSecurityIntegrationTest` | 11 |
| `BaseOperacionalHttpSecurityIntegrationTest` | 11 |
| `UnidadeHttpSecurityIntegrationTest` | 11 |
| `ChamadoFeedHttpSecurityIntegrationTest` | 8 |
| `ComentarioChamadoHttpSecurityIntegrationTest` | 8 |
| `HistoricoChamadoHttpSecurityIntegrationTest` | 6 |

Essa camada valida, com infraestrutura real (nenhum mock de segurança em nenhuma dessas classes):

- requisição HTTP real via `MockMvc`, contexto Spring real;
- `SecurityFilterChain` real;
- JWT real, gerado por `TokenService` real;
- `@PreAuthorize`/`AutorizacaoService` reais;
- perfil atual lido do banco (não da claim do token) — a regra da seção 7 provada em HTTP de ponta a ponta;
- isolamento operacional por contrato (técnico só acessa recursos do próprio contrato);
- a matriz 401/403/2xx dos 11 controllers.

Dependência necessária: `spring-boot-starter-webmvc-test` (ver seção 17).

---

## 25. Fase D — Fluxos críticos integrados (fechamento)

**CONCLUÍDA.** 6 testes ("jornadas") de ponta a ponta, em 2 classes: `DispatchCriticalFlowIntegrationTest` (3 jornadas) e `UsuarioCriticalFlowIntegrationTest` (3 jornadas).

Diferença em relação à Fase C: a Fase C prova a matriz de autorização endpoint a endpoint; a Fase D prova **sequências completas de negócio** atravessando várias camadas e várias requisições reais, com login real via `POST /auth/login` em cada passo autenticado — nenhuma jornada gera JWT diretamente via `TokenService`. O único acesso direto ao banco/`EntityManager` em cada jornada é o bootstrap mínimo do usuário ADMIN/gestor inicial (senha via `PasswordEncoder` real); toda mutação de estado subsequente passa pela API real.

**Jornadas protegidas:**

1. **Ciclo completo de despacho** — login → contrato → base → unidade → técnico → chamado → OS → atribuição → check-in → check-out → histórico → estado final do `Chamado` no banco (`AGUARDANDO_ANALISE`, conforme `recalcularStatusOperacionalDoChamado`, seção 10).
2. **Conflito de check-in / encerramento automático** — técnico com OS ativa tenta check-in em outra OS sem `encerrarCheckInAnterior` → `409 Conflict`; repete com `encerrarCheckInAnterior=true` → OS anterior encerrada automaticamente, nova OS ativa, histórico `ATENDIMENTO_FINALIZADO_AUTOMATICAMENTE` registrado.
3. **Isolamento operacional de ponta a ponta** — técnico do Contrato A só enxerga o próprio contrato (`GET /contratos`) e os próprios chamados (`GET /chamados`), provado por conteúdo (não só status); acesso ao Contrato B bloqueado com `403`.
4. **JWT antigo após alteração de perfil** — JWT emitido como `TECNICO` recebe `403` em `GET /usuarios`; ADMIN altera o usuário para `CTO`; **sem novo login**, o mesmo JWT antigo passa a receber `200 OK` — prova de ponta a ponta a decisão da seção 7 (identidade no token, autorização no banco).
5. **Ciclo completo de senha** — login com senha inicial (`"cto"`) → alteração própria via `PATCH /usuarios/me/senha` → login com senha antiga falha (`401`) → login com senha nova funciona (`200`) → reset administrativo → login com a senha personalizada falha (`401`) → login com a senha padrão de reset (`"cto"`) funciona (`200`).
6. **Ciclo gestor → técnico → gestor** — `CTO` sem `Tecnico` associado → alterado para `TECNICO` (vínculo criado) → alterado de volta para `CTO` (vínculo preservado, `Tecnico.ativo=false`, seção 5) → alterado novamente para `TECNICO` (mesmo vínculo histórico reutilizado, não um segundo `Tecnico` — provado via `TecnicoRepository.findByUsuarioId`, que é um `Optional` singular: se um segundo vínculo tivesse sido criado por engano, a própria chamada teria lançado erro de resultado não-único).

Em todas as 6 jornadas: `flush` + `clear` foram usados nos pontos em que uma mutação de entidade precisava ser relida via repository, para evitar falso positivo por first-level cache do Hibernate — mesmo padrão de risco identificado e corrigido durante a Fase C.

---

## 26. Fase E — Fechamento

**CONCLUÍDA.**

**OpenAPI/Swagger HTTP smoke** (`OpenApiHttpSmokeIntegrationTest`, 3 testes) — valida o documento `/v3/api-docs` real servido pela aplicação (não apenas a configuração do bean isolada, que já era coberta por `OpenApiConfigTest`):

- documento público (`200 OK` sem token), `info.title == "Smart Dispatch API"`, `info.version == "v1"`;
- `components.securitySchemes.bearerAuth` com `type=http`, `scheme=bearer`, `bearerFormat=JWT`;
- presença das rotas críticas no documento (`/auth/login`, `/usuarios`, `/contratos`, `/contratos/{contratoId}/chamados`, `/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico`).

**Build final:**

- `mvn clean test` — `BUILD SUCCESS`;
- `mvn clean verify` — `BUILD SUCCESS` (empacota o jar executável com sucesso).

**Frontend** (`frontend/`) — verificado localmente, consistente com o job `frontend` do CI (seção 18):

- `npm ci` — sucesso;
- `npm run lint` — sucesso;
- `npm run build` — sucesso.

**Auditoria final de gaps** — inventário cruzado de 14 services, 11 controllers, 9 repositories, 4 configs contra as camadas de teste (unitário, repository integration, HTTP/Security, fluxos críticos): **nenhum gap crítico adicional identificado**. Todos os componentes têm proteção relevante na(s) camada(s) apropriada(s).

**Pendências não bloqueantes registradas nesta rodada** (ver também seção 20, itens 6 e 7):

1. **OpenAPI / login** — a operação `POST /auth/login` (real, `permitAll()` no `SecurityConfig`) não recebe o override `security: []` esperado da anotação `@Operation(security = {})` nesta versão do springdoc — o campo `security` fica ausente na operação, o que pela semântica do OpenAPI 3.x significa "herda o requisito global `bearerAuth`". Consequência: o Swagger pode sugerir que `/auth/login` exige bearer, quando na prática não exige. Classificação: **IMPORTANTE** — não é falha de segurança, não bloqueia o sistema, não bloqueou a ETAPA 6.
2. **Frontend / `npm audit`** — 3 vulnerabilidades foram reportadas pelo npm nas dependências do frontend durante `npm ci`. Não bloquearam `npm ci`/`lint`/`build`. Dívida técnica registrada para avaliação futura.

---

## 27. Marco final de testes — ETAPA 6

```
Tests run: 349
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

**Distribuição dos 349 testes:**

| Camada | Execuções |
|---|---|
| Testes unitários (services + configs, seção 3) | 164 |
| Integração de repository/JPA (Fase B, seção 14) | 67 |
| HTTP/Security (Fase C, seção 24) | 109 |
| Fluxos críticos integrados (Fase D, seção 25) | 6 |
| OpenAPI/Swagger HTTP smoke (Fase E, seção 26) | 3 |
| **Total** | **349** |

Este é o baseline atual do backend. Números anteriores (164 → 231 → 265 → 340) registrados em seções específicas ao longo deste documento são marcos históricos de fases já fechadas, não o estado atual.

**Este é o baseline atual de qualidade do backend.** A suíte protege o comportamento implementado até este marco e deverá acompanhar futuras alterações funcionais e de regras de negócio — não é um número congelado. Mudanças futuras de regra podem exigir adaptação de testes existentes (não só criação de testes novos); isso é **esperado** em uma suíte de regressão, não um sinal de instabilidade ou de retrabalho indevido. Ver seção 32.

---

## 28. Testes que não devem ser criados por impulso

- não reabrir a fase de testes unitários sem motivo concreto;
- não criar `ControllerTest` que só chama método Java e verifica delegação ao service;
- não mockar repository para "provar" uma query Spring Data — isso não prova nada;
- não testar getter/setter;
- não perseguir cobertura artificial;
- não adicionar dezenas de testes de repository sem priorização (ver seção 14);
- não mudar produção para facilitar teste;
- não adicionar H2, Testcontainers ou novas dependências de teste sem necessidade técnica e tarefa explícita — H2 e Testcontainers não fazem parte da estratégia atual aprovada (seção 22); a Fase C confirmou que `spring-security-test` **não** foi necessário (MockMvc + `SecurityFilterChain` real bastaram) — não adicionar sem nova decisão explícita.

---

## 29. Histórico de decisões importantes (por quê, não só o quê)

- Mocks foram usados para toda dependência **externa à unidade** sob teste — nunca para a própria classe testada.
- Quando uma especificação de teste parecia desatualizada frente ao comportamento real do código (ex.: `FACTOR_BEARER`, tipo do header `alg` em `JwtConfigTest`, data fixa expirada), o **comportamento real da produção prevaleceu** — o teste foi adaptado, a produção nunca foi tocada para "confirmar" a especificação.
- Ao descobrir `FACTOR_BEARER` (seção 7), a decisão foi documentar e filtrar no teste, não investigar/alterar a configuração de segurança — não havia indício de que fosse um problema real de autorização.
- `JwtConfigTest` usa encoder/decoder **reais** (Nimbus) para um teste de round-trip completo, em vez de mockar — o objetivo era provar que os beans realmente produzem um JWT válido e mutuamente compatível, não só que os métodos são chamados.
- Os testes unitários deliberadamente **não** testam se uma query Spring Data gera o SQL certo — isso é reservado para a fase de integração, onde um banco real pode provar isso.
- Controllers deliberadamente ficaram fora da fase unitária — testar `@PreAuthorize`, mapping HTTP e serialização por instanciação direta do controller (sem Spring) não prova nada sobre segurança real.
- Na Fase C, descoberto que `@AutoConfigureMockMvc` também foi extraído de `spring-boot-starter-test` no Spring Boot 4.0.6 (mesmo padrão de modularização do `@DataJpaTest` na Fase A) — resolvido adicionando `spring-boot-starter-webmvc-test`, confirmado via inspeção direta dos jars antes de adicionar a dependência.
- Na Fase D, o risco de first-level cache mascarar uma releitura pós-mutação (já identificado numa correção pontual da Fase C) se repetiu nas jornadas que alteram perfil/vínculo ou estado de OS — resolvido com o mesmo padrão `flush` → `clear` → releitura via repository.
- Na Fase E, ao escrever o smoke test de OpenAPI, descoberto que `@Operation(security = {})` no `AuthController` não produz o override esperado no `/v3/api-docs` gerado (campo `security` fica ausente, não vazio) — a decisão foi **não afirmar no teste** um comportamento que o código atual não reflete (documentado como pendência IMPORTANTE, seção 20 item 6, em vez de forçar a asserção ou alterar produção).

---

## 30. Padrão de trabalho do PM

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

## 31. Convenções da fase de integração

Heurísticas, não regras mecânicas.

1. O primeiro teste de uma nova infraestrutura deve ser mínimo e controlado.
2. Repository integration deve provar PostgreSQL real.
3. Persistir apenas as entidades mínimas necessárias para satisfazer os mappings JPA do cenário.
4. Não criar cleanup manual quando transação/rollback do slice de teste já garantir isolamento adequado.
5. Quando o objetivo for provar leitura real do banco e isso acrescentar valor ao cenário, considerar `persist` → `flush` → guardar IDs → `clear` → `query`, para reduzir a possibilidade de o first-level cache do Hibernate mascarar a leitura real. **Não** é regra obrigatória para todos os testes — usar somente quando acrescentar valor ao comportamento validado.
6. Começar por queries simples para provar a infraestrutura. Depois avançar para queries de maior risco.
7. Não transformar o primeiro teste de integração em busca por coverage.
8. Na camada HTTP/security (Fase C, concluída), o fluxo real priorizado foi exatamente o relevante ao Smart Dispatch: Bearer JWT → `JwtDecoder` → `SecurityFilterChain` → `usuarioId` → banco → perfil atual → `ROLE` → `@PreAuthorize`. `@WithMockUser` não foi usado em nenhuma classe de integração deste projeto — o fluxo real acima é, ele próprio, o comportamento sob teste em todas as 109 + 6 execuções das Fases C e D.

---

## 32. Próximo passo exato

**O ciclo planejado até a ETAPA 6 está concluído.** Fases A, B, C, D e E — todas fechadas (seções 22, 14, 24, 25 e 26). Não há uma "próxima fase" pré-definida neste documento: a ETAPA 6 era o horizonte de planejamento vigente, e ele foi cumprido por inteiro.

**A conclusão da ETAPA 6 não representa o encerramento do Smart Dispatch.** Representa o fechamento da estratégia de testes, build e validação do estado atual do backend. O produto continua em desenvolvimento — ainda haverá:

- refinamento de regras de negócio no backend;
- continuidade do desenvolvimento do frontend;
- integração frontend/backend (autenticação definitiva do frontend, seção 21, é um exemplo já conhecido).

Os 349 testes (seção 27) são a baseline atual de regressão, não um teto definitivo. À medida que regras de negócio forem refinadas, é **esperado** que parte dos testes existentes precise ser adaptada — isso é o funcionamento normal de uma suíte de regressão acompanhando um produto vivo, não uma falha da suíte nem motivo para reabrir fases já fechadas sem necessidade.

Próximas evoluções do projeto (nova etapa, nova funcionalidade, refinamento de regra, autenticação definitiva do frontend, integração frontend/backend, ou qualquer outra iniciativa) devem ser definidas em uma **nova rodada de planejamento explícita** pelo PM antes de qualquer implementação — este documento deliberadamente não antecipa nem inventa um roadmap técnico além do que já foi decidido (não há ETAPA 7 aqui). As dívidas/pendências não bloqueantes conhecidas hoje estão na seção 20; nenhuma delas, por si só, define a próxima iniciativa.

**Pendência registrada, não bloqueante:** o estudo aprofundado de `@DataJpaTest`, `EntityManager`, `persist`/`flush`/`clear`, rollback e o fluxo JPA em geral segue como pendência planejada — foi adiado por decisão do usuário durante a Fase B e **continua pendente e não bloqueante** até hoje. Isso é estudo, não pendência de implementação; não impediu a conclusão de nenhuma fase da ETAPA 6.

---

## 33. Bootstrap para próxima sessão

> Antes de implementar qualquer coisa nova:
> 1. leia `CLAUDE.md`;
> 2. leia este `HANDOFF.md` inteiro;
> 3. confirme o marco de 349 testes verdes (164 unitários + 67 repository/JPA + 109 HTTP/Security + 6 fluxos críticos + 3 OpenAPI — seção 27);
> 4. confirme que a ETAPA 6 está **concluída** (Fases A a E, seção 2);
> 5. confirme que não existe uma "próxima fase" pré-definida — a próxima iniciativa depende de nova decisão explícita do PM (seção 32);
> 6. lembre que a conclusão da ETAPA 6 não é o encerramento do produto — o Smart Dispatch continua em desenvolvimento (refinamento de regras, frontend, integração frontend/backend), e os 349 testes são a baseline de regressão atual, não um teto (seção 32);
> 7. respeite as heurísticas das seções 30 e 31;
> 8. não reabra decisões de nenhuma fase já fechada sem divergência real no código ou tarefa explícita — mas esteja pronto para **adaptar** testes existentes quando uma mudança de regra de negócio legítima exigir, já que isso é esperado numa suíte de regressão (seção 27);
> 9. verifique `git status`/baseline antes de qualquer rodada;
> 10. se o pedido for uma nova funcionalidade/etapa, trate como Modo B (`CLAUDE.md`) — mapear, apresentar plano, aguardar aprovação antes de implementar.

---

## 34. Regra de manutenção deste arquivo

Este `HANDOFF.md` deve continuar sendo atualizado quando:

- uma fase terminar;
- uma decisão arquitetural relevante for tomada;
- uma dívida importante surgir;
- a estratégia de testes mudar;
- o contexto/chat for transferido.

Não atualizar a cada alteração pequena. Quando uma informação deixar de ser válida, **atualizar ou remover** — não acumular histórico obsoleto como se ainda fosse estado atual. Decisões históricas ainda relevantes podem permanecer marcadas explicitamente como "decisão histórica" em vez de removidas.
