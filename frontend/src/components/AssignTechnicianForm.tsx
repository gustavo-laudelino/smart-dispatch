import {
    useEffect,
    useState,
} from "react";

import {
    atualizarOrdemServico,
    buscarSugestoesTecnicos,
} from "../api";

import type {
    Chamado,
    OrdemServico,
    OrdemServicoRequest,
    SugestaoTecnico,
} from "../types";

type AssignTechnicianFormProps = {
    chamado: Chamado;
    ordemServico: OrdemServico;

    aoCancelar: () => void;

    aoTecnicoAtribuido: (
        ordemServico: OrdemServico
    ) => void | Promise<void>;
};

function formatarNivelIndicacao(
    nivel: SugestaoTecnico["nivelIndicacao"]
) {
    switch (nivel) {
        case "ALTA":
            return "Indicação alta";

        case "MODERADA":
            return "Indicação moderada";

        default:
            return "Indicação leve";
    }
}

function obterIniciais(
    nome: string
) {
    return nome
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map((parte) =>
            parte.charAt(0)
        )
        .join("")
        .toUpperCase();
}

function formatarDistancia(
    distanciaKm: number
) {
    return distanciaKm.toLocaleString(
        "pt-BR",
        {
            minimumFractionDigits: 1,
            maximumFractionDigits: 1,
        }
    );
}

