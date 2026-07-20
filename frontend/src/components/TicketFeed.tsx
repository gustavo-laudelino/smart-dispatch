import type {
    Chamado,
    Contrato,
} from "../types";

import { FeedSkeleton } from "./LoadingSkeletons";

type FiltroStatus =
    | Chamado["status"]
    | "TODOS";

type OrdemData =
    | "MAIS_RECENTES"
    | "MAIS_ANTIGOS";

type TicketFeedProps = {
    chamados: Chamado[];
    contratos: Contrato[];

    contratoSelecionado: string;
    filtroStatus: FiltroStatus;
    ordemData: OrdemData;

    chamadoSelecionado: Chamado | null;
    carregando: boolean;

    aoAlterarContrato: (
        contratoId: string
    ) => void;

    aoAlterarStatus: (
        status: FiltroStatus
    ) => void;

    aoAlternarOrdenacao: () => void;

    aoSelecionarChamado: (
        chamado: Chamado
    ) => void;
};

const STATUS_OPTIONS: {
    valor: Chamado["status"];
    rotulo: string;
}[] = [
    {
        valor: "ABERTO",
        rotulo: "Aberto",
    },
    {
        valor: "ATRIBUIDO",
        rotulo: "Atribuído",
    },
    {
        valor: "EM_ATENDIMENTO",
        rotulo: "Em atendimento",
    },
    {
        valor: "AGUARDANDO_ANALISE",
        rotulo: "Em análise",
    },
    {
        valor: "PRONTO_PARA_FINALIZAR",
        rotulo: "Pronto para finalizar",
    },
    {
        valor: "PENDENTE",
        rotulo: "Pendente",
    },
    {
        valor: "AGUARDANDO_CLIENTE",
        rotulo: "Aguardando cliente",
    },
    {
        valor: "FINALIZADO",
        rotulo: "Finalizado",
    },
    {
        valor: "CANCELADO",
        rotulo: "Cancelado",
    },
];

const STATUS_LABELS: Record<
    Chamado["status"],
    string
> = STATUS_OPTIONS.reduce(
    (resultado, opcao) => {
        resultado[opcao.valor] =
            opcao.rotulo;

        return resultado;
    },
    {} as Record<
        Chamado["status"],
        string
    >
);

function criarClasseStatus(
    status: Chamado["status"]
) {
    return status
        .toLowerCase()
        .replace(/_/g, "-");
}

function formatarDataAbertura(
    data: string
) {
    const dataAbertura =
        new Date(data);

    if (
        Number.isNaN(
            dataAbertura.getTime()
        )
    ) {
        return "Data não informada";
    }

    return dataAbertura
        .toLocaleString("pt-BR", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        })
        .replace(",", "");
}

function TicketFeed({
                        chamados,
                        contratos,
                        contratoSelecionado,
                        filtroStatus,
                        ordemData,
                        chamadoSelecionado,
                        carregando,
                        aoAlterarContrato,
                        aoAlterarStatus,
                        aoAlternarOrdenacao,
                        aoSelecionarChamado,
                    }: TicketFeedProps) {
    const ordenandoMaisRecentes =
        ordemData === "MAIS_RECENTES";

    return (
        <section className="card feed-card">
            <div className="feed-header">
                <div className="feed-header-content">
                    <div className="feed-title-row">
                        <h2 className="section-title">
                            Chamados
                        </h2>

                        <span className="feed-count">
                            {chamados.length}
                        </span>
                    </div>

                    <div className="feed-inline-filters">
                        <label className="feed-inline-select feed-contract-button">
                            <span className="sr-only">
                                Contrato
                            </span>

                            <select
                                value={
                                    contratoSelecionado
                                }
                                onChange={(event) =>
                                    aoAlterarContrato(
                                        event.target.value
                                    )
                                }
                                aria-label="Selecionar contrato"
                                title="Selecionar contrato"
                            >
                                <option value="todos">
                                    Todos contratos
                                </option>

                                {contratos.map(
                                    (contrato) => (
                                        <option
                                            key={
                                                contrato.id
                                            }
                                            value={
                                                contrato.id
                                            }
                                        >
                                            {
                                                contrato.cidade
                                            }
                                        </option>
                                    )
                                )}
                            </select>
                        </label>

                        <label className="feed-inline-select feed-status-button">
                            <span className="sr-only">
                                Status
                            </span>

                            <select
                                value={filtroStatus}
                                onChange={(event) =>
                                    aoAlterarStatus(
                                        event.target
                                            .value as FiltroStatus
                                    )
                                }
                                aria-label="Filtrar por status"
                                title="Filtrar por status"
                            >
                                <option value="TODOS">
                                    Status
                                </option>

                                {STATUS_OPTIONS.map(
                                    (opcao) => (
                                        <option
                                            key={
                                                opcao.valor
                                            }
                                            value={
                                                opcao.valor
                                            }
                                        >
                                            {
                                                opcao.rotulo
                                            }
                                        </option>
                                    )
                                )}
                            </select>
                        </label>

                        <button
                            type="button"
                            className="feed-sort-toggle"
                            onClick={
                                aoAlternarOrdenacao
                            }
                            title={
                                ordenandoMaisRecentes
                                    ? "Mais recentes primeiro. Clique para mostrar os mais antigos."
                                    : "Mais antigos primeiro. Clique para mostrar os mais recentes."
                            }
                            aria-label={
                                ordenandoMaisRecentes
                                    ? "Ordenado pelos mais recentes"
                                    : "Ordenado pelos mais antigos"
                            }
                        >
                            <svg
                                viewBox="0 0 24 24"
                                aria-hidden="true"
                            >
                                <path
                                    className={
                                        ordenandoMaisRecentes
                                            ? "sort-arrow active"
                                            : "sort-arrow"
                                    }
                                    d="M7 4v14m-4-4 4 4 4-4"
                                />

                                <path
                                    className={
                                        !ordenandoMaisRecentes
                                            ? "sort-arrow active"
                                            : "sort-arrow"
                                    }
                                    d="M17 20V6m-4 4 4-4 4 4"
                                />
                            </svg>
                        </button>
                    </div>
                </div>
            </div>

            {carregando ? (
                <FeedSkeleton />
            ) : chamados.length === 0 ? (
                <div className="feed-empty-filter">
                    <strong>
                        Nenhum chamado encontrado
                    </strong>

                    <span>
                        Não existem chamados para
                        os filtros selecionados.
                    </span>
                </div>
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
                                        {
                                            chamado.numeroChamado
                                        }
                                    </span>

                                    <span
                                        className={`ticket-status ticket-status-${classeStatus}`}
                                    >
                                        {
                                            STATUS_LABELS[
                                                chamado.status
                                                ]
                                        }
                                    </span>
                                </div>

                                <div className="ticket-opened-at">
                                    <span className="ticket-opened-label">
                                        Aberto em
                                    </span>

                                    <time
                                        dateTime={
                                            chamado.dataAbertura
                                        }
                                    >
                                        {formatarDataAbertura(
                                            chamado.dataAbertura
                                        )}
                                    </time>
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