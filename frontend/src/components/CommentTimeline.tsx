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
    aoAlterarOrdemServico: (ordemServicoId: string) => void;
    aoAdicionarComentario: () => void;
};

function formatarData(data: string | null) {
    if (!data) {
        return "Não informado";
    }

    return new Date(data).toLocaleString("pt-BR");
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
        <section className="card">
            <h2 className="section-title">
                Linha do tempo
            </h2>

            <div className="comment-form">
                <textarea
                    value={novoComentarioTexto}
                    onChange={(event) =>
                        aoAlterarTexto(event.target.value)
                    }
                    placeholder="Escreva um comentário sobre o chamado..."
                    rows={4}
                />

                <div className="comment-form-footer">
                    <select
                        value={novaComentarioOrdemServicoId}
                        onChange={(event) =>
                            aoAlterarOrdemServico(
                                event.target.value
                            )
                        }
                    >
                        <option value="sem-os">
                            Comentário geral
                        </option>

                        {ordensServico.map((ordemServico) => (
                            <option
                                key={ordemServico.id}
                                value={ordemServico.id}
                            >
                                OS {ordemServico.numeroOrdemServico}
                            </option>
                        ))}
                    </select>

                    <button
                        className="primary-button"
                        onClick={aoAdicionarComentario}
                    >
                        Adicionar comentário
                    </button>
                </div>
            </div>

            {comentarios.length === 0 ? (
                <p>Nenhum comentário registrado.</p>
            ) : (
                <div className="timeline">
                    {comentarios.map((comentario) => (
                        <article
                            key={comentario.id}
                            className="timeline-item"
                        >
                            <div className="timeline-header">
                                <div>
                                    <strong>
                                        {comentario.autorNome}
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

                                <span>
                                    {formatarData(
                                        comentario.dataCriacao
                                    )}
                                </span>
                            </div>

                            <p>{comentario.texto}</p>
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
}

export default CommentTimeline;