function AssignTechnicianForm({
                                  chamado,
                                  ordemServico,
                                  aoCancelar,
                                  aoTecnicoAtribuido,
                              }: AssignTechnicianFormProps) {
    const possuiTecnico =
        ordemServico.tecnicoId !== null;

    const [
        sugestoes,
        setSugestoes,
    ] = useState<SugestaoTecnico[]>([]);

    const [
        tecnicoSelecionadoId,
        setTecnicoSelecionadoId,
    ] = useState<number | null>(
        ordemServico.tecnicoId
    );

    const [
        carregando,
        setCarregando,
    ] = useState(true);

    const [
        tecnicoEmProcessamentoId,
        setTecnicoEmProcessamentoId,
    ] = useState<number | null>(null);

    const [
        removendo,
        setRemovendo,
    ] = useState(false);

    const [
        erro,
        setErro,
    ] = useState<string | null>(null);

    const processando =
        tecnicoEmProcessamentoId !== null ||
        removendo;

    const tecnicoSelecionado =
        sugestoes.find(
            (sugestao) =>
                sugestao.tecnicoId ===
                tecnicoSelecionadoId
        ) ?? null;

    const tecnicoSelecionadoEhAtual =
        tecnicoSelecionadoId !== null &&
        tecnicoSelecionadoId ===
        ordemServico.tecnicoId;

    useEffect(() => {
        setCarregando(true);
        setErro(null);

        buscarSugestoesTecnicos(
            chamado.contratoId,
            chamado.id,
            ordemServico.id
        )
            .then((dados) => {
                setSugestoes(dados);

                setTecnicoSelecionadoId(
                    ordemServico.tecnicoId
                );
            })
            .catch((error: Error) => {
                setErro(error.message);
            })
            .finally(() => {
                setCarregando(false);
            });
    }, [
        chamado.contratoId,
        chamado.id,
        ordemServico.id,
        ordemServico.tecnicoId,
    ]);

    async function atualizarTecnico(
        tecnicoId: number | null
    ) {
        const request: OrdemServicoRequest = {
            numeroOrdemServico:
            ordemServico.numeroOrdemServico,

            tecnicoId,

            unidadeAtendimentoId: null,
        };

        return atualizarOrdemServico(
            chamado.contratoId,
            chamado.id,
            ordemServico.id,
            request
        );
    }

    async function confirmarTecnico() {
        if (
            tecnicoSelecionado === null ||
            tecnicoSelecionadoEhAtual
        ) {
            return;
        }

        setErro(null);

        setTecnicoEmProcessamentoId(
            tecnicoSelecionado.tecnicoId
        );

        try {
            const ordemAtualizada =
                await atualizarTecnico(
                    tecnicoSelecionado.tecnicoId
                );

            await aoTecnicoAtribuido(
                ordemAtualizada
            );
        } catch (error) {
            setErro(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        } finally {
            setTecnicoEmProcessamentoId(
                null
            );
        }
    }

    async function removerTecnico() {
        if (!possuiTecnico) {
            return;
        }

        const confirmou =
            window.confirm(
                `Deseja remover o técnico ${ordemServico.tecnicoNome} desta ordem de serviço?`
            );

        if (!confirmou) {
            return;
        }

        setErro(null);
        setRemovendo(true);

        try {
            const ordemAtualizada =
                await atualizarTecnico(
                    null
                );

            await aoTecnicoAtribuido(
                ordemAtualizada
            );
        } catch (error) {
            setErro(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        } finally {
            setRemovendo(false);
        }
    }

    return (
        <section className="assign-technician-form technician-ranking-panel">
            <header className="technician-selection-header">
                <div>
                    <span className="label">
                        Smart Dispatch
                    </span>

                    <h3>
                        Selecionar técnico
                    </h3>

                    <p>
                        Compare distância, carga atual
                        e distribuição recente.
                    </p>
                </div>

                <button
                    type="button"
                    className="technician-drawer-close"
                    onClick={aoCancelar}
                    disabled={processando}
                    aria-label="Fechar seleção de técnicos"
                >
                    ×
                </button>
            </header>

            <div className="technician-ranking-body">
                <div className="technician-ranking-context">
                    <span>
                        Ordem de serviço
                    </span>

                    <strong>
                        {
                            ordemServico.numeroOrdemServico
                        }
                    </strong>

                    <small>
                        {
                            ordemServico
                                .unidadeAtendimentoNome
                        }
                    </small>
                </div>

                {possuiTecnico && (
                    <div className="technician-current-summary">
                        <span>
                            Técnico atual
                        </span>

                        <strong>
                            {
                                ordemServico.tecnicoNome
                            }
                        </strong>
                    </div>
                )}

                {erro && (
                    <div className="technician-drawer-error">
                        {erro}
                    </div>
                )}

                {carregando && (
                    <div className="technician-ranking-loading">
                        <span className="technician-loading-spinner" />

                        <strong>
                            Calculando sugestões
                        </strong>

                        <p>
                            Analisando distância e
                            carga operacional.
                        </p>
                    </div>
                )}

                {!carregando &&
                    sugestoes.length === 0 && (
                        <div className="technician-ranking-empty">
                            <strong>
                                Nenhum técnico disponível
                            </strong>

                            <p>
                                Não encontramos técnicos
                                ativos para este contrato.
                            </p>
                        </div>
                    )}

                {!carregando &&
                    sugestoes.length > 0 && (
                        <div className="technician-suggestion-list">
                            {sugestoes.map(
                                (
                                    sugestao,
                                    index
                                ) => {
                                    const selecionado =
                                        tecnicoSelecionadoId ===
                                        sugestao.tecnicoId;

                                    const tecnicoAtual =
                                        ordemServico.tecnicoId ===
                                        sugestao.tecnicoId;

                                    return (
                                        <button
                                            type="button"
                                            key={
                                                sugestao.tecnicoId
                                            }
                                            className={`technician-suggestion-card ${
                                                index === 0
                                                    ? "best-suggestion"
                                                    : ""
                                            } ${
                                                selecionado
                                                    ? "selected"
                                                    : ""
                                            }`}
                                            onClick={() =>
                                                setTecnicoSelecionadoId(
                                                    sugestao.tecnicoId
                                                )
                                            }
                                            disabled={
                                                processando
                                            }
                                            aria-pressed={
                                                selecionado
                                            }
                                        >
                                            <div className="technician-card-top">
                                                <div className="technician-card-person">
                                                    <span className="technician-card-avatar">
                                                        {obterIniciais(
                                                            sugestao.tecnicoNome
                                                        )}
                                                    </span>

                                                    <div>
                                                        <span className="technician-position">
                                                            #
                                                            {
                                                                index +
                                                                1
                                                            }{" "}
                                                            no ranking
                                                        </span>

                                                        <h4>
                                                            {
                                                                sugestao.tecnicoNome
                                                            }
                                                        </h4>
                                                    </div>
                                                </div>

                                                <span className="technician-selection-indicator">
                                                    {selecionado
                                                        ? "✓"
                                                        : ""}
                                                </span>
                                            </div>

                                            <div className="technician-card-badges">
                                                {index ===
                                                    0 && (
                                                        <span className="best-suggestion-label">
                                                        Melhor indicação
                                                    </span>
                                                    )}

                                                {tecnicoAtual && (
                                                    <span className="current-technician-label">
                                                        Técnico atual
                                                    </span>
                                                )}

                                                <span
                                                    className={`indication-badge indication-${sugestao.nivelIndicacao.toLowerCase()}`}
                                                >
                                                    {formatarNivelIndicacao(
                                                        sugestao.nivelIndicacao
                                                    )}
                                                </span>

                                                <div
                                                    className="technician-rating"
                                                    aria-label={`${sugestao.estrelas} estrelas`}
                                                >
                                                    {[1, 2, 3].map(
                                                        (
                                                            estrela
                                                        ) => (
                                                            <span
                                                                key={
                                                                    estrela
                                                                }
                                                                className={
                                                                    estrela <=
                                                                    sugestao.estrelas
                                                                        ? "star active"
                                                                        : "star"
                                                                }
                                                            >
                                                                ★
                                                            </span>
                                                        )
                                                    )}
                                                </div>
                                            </div>

                                            <div className="technician-metrics-grid">
                                                <div className="technician-metric">
                                                    <span>
                                                        Distância
                                                        estimada
                                                    </span>

                                                    <strong>
                                                        {formatarDistancia(
                                                            sugestao.distanciaKm
                                                        )}{" "}
                                                        km
                                                    </strong>
                                                </div>

                                                <div className="technician-metric">
                                                    <span>
                                                        OS ativas
                                                    </span>

                                                    <strong>
                                                        {
                                                            sugestao.quantidadeOsAtivas
                                                        }
                                                    </strong>
                                                </div>

                                                <div className="technician-metric">
                                                    <span>
                                                        Atribuídas
                                                        hoje
                                                    </span>

                                                    <strong>
                                                        {
                                                            sugestao.atribuicoesHoje
                                                        }
                                                    </strong>
                                                </div>

                                                <div className="technician-metric">
                                                    <span>
                                                        Concluídas
                                                        em 15 dias
                                                    </span>

                                                    <strong>
                                                        {
                                                            sugestao.atendimentosUltimos15Dias
                                                        }
                                                    </strong>
                                                </div>
                                            </div>

                                            <div className="technician-card-score">
                                                <span>
                                                    Pontuação operacional
                                                </span>

                                                <strong>
                                                    {sugestao.pontuacao.toLocaleString(
                                                        "pt-BR",
                                                        {
                                                            maximumFractionDigits: 2,
                                                        }
                                                    )}
                                                </strong>
                                            </div>
                                        </button>
                                    );
                                }
                            )}
                        </div>
                    )}

                <p className="technician-ranking-note">
                    A distância considera a OS ativa
                    mais próxima. Sem OS ativa, utiliza
                    a base operacional do técnico.
                </p>
            </div>

            <footer className="technician-selection-footer">
                <div className="technician-footer-selection">
                    <span>
                        Técnico selecionado
                    </span>

                    <strong>
                        {tecnicoSelecionado
                            ? tecnicoSelecionado.tecnicoNome
                            : "Nenhum técnico selecionado"}
                    </strong>
                </div>

                <div className="technician-footer-actions">
                    {possuiTecnico && (
                        <button
                            type="button"
                            className="technician-remove-button"
                            onClick={
                                removerTecnico
                            }
                            disabled={
                                processando
                            }
                        >
                            {removendo
                                ? "Removendo..."
                                : "Remover atual"}
                        </button>
                    )}

                    <button
                        type="button"
                        className="technician-cancel-button"
                        onClick={aoCancelar}
                        disabled={processando}
                    >
                        Cancelar
                    </button>

                    <button
                        type="button"
                        className="technician-confirm-button"
                        onClick={
                            confirmarTecnico
                        }
                        disabled={
                            processando ||
                            tecnicoSelecionado ===
                            null ||
                            tecnicoSelecionadoEhAtual
                        }
                    >
                        {tecnicoEmProcessamentoId !==
                        null
                            ? "Atribuindo..."
                            : tecnicoSelecionadoEhAtual
                                ? "Já atribuído"
                                : "Atribuir técnico"}
                    </button>
                </div>
            </footer>
        </section>
    );
}

export default AssignTechnicianForm;