-- Dados de demonstração. Sem isto, um clone novo consegue logar (V2) mas não
-- consegue demonstrar o fluxo principal pela UI: criar Chamado exige uma
-- Unidade existente, criar OS exige um Técnico existente, e não há tela no
-- frontend para cadastrar Contrato/Unidade/Base/Técnico (ver README) — só
-- pela API/Swagger. Este seed cobre o mínimo para o fluxo completo (login →
-- Chamado → OS → sugestão de técnico → check-in/check-out) funcionar direto
-- pela UI, sem passar pelo Swagger primeiro.
--
-- Coordenadas reais (região da Av. Paulista/Consolação, São Paulo) para a
-- sugestão de técnico calcular uma distância não-trivial.
--
-- Se este seed não fizer sentido para um ambiente que não seja de demonstração
-- local, não rodar esta migration nesse ambiente é uma decisão de deploy —
-- fora do escopo deste fechamento.
WITH contrato_demo AS (
    INSERT INTO contratos (cidade, secretario_responsavel, sla_horas, link_portal_chamados)
    VALUES ('São Paulo', 'Secretaria Demo', 24, NULL)
    RETURNING id
), base_demo AS (
    INSERT INTO bases_operacionais (nome, endereco, cep, bairro, cidade, latitude, longitude, contrato_id)
    SELECT 'Base Operacional Central', 'Av. Paulista, 1000', '01310100', 'Bela Vista', 'São Paulo',
           -23.5613, -46.6565, contrato_demo.id
    FROM contrato_demo
    RETURNING id, contrato_id
), unidade_demo AS (
    INSERT INTO unidades (nome, endereco, cep, bairro, cidade, latitude, longitude, contrato_id)
    SELECT 'Unidade Demo', 'Rua Augusta, 500', '01305000', 'Consolação', 'São Paulo',
           -23.5548, -46.6620, contrato_demo.id
    FROM contrato_demo
    RETURNING id
), usuario_tecnico_demo AS (
    INSERT INTO usuarios (nome, email, perfil, senha, ativo)
    VALUES (
        'Técnico Demo',
        'tecnico@smartdispatch.local',
        'TECNICO',
        '$2a$10$inpgn293PZZjQAgPet81p.kOPVdV4RXUWGIdXxFmoOx08NMTPaMV.',
        true
    )
    RETURNING id
)
INSERT INTO tecnicos (usuario_id, base_operacional_id, ativo)
SELECT usuario_tecnico_demo.id, base_demo.id, true
FROM usuario_tecnico_demo, base_demo;
