import { useEffect, useState } from "react";

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

function AssignTechnicianForm({
                                  chamado,
                                  ordemServico,
                                  aoCancelar,
                                  aoTecnicoAtribuido,
                              }: AssignTechnicianFormProps) {
    const possuiTecnico =
        ordemServico.tecnicoId !== null;

    const [sugestoes, setSugestoes] =
        useState<SugestaoTecnico[]>([]);

    const [carregando, setCarregando] =
        useState(true);

    const [
        tecnicoEmProcessamentoId,
        setTecnicoEmProcessamentoId,
    ] = useState<number | null>(null);

    const [removendo, setRemovendo] =
        useState(false);

    const [erro, setErro] =
        useState<string | null>(null);

    const processando =
        tecnicoEmProcessamentoId !== null ||
        removendo;

    useEffect(() => {
        setCarregando(true);
        setErro(null);

        buscarSugestoesTecnicos(
            chamado.contratoId,
            chamado.id,
            ordemServico.id
        )
            .then(setSugestoes)
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

    async function selecionarTecnico(
        sugestao: SugestaoTecnico
    ) {
        setErro(null);
        setTecnicoEmProcessamentoId(
            sugestao.tecnicoId
        );

        try {
            const ordemAtualizada =
                await atualizarTecnico(
                    sugestao.tecnicoId
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
            setTecnicoEmProcessamentoId(null);
        }
    }

    async function removerTecnico() {
        if (!possuiTecnico) {
            return;
        }

        const confirmou = window.confirm(
            `Deseja remover o técnico ${ordemServico.tecnicoNome} desta ordem de serviço?`
        );

        if (!confirmou) {
            return;
        }

        setErro(null);
        setRemovendo(true);

        try {
            const ordemAtualizada =
                await atualizarTecnico(null);

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
        <section className="assign-technician-form">
            <div className="technician-selection-header">
                <div>
                    <span className="label">
                        Smart Dispatch
                    </span>

                    <h3>Selecionar técnico</h3>

                    <p>
                        Sugestões baseadas em proximidade
                        e carga operacional.
                    </p>
                </div>

                <button
                    type="button"
                    className="secondary-button"
                    onClick={aoCancelar}
                    disabled={processando}
                >
                    Fechar
                </button>
            </div>

            {possuiTecnico && (
                <p className="current-technician">
                    Técnico atual:{" "}
                    <strong>
                        {ordemServico.tecnicoNome}
                    </strong>
                </p>
            )}

            {erro && (
                <div className="error">
                    {erro}
                </div>
            )}

            {carregando && (
                <p>Calculando sugestões...</p>
            )}

            {!carregando &&
                sugestoes.length === 0 && (
                    <p>
                        Nenhum técnico disponível para
                        este chamado.
                    </p>
                )}

            {!carregando &&
                sugestoes.length > 0 && (
                    <div className="technician-suggestion-list">
                        {sugestoes.map(
                            (sugestao, index) => {
                                const tecnicoAtual =
                                    ordemServico.tecnicoId ===
                                    sugestao.tecnicoId;

                                const salvando =
                                    tecnicoEmProcessamentoId ===
                                    sugestao.tecnicoId;

                                return (
                                    <article
                                        key={
                                            sugestao.tecnicoId
                                        }
                                        className={`technician-suggestion-card ${
                                            index === 0
                                                ? "best-suggestion"
                                                : ""
                                        }`}
                                    >
                                        <div className="technician-suggestion-content">
                                            {index === 0 && (
                                                <span className="best-suggestion-label">
                                                    Melhor indicação
                                                </span>
                                            )}

                                            <h4>
                                                {
                                                    sugestao.tecnicoNome
                                                }
                                            </h4>

                                            <div className="technician-rating">
                                                <div
                                                    className="stars"
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

                                                <span
                                                    className={`indication-badge indication-${sugestao.nivelIndicacao.toLowerCase()}`}
                                                >
                                                    {formatarNivelIndicacao(
                                                        sugestao.nivelIndicacao
                                                    )}
                                                </span>
                                            </div>
                                        </div>

                                        <button
                                            type="button"
                                            className={
                                                index === 0
                                                    ? "primary-button"
                                                    : "secondary-button"
                                            }
                                            onClick={() =>
                                                selecionarTecnico(
                                                    sugestao
                                                )
                                            }
                                            disabled={
                                                processando ||
                                                tecnicoAtual
                                            }
                                        >
                                            {tecnicoAtual
                                                ? "Selecionado"
                                                : salvando
                                                    ? "Selecionando..."
                                                    : "Selecionar"}
                                        </button>
                                    </article>
                                );
                            }
                        )}
                    </div>
                )}

            {possuiTecnico && (
                <div className="form-actions">
                    <button
                        type="button"
                        className="danger-button"
                        onClick={removerTecnico}
                        disabled={processando}
                    >
                        {removendo
                            ? "Removendo..."
                            : "Remover técnico"}
                    </button>
                </div>
            )}
        </section>
    );
}

export default AssignTechnicianForm;