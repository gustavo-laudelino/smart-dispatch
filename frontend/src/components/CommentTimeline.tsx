import {
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    buscarHistoricoChamado,
} from "../api";

import type {
    Chamado,
    ComentarioChamado,
    HistoricoChamado,
    OrdemServico,
    TipoEventoChamado,
} from "../types";

type CommentTimelineProps = {
    chamado: Chamado;
    comentarios: ComentarioChamado[];
    ordensServico: OrdemServico[];

    novoComentarioTexto: string;
    novaComentarioOrdemServicoId: string;

    aoAlterarTexto: (
        texto: string
    ) => void;

    aoAlterarOrdemServico: (
        ordemServicoId: string
    ) => void;

    aoAdicionarComentario: () => void;
};

type ItemTimeline =
    | {
    id: string;
    origem: "COMENTARIO";
    data: string;
    comentario: ComentarioChamado;
}
    | {
    id: string;
    origem: "HISTORICO";
    data: string;
    historico: HistoricoChamado;
};

type ConfiguracaoEvento = {
    rotulo: string;
    marcador: string;
    classe: string;
};

const CONFIGURACOES_EVENTO: Record<
    TipoEventoChamado,
    ConfiguracaoEvento
> = {
    CHAMADO_CRIADO: {
        rotulo: "Chamado criado",
        marcador: "+",
        classe: "created",
    },

    DADOS_CHAMADO_ALTERADOS: {
        rotulo: "Dados atualizados",
        marcador: "✎",
        classe: "updated",
    },

    STATUS_ALTERADO: {
        rotulo: "Status alterado",
        marcador: "S",
        classe: "status",
    },

    ORDEM_SERVICO_CRIADA: {
        rotulo: "Ordem de serviço criada",
        marcador: "OS",
        classe: "service-order",
    },

    ORDEM_SERVICO_ALTERADA: {
        rotulo: "Ordem de serviço alterada",
        marcador: "OS",
        classe: "updated",
    },

    TECNICO_ATRIBUIDO: {
        rotulo: "Técnico atribuído",
        marcador: "T",
        classe: "technician",
    },

    TECNICO_ALTERADO: {
        rotulo: "Técnico alterado",
        marcador: "T",
        classe: "technician",
    },

    TECNICO_REMOVIDO: {
        rotulo: "Técnico removido",
        marcador: "T",
        classe: "removed",
    },

    UNIDADE_ORDEM_ALTERADA: {
        rotulo: "Unidade alterada",
        marcador: "U",
        classe: "updated",
    },

    ATENDIMENTO_INICIADO: {
        rotulo: "Atendimento iniciado",
        marcador: "▶",
        classe: "started",
    },

    ATENDIMENTO_FINALIZADO: {
        rotulo: "Atendimento finalizado",
        marcador: "✓",
        classe: "finished",
    },

    ATENDIMENTO_FINALIZADO_AUTOMATICAMENTE: {
        rotulo:
            "Atendimento finalizado automaticamente",
        marcador: "✓",
        classe: "automatic",
    },
};

function formatarData(
    data: string | null
) {
    if (!data) {
        return "Não informado";
    }

    return new Date(data).toLocaleString(
        "pt-BR",
        {
            dateStyle: "short",
            timeStyle: "short",
        }
    );
}

