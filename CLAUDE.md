# CLAUDE.md — Smart Dispatch

Instruções permanentes para agentes trabalhando neste repositório. Leia antes de qualquer alteração.

## 1. Projeto e stack

Smart Dispatch — sistema de despacho e gestão operacional de chamados técnicos.

- **Backend**: Java 21, Spring Boot 4.0.6 (via `spring-boot-starter-parent`), Maven
- Spring Web, Spring Data JPA, Spring Security + OAuth2 Resource Server (JWT self-issued via Nimbus/HMAC-SHA256), BCrypt para senhas
- PostgreSQL (runtime), springdoc-openapi/Swagger
- **Frontend**: React 19 + TypeScript + Vite (`frontend/`)
- **Arquitetura**: `controller` → `service` → `repository` (Spring Data JPA) → `model`, com `dto` para request/response e `config`/`exception` transversais. Escopo operacional por contrato via `contratoId` na maioria das rotas (`/contratos/{contratoId}/...`).

## 2. Regras de atuação do agente

- O código atual é a fonte de verdade. O CLAUDE.md fornece contexto e regras de trabalho, mas antes de qualquer alteração o agente deve inspecionar a implementação atual relacionada à tarefa. Se houver divergência entre o CLAUDE.md e o código, reportar antes de agir.
- Não alterar regras de negócio sem autorização explícita.
- Não modificar código de produção apenas para fazer testes passarem.
- Não fazer commit ou push sem solicitação explícita.
- Sempre executar `git status` antes de iniciar alterações.
- Preservar alterações locais existentes (staged, unstaged ou untracked) — nunca descartar sem confirmar que não é trabalho em andamento.
- Não fazer rollback/revert de trabalho existente sem autorização.
- Trabalhar em mudanças pequenas, com uma responsabilidade por vez.
- Se encontrar comportamento aparentemente incorreto ou ambíguo, reportar antes de alterar — não corrigir por conta própria.
- Revisar o diff antes de considerar uma tarefa concluída.

## 3. Regras de domínio que devem ser preservadas

- `ADMIN` e `CTO` são perfis gestores (acesso amplo).
- `TECNICO` e `TECNICO_INTERNO` são perfis técnicos e devem possuir vínculo operacional com `Tecnico`.
- `Usuario.ativo` controla acesso ao sistema / possibilidade de login.
- `Tecnico.ativo` controla capacidade operacional do técnico (distinto de `Usuario.ativo`).
- Mudanças entre perfis gestores e técnicos possuem regras próprias já implementadas em `UsuarioService.atualizar` — preservar.
- A autorização efetiva utiliza o perfil atual do `Usuario` no banco (resolvido a cada requisição em `SecurityConfig`), não o perfil armazenado no JWT.
- Não confiar apenas no claim `perfil` do JWT para autorização — mudança de perfil deve refletir imediatamente, mesmo com token antigo ainda válido.
- Chamados e Ordens de Serviço possuem regras de negócio (status automáticos, bloqueio de edição quando finalizado, travas pós check-in, etc.) que devem ser preservadas conforme a implementação atual em `ChamadoService`/`OrdemServicoService`.
- `numeroOrdemServico` atualmente possui unicidade global (não por contrato). Não alterar sem decisão explícita.
- A busca atual de OS ativa do técnico (`OrdemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull`) não aplica isolamento por contrato. Não alterar nem assumir que seja bug sem decisão explícita.
- A senha inicial/reset `"cto"` é uma decisão atual do MVP e não deve ser alterada sem solicitação.
- A autenticação definitiva do frontend é uma etapa do roadmap. Não alterar o frontend sem solicitação explícita.

## 4. Estratégia de testes

- Estamos criando testes para comportamento **já existente** — os testes protegem regras atuais, não guiam refatoração.
- Testes unitários usam JUnit 5 + Mockito (`spring-boot-starter-test`).
- Testar comportamento e regras de negócio, não perseguir cobertura artificial.
- Não criar testes triviais de getters/setters apenas para aumentar cobertura.
- Uma classe de teste por vez.
- Antes de implementar, mapear os cenários relevantes.
- Aguardar aprovação dos cenários antes de implementar, quando a tarefa estiver sendo conduzida de forma supervisionada.
- Executar os testes depois das alterações.
- Não alterar produção para fazer teste passar.
- Se um teste revelar possível bug ou ambiguidade, parar e reportar — aguardar decisão antes de modificar produção.
- Preservar testes existentes e não reescrevê-los sem necessidade justificada.

Fluxo esperado:

```
mapear cenários → aprovar → implementar → executar testes → revisar diff → só então seguir para outra classe
```

## 5. Comandos importantes (Windows/PowerShell)

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

## 6. Git

- Branch principal: `main`.
- Não commitar automaticamente.
- Não fazer push automaticamente.
- Commits devem representar mudanças coesas (uma responsabilidade por commit).
- Mensagens de commit em inglês.

## 7. Áreas críticas

Alterações nas classes abaixo exigem atenção especial — concentram regra de negócio e/ou segurança sensível:

- `ChamadoService`
- `OrdemServicoService`
- `UsuarioService`
- `TecnicoService`
- `AutorizacaoService`
- `SecurityConfig`
