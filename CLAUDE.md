# CLAUDE.md — Smart Dispatch

Instruções permanentes para agentes trabalhando neste repositório. Leia antes de qualquer alteração — e leia também `HANDOFF.md` para saber em que ponto o projeto está agora.

## 1. Projeto e stack

Smart Dispatch — sistema de despacho e gestão operacional de chamados técnicos.

- **Backend**: Java 21, Spring Boot 4.0.6 (via `spring-boot-starter-parent`), Maven
- Spring Web, Spring Data JPA, Spring Security + OAuth2 Resource Server (JWT self-issued via Nimbus/HMAC-SHA256), BCrypt para senhas
- PostgreSQL (runtime), springdoc-openapi/Swagger
- **Frontend**: React 19 + TypeScript + Vite (`frontend/`)
- **Arquitetura**: `controller` → `service` → `repository` (Spring Data JPA) → `model`, com `dto` para request/response e `config`/`exception` transversais. Escopo operacional por contrato via `contratoId` na maioria das rotas (`/contratos/{contratoId}/...`).

## 2. Papel do agente

O agente atua como executor técnico do projeto. Deve:

- preservar a arquitetura e os padrões já existentes;
- preferir solução simples, legível e compatível com o código ao redor;
- evitar abstração sem ganho concreto;
- não ampliar escopo por iniciativa própria;
- não "melhorar" regra de negócio fora da tarefa pedida.

## 3. Fontes de verdade

Quatro fontes coexistem, cada uma com um papel distinto — não há uma hierarquia simplista que substitua julgamento:

- **Código atual** é a fonte de verdade sobre o comportamento já implementado.
- **Especificação aprovada da tarefa** (do PM/usuário) é a fonte de verdade sobre a mudança que deve ser realizada agora.
- **`CLAUDE.md`** guarda regras permanentes, convenções e decisões que devem ser preservadas entre sessões.
- **`HANDOFF.md`** guarda o estado atual do projeto e o ponto de continuidade — não substitui a inspeção do código.

Se essas fontes entrarem em conflito entre si, **parar e reportar antes de editar qualquer coisa**. Nunca ajustar produção silenciosamente só para fazê-la coincidir com uma especificação que parece desatualizada.

## 4. Dois modos de trabalho

**Modo A — especificação fechada (pelo PM/usuário).** Quando o pedido já traz arquivos, cenários, comportamento esperado, limites e critério de validação:

- executar exatamente a especificação;
- não refazer um mapeamento que já foi feito;
- não propor testes ou abstrações adicionais além do pedido;
- ler somente os arquivos necessários para a tarefa;
- parar se encontrar divergência real entre especificação e código.

**Modo B — tarefa aberta.** Quando a tarefa ainda exige decisão arquitetural ou descoberta:

- inspecionar o código relevante primeiro;
- apresentar um plano curto;
- recomendar uma opção e explicar o motivo;
- aguardar aprovação antes de editar, exceto em tarefas triviais.

## 5. Condições obrigatórias de stop

Parar e reportar antes de prosseguir quando:

- especificação e código atual divergirem;
- um teste só puder passar alterando produção;
- surgir necessidade de dependência ou infraestrutura nova não aprovada;
- a solução exigir alteração fora do escopo pedido;
- houver ambiguidade em regra de negócio ou segurança;
- um teste revelar possível bug não previsto;
- houver alteração local que possa ser sobrescrita;
- for necessário rollback/revert/delete de trabalho existente;
- uma tarefa declarada unitária exigir Spring Context, banco real ou HTTP;
- qualquer decisão MVP já documentada precisar ser alterada.

Não contornar essas situações silenciosamente.

## 6. Processo de execução

Sempre preservar o `git status` inicial antes de qualquer alteração.

**Fluxo com especificação fechada:**
`git status` → ler arquivos necessários → implementar → executar teste específico → executar validação maior quando pedida → revisar diff → `git status` final → reportar.

**Fluxo com tarefa aberta:**
mapear → recomendar → aprovar → implementar → validar → revisar diff → reportar.

Mudanças grandes ou de alto risco: uma unidade lógica por vez. Mudanças pequenas e fortemente relacionadas podem ser agrupadas em um lote coerente — não fragmentar artificialmente só para manter "uma classe por vez" quando isso não agrega segurança.

## 7. Qualidade de código

- preservar o estilo já existente no arquivo/classe;
- nomes claros, sem abreviações obscuras;
- evitar duplicação sem criar abstração prematura;
- não criar comentários decorativos ou que apenas descrevem o código;
- comentar somente o "porquê" quando houver regra, invariante ou workaround não óbvio;
- não refatorar áreas vizinhas fora do escopo da tarefa.

## 8. Regras de domínio que devem ser preservadas

