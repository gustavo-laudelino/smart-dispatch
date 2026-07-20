import type { Chamado } from "../types";
import { FeedSkeleton } from "./LoadingSkeletons";

type TicketFeedProps = {
    chamados: Chamado[];
    chamadoSelecionado: Chamado | null;
    carregando: boolean;

    aoSelecionarChamado: (
        chamado: Chamado
    ) => void;
};

const STATUS_LABELS: Record<
    Chamado["status"],
    string
> = {
    ABERTO: "Aberto",
    ATRIBUIDO: "Atribuído",
    EM_ATENDIMENTO: "Em atendimento",
    AGUARDANDO_ANALISE: "Em análise",
    PRONTO_PARA_FINALIZAR: "Pronto para finalizar",
    PENDENTE: "Pendente",
    AGUARDANDO_CLIENTE: "Aguardando cliente",
    FINALIZADO: "Finalizado",
    CANCELADO: "Cancelado",
};

function criarClasseStatus(
    status: Chamado["status"]
) {
    return status
        .toLowerCase()
        .replace(/_/g, "-");
}

function TicketFeed({
                        chamados,
                        chamadoSelecionado,
                        carregando,
                        aoSelecionarChamado,
                    }: TicketFeedProps) {
    return (
        <section className="card feed-card">
            <div className="feed-header">
                <div>
                    <span className="label">
                        Fila operacional
                    </span>

                    <h2 className="section-title">
                        Chamados
                    </h2>
                </div>

                <span className="feed-count">
                    {chamados.length}
                </span>
            </div>

            {carregando ? (
                <FeedSkeleton />
            ) : chamados.length === 0 ? (
                <p className="feed-message">
                    Nenhum chamado encontrado.
                </p>
            ) : (
                <div className="ticket-list">
                    {chamados.map((chamado) => {
                        const selecionado =
                            chamadoSelecionado?.id ===
                            chamado.id;

                        const classeStatus =
                            criarClasseStatus(
                                chamado.status
                            );

                        return (
                            <button
                                type="button"
                                key={chamado.id}
                                className={
                                    selecionado
                                        ? "ticket-item selected"
                                        : "ticket-item"
                                }
                                onClick={() =>
                                    aoSelecionarChamado(
                                        chamado
                                    )
                                }
                            >
                                <div className="ticket-item-top">
                                    <span className="ticket-number">
                                        OSTI{" "}
                                        {
                                            chamado.numeroChamado
                                        }
                                    </span>

                                    <span
                                        className={`ticket-status ticket-status-${classeStatus}`}
                                    >
                                        {
                                            STATUS_LABELS[
                                                chamado
                                                    .status
                                                ]
                                        }
                                    </span>
                                </div>

                                <strong className="ticket-unit">
                                    {
                                        chamado.unidadeNome
                                    }
                                </strong>

                                <div className="ticket-item-footer">
                                    <span>
                                        {
                                            chamado.contratoCidade
                                        }
                                    </span>

                                    <span className="ticket-priority">
                                        {
                                            chamado.prioridade
                                        }
                                    </span>
                                </div>
                            </button>
                        );
                    })}
                </div>
            )}
        </section>
    );
}

export default TicketFeed;