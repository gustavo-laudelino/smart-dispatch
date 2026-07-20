import type {
    ComentarioChamado,
    OrdemServico,
} from "../types";

type CommentTimelineProps = {
    comentarios: ComentarioChamado[];
    ordensServico: OrdemServico[];
    novoComentarioTexto: string;
    novaComentarioOrdemServicoId: string;

    aoAlterarTexto: (texto: string) => void;

    aoAlterarOrdemServico: (
        ordemServicoId: string
    ) => void;

    aoAdicionarComentario: () => void;
};

function formatarData(data: string | null) {
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

function obterIniciais(nome: string) {
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
                             comentarios,
                             ordensServico,
                             novoComentarioTexto,
                             novaComentarioOrdemServicoId,
                             aoAlterarTexto,
                             aoAlterarOrdemServico,
                             aoAdicionarComentario,
                         }: CommentTimelineProps) {
    return (
        <section className="card activity-card">
            <header className="activity-header">
                <div>
                    <span className="label">
                        Comunicação
                    </span>

                    <div className="activity-title-row">
                        <h2 className="section-title">
                            Linha do tempo
                        </h2>

                        <span className="activity-count">
                            {comentarios.length}
                        </span>
                    </div>

                    <p>
                        Registre observações e acompanhe
                        as informações compartilhadas
                        durante o atendimento.
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
                            Registre uma nova informação
                            sobre o chamado.
                        </span>
                    </div>
                </div>

                <textarea
                    value={novoComentarioTexto}
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
                                    event.target.value
                                )
                            }
                        >
                            <option value="sem-os">
                                Comentário geral
                            </option>

                            {ordensServico.map(
                                (ordemServico) => (
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
                {comentarios.length === 0 ? (
                    <div className="activity-empty">
                        <div className="activity-empty-icon">
                            ◇
                        </div>

                        <h3>
                            Nenhum comentário registrado
                        </h3>

                        <p>
                            As observações adicionadas ao
                            chamado aparecerão aqui.
                        </p>
                    </div>
                ) : (
                    <div className="timeline">
                        {comentarios.map(
                            (comentario) => (
                                <article
                                    key={
                                        comentario.id
                                    }
                                    className="timeline-item"
                                >
                                    <div className="timeline-marker">
                                        {obterIniciais(
                                            comentario.autorNome
                                        )}
                                    </div>

                                    <div className="timeline-content">
                                        <div className="timeline-header">
                                            <div className="timeline-author">
                                                <strong>
                                                    {
                                                        comentario.autorNome
                                                    }
                                                </strong>

                                                {comentario.numeroOrdemServico && (
                                                    <span className="os-tag">
                                                        OS{" "}
                                                        {
                                                            comentario.numeroOrdemServico
                                                        }
                                                    </span>
                                                )}
                                            </div>

                                            <time>
                                                {formatarData(
                                                    comentario.dataCriacao
                                                )}
                                            </time>
                                        </div>

                                        <p>
                                            {
                                                comentario.texto
                                            }
                                        </p>
                                    </div>
                                </article>
                            )
                        )}
                    </div>
                )}
            </div>
        </section>
    );
}

export default CommentTimeline;