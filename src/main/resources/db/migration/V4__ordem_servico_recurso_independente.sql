-- Sprint de Ordens de Serviço: OS deixa de depender obrigatoriamente de
-- Chamado; Chamado ganha contrato_id direto + numeroChamadoInterno
-- sequencial por contrato. V1/V2/V3 não são alteradas.
--
-- Dados atuais são só de demo (nenhum ambiente real depende deles) — os
-- backfills abaixo são simples de propósito, não uma migração de produção.

-- 1) chamados.contrato_id — derivado de unidade.contrato_id, nunca
--    informado livremente pelo cliente (aplicação sempre escreve os dois
--    a partir do mesmo contrato resolvido, evitando divergência).
ALTER TABLE chamados ADD COLUMN contrato_id BIGINT;

UPDATE chamados c
SET contrato_id = u.contrato_id
FROM unidades u
WHERE c.unidade_id = u.id;

ALTER TABLE chamados ALTER COLUMN contrato_id SET NOT NULL;

ALTER TABLE chamados
    ADD CONSTRAINT fk_chamados_contrato
        FOREIGN KEY (contrato_id) REFERENCES contratos (id);

-- 2) chamados.numero_chamado_interno — automático, sequencial por
--    contrato. Backfill dos registros existentes por ordem de abertura,
--    numeração real por contrato feita a partir daqui pela aplicação
--    (NumeracaoService), nunca por MAX+1.
ALTER TABLE chamados ADD COLUMN numero_chamado_interno BIGINT;

WITH numerados AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY contrato_id
               ORDER BY data_abertura, id
           ) AS numero
    FROM chamados
)
UPDATE chamados c
SET numero_chamado_interno = numerados.numero
FROM numerados
WHERE c.id = numerados.id;

ALTER TABLE chamados ALTER COLUMN numero_chamado_interno SET NOT NULL;

ALTER TABLE chamados
    ADD CONSTRAINT uk_chamados_contrato_numero_interno
        UNIQUE (contrato_id, numero_chamado_interno);

-- 3) contador por contrato — mecanismo seguro sob concorrência para gerar
--    numeroChamadoInterno (SELECT ... FOR UPDATE + incremento, nunca
--    MAX+1). Semântica: "próximo número a ser distribuído".
CREATE TABLE contrato_contadores (
    contrato_id BIGINT PRIMARY KEY,
    proximo_numero_chamado_interno BIGINT NOT NULL,
    CONSTRAINT fk_contrato_contadores_contrato
        FOREIGN KEY (contrato_id) REFERENCES contratos (id)
);

INSERT INTO contrato_contadores (contrato_id, proximo_numero_chamado_interno)
SELECT co.id, COALESCE(MAX(ch.numero_chamado_interno), 0) + 1
FROM contratos co
LEFT JOIN chamados ch ON ch.contrato_id = co.id
GROUP BY co.id;

-- 4) ordens_servico.chamado_id passa a ser opcional.
ALTER TABLE ordens_servico ALTER COLUMN chamado_id DROP NOT NULL;

-- 5) numero_ordem_servico: texto informado pelo cliente -> número gerado
--    pelo backend (sequência global, sem prefixo/zero-padding). O valor
--    textual anterior (ex.: "OS-DEMO-001", digitado livremente na tela
--    antiga) não tem significado de negócio a preservar — registros
--    existentes são renumerados sequencialmente por ordem de criação
--    (não é seguro assumir que o texto já era um número: subconsulta
--    correlacionada não é permitida em USING de ALTER COLUMN TYPE, por
--    isso a troca de tipo é feita via coluna nova + rename).
CREATE SEQUENCE seq_numero_os START WITH 1;

ALTER TABLE ordens_servico ADD COLUMN numero_ordem_servico_novo BIGINT;

WITH numerados AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS numero
    FROM ordens_servico
)
UPDATE ordens_servico os
SET numero_ordem_servico_novo = numerados.numero
FROM numerados
WHERE os.id = numerados.id;

ALTER TABLE ordens_servico DROP COLUMN numero_ordem_servico;
ALTER TABLE ordens_servico
    RENAME COLUMN numero_ordem_servico_novo TO numero_ordem_servico;

ALTER TABLE ordens_servico ALTER COLUMN numero_ordem_servico SET NOT NULL;
ALTER TABLE ordens_servico
    ADD CONSTRAINT uk_ordens_servico_numero UNIQUE (numero_ordem_servico);

SELECT setval(
    'seq_numero_os',
    COALESCE((SELECT MAX(numero_ordem_servico) FROM ordens_servico), 0) + 1,
    false
);

-- 6) novos campos da OS.
ALTER TABLE ordens_servico ADD COLUMN descricao VARCHAR(2000);
ALTER TABLE ordens_servico ADD COLUMN numero_patrimonio VARCHAR(255);
ALTER TABLE ordens_servico ADD COLUMN data DATE;
ALTER TABLE ordens_servico ADD COLUMN hora TIME;

UPDATE ordens_servico SET data = CURRENT_DATE WHERE data IS NULL;

ALTER TABLE ordens_servico ALTER COLUMN data SET NOT NULL;

-- 7) índice de apoio à visão diária/semanal (contrato via unidade de
--    atendimento, que é sempre obrigatória, com ou sem chamado).
CREATE INDEX idx_ordens_servico_unidade_atendimento_data
    ON ordens_servico (unidade_atendimento_id, data);