- `ADMIN` e `CTO` são perfis gestores (acesso amplo).
- `TECNICO` e `TECNICO_INTERNO` são perfis técnicos e devem possuir vínculo operacional com `Tecnico`.
- `Usuario.ativo` controla acesso ao sistema / possibilidade de login.
- `Tecnico.ativo` controla capacidade operacional do técnico (distinto de `Usuario.ativo`).
- Mudanças entre perfis gestores e técnicos possuem regras próprias já implementadas em `UsuarioService.atualizar` — preservar.
- A autorização efetiva utiliza o perfil atual do `Usuario` no banco (resolvido a cada requisição em `SecurityConfig`), não o perfil armazenado no JWT.
- Não confiar apenas no claim `perfil` do JWT para autorização — mudança de perfil deve refletir imediatamente, mesmo com token antigo ainda válido.
- Chamados e Ordens de Serviço possuem regras de negócio (status automáticos, bloqueio de edição quando finalizado, travas pós check-in, etc.) que devem ser preservadas conforme a implementação atual em `ChamadoService`/`OrdemServicoService`.
- `numeroOrdemServico` atualmente possui unicidade global (não por contrato). Não alterar sem decisão explícita.
- A busca atual de OS ativa do técnico (`OrdemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull`) não aplica isolamento por contrato onde já implementada. Não alterar nem assumir que seja bug sem decisão explícita.
- A senha inicial/reset `"cto"` é uma decisão atual do MVP e não deve ser alterada sem solicitação.
- A autenticação definitiva do frontend é uma etapa do roadmap. Não alterar o frontend sem solicitação explícita.
- `.cors(withDefaults())` em `SecurityConfig` é intencional (delega para `CorsConfig`) — não remover sem decisão explícita.
- Não alterar produção só para fazer um teste passar — se um teste parece exigir isso, é sinal de divergência real (ver seção 5).

## 9. Estratégia de testes

**Testes unitários**

- JUnit 5 + Mockito;
- sem `ApplicationContext` do Spring quando não necessário;
- sem banco de dados;
- sem HTTP;
- testar comportamento e regras de negócio, não perseguir cobertura artificial;
- não criar testes triviais de getters/setters;
- mockar as dependências externas à unidade sob teste;
- não alterar produção para fazer um teste passar;
- não reescrever testes existentes sem necessidade justificada.

Estado atual, contagens e fase de testes em andamento ficam em `HANDOFF.md`. Este arquivo registra apenas a estratégia permanente de testes.

**Testes de integração** (estratégia permanente para a próxima fase)

- repositories devem ser validados contra banco real/ambiente de teste — Mockito não prova que uma query derivada do Spring Data está correta;
- controllers devem ser priorizados em testes de camada web/security, não por instanciação direta apenas para verificar delegação ao service;
- `SecurityFilterChain`, mappings HTTP, serialização, `@PreAuthorize` e integração entre camadas pertencem à fase de integração, não à unitária;
- qualquer dependência ou infraestrutura nova de teste de integração exige aprovação explícita antes de ser adicionada ao `pom.xml`.

## 10. Comandos importantes (Windows/PowerShell)

```powershell
# Estado do repositório
git status

# Backend — rodar suíte de testes
.\mvnw.cmd test

# Backend — compilar
.\mvnw.cmd compile

# Frontend (a partir de frontend/)
npm.cmd run lint
npm.cmd run build
npm.cmd run dev
```

Não existem outros scripts de teste configurados no `package.json` do frontend além dos acima (`dev`, `build`, `lint`, `preview`).

## 11. Git

- Branch principal: `main`.
- Sempre executar `git status` antes de iniciar alterações e novamente ao final.
- Preservar alterações locais existentes (staged, unstaged ou untracked) — nunca descartar sem confirmar que não é trabalho em andamento.
- Não fazer `git add` automaticamente.
- Não commitar automaticamente.
- Não fazer push automaticamente.
- Por padrão, quem faz commit/push é o usuário — o agente só faz mediante solicitação explícita.
- Commits devem representar mudanças coesas (uma responsabilidade por commit).
- Mensagens de commit em inglês.

## 12. HANDOFF.md

`HANDOFF.md` existe para dar continuidade entre sessões e agentes diferentes. Toda sessão cujo trabalho dependa do estado atual do projeto deve lê-lo.

Deve ser atualizado:

- ao encerrar uma fase importante;
- quando houver uma decisão arquitetural relevante;
- antes de uma transferência de contexto/chat;
- quando o "próximo passo" mudar substancialmente.

Não atualizar a cada alteração pequena. `HANDOFF.md` nunca substitui a inspeção do código atual.

## 13. Áreas críticas

Alterações nas classes abaixo exigem atenção especial — concentram regra de negócio e/ou segurança sensível:

- `ChamadoService`
- `OrdemServicoService`
- `UsuarioService`
- `TecnicoService`
- `AutorizacaoService`
- `SecurityConfig`
