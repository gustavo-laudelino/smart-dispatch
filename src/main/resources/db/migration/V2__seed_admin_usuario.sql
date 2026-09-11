-- Seed de um usuário ADMIN inicial. Sem isto, um banco recém-criado não tem
-- como logar (criação de usuário via POST /usuarios já exige ADMIN autenticado
-- — não há endpoint público de cadastro). Senha "cto", mesma convenção já usada
-- para senha inicial/reset em UsuarioService (ver CLAUDE.md/HANDOFF.md).
--
-- Hash gerado com o BCryptPasswordEncoder real do projeto (SecurityConfig),
-- não inventado — confirmado via PasswordEncoder.matches("cto", hash) = true.
INSERT INTO usuarios (nome, email, perfil, senha, ativo)
VALUES (
    'Administrador',
    'admin@smartdispatch.local',
    'ADMIN',
    '$2a$10$inpgn293PZZjQAgPet81p.kOPVdV4RXUWGIdXxFmoOx08NMTPaMV.',
    true
);