function obterTimestamp(
    data: string
) {
    const timestamp =
        new Date(data).getTime();

    return Number.isNaN(timestamp)
        ? 0
        : timestamp;
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

function CommentTimeline({
                             chamado,
                             comentarios,
                             ordensServico,
                             novoComentarioTexto,
                             novaComentarioOrdemServicoId,
                             aoAlterarTexto,
                             aoAlterarOrdemServico,
                             aoAdicionarComentario,
                         }: CommentTimelineProps) {
    const [
        historicos,
        setHistoricos,
    ] = useState<HistoricoChamado[]>([]);

    const [
        carregandoHistorico,
        setCarregandoHistorico,
    ] = useState(true);

    const [
        erroHistorico,
        setErroHistorico,
    ] = useState<string | null>(null);

    useEffect(() => {
        let requisicaoAtiva = true;

        setCarregandoHistorico(true);
        setErroHistorico(null);

        buscarHistoricoChamado(
            chamado.contratoId,
            chamado.id
        )
            .then((dados) => {
                if (!requisicaoAtiva) {
                    return;
                }

                setHistoricos(dados);
            })
            .catch((error: Error) => {
                if (!requisicaoAtiva) {
                    return;
                }

                setErroHistorico(
                    error.message
                );
            })
            .finally(() => {
                if (!requisicaoAtiva) {
                    return;
                }

                setCarregandoHistorico(
                    false
                );
            });

        return () => {
            requisicaoAtiva = false;
        };
    }, [chamado]);

    const itensTimeline =
        useMemo<ItemTimeline[]>(() => {
            const itensComentarios: ItemTimeline[] =
                comentarios.map(
                    (comentario) => ({
                        id:
                            "comentario-" +
                            comentario.id,

                        origem: "COMENTARIO",
                        data:
                        comentario.dataCriacao,
                        comentario,
                    })
                );

            const itensHistorico: ItemTimeline[] =
                historicos.map(
                    (historico) => ({
                        id:
                            "historico-" +
                            historico.id,

                        origem: "HISTORICO",
                        data:
                        historico.dataEvento,
                        historico,
                    })
                );

            return [
                ...itensComentarios,
                ...itensHistorico,
            ].sort(
                (itemA, itemB) =>
                    obterTimestamp(itemA.data) -
                    obterTimestamp(itemB.data)
            );
        }, [
            comentarios,
            historicos,
        ]);

    const quantidadeAtividades =
        comentarios.length +
        historicos.length;

    return (
        <section className="card activity-card">
            <header className="activity-header">
                <div>
                    <span className="label">
                        Histórico operacional
                    </span>

                    <div className="activity-title-row">
                        <h2 className="section-title">
                            Linha do tempo
                        </h2>

                        <span className="activity-count">
                            {
                                quantidadeAtividades
                            }
                        </span>
                    </div>

                    <p>
                        Acompanhe comentários,
                        alterações, atribuições e
                        atendimentos realizados neste
                        chamado.
                    </p>
                </div>
            </header>

            <div className="comment-composer">
                <div className="comment-composer-heading">
                    <div className="comment-composer-icon">
                        +
                    </div>

                    <div>
                        <strong>
                            Adicionar comentário
                        </strong>

                        <span>
                            Registre uma observação
                            complementar sobre o
                            chamado.
                        </span>
                    </div>
                </div>

                <textarea
                    value={
                        novoComentarioTexto
                    }
                    onChange={(event) =>
                        aoAlterarTexto(
                            event.target.value
                        )
                    }
                    placeholder="Escreva uma observação sobre o chamado..."
                    rows={4}
                />

                <div className="comment-composer-footer">
                    <label className="comment-context">
                        <span className="label">
                            Contexto
                        </span>

                        <select
                            value={
                                novaComentarioOrdemServicoId
                            }
                            onChange={(event) =>
                                aoAlterarOrdemServico(
                                    event.target
                                        .value
                                )
                            }
                        >
                            <option value="sem-os">
                                Comentário geral
                            </option>

                            {ordensServico.map(
                                (
                                    ordemServico
                                ) => (
                                    <option
                                        key={
                                            ordemServico.id
                                        }
                                        value={
                                            ordemServico.id
                                        }
                                    >
                                        OS{" "}
                                        {
                                            ordemServico.numeroOrdemServico
                                        }
                                    </option>
                                )
                            )}
                        </select>
                    </label>

                    <button
                        type="button"
                        className="primary-button"
                        onClick={
                            aoAdicionarComentario
                        }
                        disabled={
                            !novoComentarioTexto.trim()
                        }
                    >
                        Publicar comentário
                    </button>
                </div>
            </div>

            <div className="activity-content">
                {erroHistorico && (
                    <div className="operational-timeline-error">
                        <strong>
                            Histórico indisponível
                        </strong>

                        <span>
                            {erroHistorico}
                        </span>
                    </div>
                )}

                {carregandoHistorico && (
                    <div className="operational-timeline-loading">
                        <span />

                        Carregando histórico...
                    </div>
                )}

                {!carregandoHistorico &&
                itensTimeline.length === 0 ? (
                    <div className="activity-empty">
                        <div className="activity-empty-icon">
                            ◇
                        </div>

                        <h3>
                            Nenhuma atividade registrada
                        </h3>

                        <p>
                            Comentários e eventos
                            operacionais aparecerão aqui.
                        </p>
                    </div>
                ) : (
                    <div className="operational-timeline-list">
                        {itensTimeline.map(
                            (item) => {
                                if (
                                    item.origem ===
                                    "COMENTARIO"
                                ) {
                                    const comentario =
                                        item.comentario;

                                    return (
                                        <article
                                            key={
                                                item.id
                                            }
                                            className="operational-timeline-item timeline-comment"
                                        >
                                            <div className="operational-timeline-rail">
                                                <div className="operational-timeline-marker comment-marker">
                                                    {obterIniciais(
                                                        comentario.autorNome
                                                    )}
                                                </div>
                                            </div>

                                            <div className="operational-timeline-content">
                                                <header className="operational-timeline-header">
                                                    <div>
                                                        <span className="operational-timeline-type">
                                                            Comentário
                                                        </span>

                                                        <strong>
                                                            {
                                                                comentario.autorNome
                                                            }
                                                        </strong>
                                                    </div>

                                                    <time>
                                                        {formatarData(
                                                            comentario.dataCriacao
                                                        )}
                                                    </time>
                                                </header>

                                                {comentario.numeroOrdemServico && (
                                                    <span className="operational-os-tag">
                                                        OS{" "}
                                                        {
                                                            comentario.numeroOrdemServico
                                                        }
                                                    </span>
                                                )}

                                                <p>
                                                    {
                                                        comentario.texto
                                                    }
                                                </p>
                                            </div>
                                        </article>
                                    );
                                }

                                const historico =
                                    item.historico;

                                const configuracao =
                                    CONFIGURACOES_EVENTO[
                                        historico
                                            .tipoEvento
                                        ];

                                return (
                                    <article
                                        key={item.id}
                                        className={`operational-timeline-item timeline-history event-${configuracao.classe}`}
                                    >
                                        <div className="operational-timeline-rail">
                                            <div className="operational-timeline-marker history-marker">
                                                {
                                                    configuracao.marcador
                                                }
                                            </div>
                                        </div>

                                        <div className="operational-timeline-content">
                                            <header className="operational-timeline-header">
                                                <div>
                                                    <span className="operational-timeline-type">
                                                        Evento do sistema
                                                    </span>

                                                    <strong>
                                                        {
                                                            configuracao.rotulo
                                                        }
                                                    </strong>
                                                </div>

                                                <time>
                                                    {formatarData(
                                                        historico.dataEvento
                                                    )}
                                                </time>
                                            </header>

                                            {historico.numeroOrdemServico && (
                                                <span className="operational-os-tag">
                                                    OS{" "}
                                                    {
                                                        historico.numeroOrdemServico
                                                    }
                                                </span>
                                            )}

                                            <p>
                                                {
                                                    historico.descricao
                                                }
                                            </p>
                                        </div>
                                    </article>
                                );
                            }
                        )}
                    </div>
                )}
            </div>
        </section>
    );
}

export default CommentTimeline;