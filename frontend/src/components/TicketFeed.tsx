import {
    useMemo,
    useState,
} from "react";

import type {
    Chamado,
    Contrato,
    Tecnico,
} from "../types";

import {
    FeedSkeleton,
} from "./LoadingSkeletons";

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

    paginaAtual: number;
    totalPaginas: number;
    totalChamados: number;

    aoAlterarContrato: (
        contratoId: string
    ) => void;

    aoAlterarStatus: (
        status: FiltroStatus
    ) => void;

    aoAlterarPagina: (
        pagina: number
    ) => void;

    aoAlternarOrdenacao: () => void;

    aoSelecionarChamado: (
        chamado: Chamado
    ) => void;

    perfilTecnico: boolean;

    tecnicoSelecionado: string;
    tecnicosDoContrato: Tecnico[];
    aoAlterarTecnico: (
        tecnicoId: string
    ) => void;

    meusChamados: boolean;
    aoAlternarMeusChamados: () => void;
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

function normalizarTexto(
    valor: string | null | undefined
) {
    return (valor ?? "")
        .normalize("NFD")
        .replace(
            /[\u0300-\u036f]/g,
            ""
        )
        .replace(/_/g, " ")
        .toLowerCase()
        .trim();
}

function criarTextoPesquisa(
    chamado: Chamado
) {
    return normalizarTexto(
        [
            chamado.numeroChamado,
            chamado.unidadeNome,
            chamado.contratoCidade,

            chamado.solicitante.nome,
            chamado.solicitante.email,
            chamado.solicitante.telefone,
            chamado.solicitante.identificacao,

            chamado.numeroPatrimonio,
            chamado.tipo,
            chamado.categoria,
            chamado.prioridade,
            chamado.status,
            STATUS_LABELS[chamado.status],
            chamado.descricao,
        ]
            .filter(Boolean)
            .join(" ")
    );
}

function TicketFeed({
                        chamados,
                        contratos,
                        contratoSelecionado,
                        filtroStatus,
                        ordemData,
                        chamadoSelecionado,
                        carregando,
                        paginaAtual,
                        totalPaginas,
                        totalChamados,
                        aoAlterarContrato,
                        aoAlterarStatus,
                        aoAlterarPagina,
                        aoAlternarOrdenacao,
                        aoSelecionarChamado,
                        perfilTecnico,
                        tecnicoSelecionado,
                        tecnicosDoContrato,
                        aoAlterarTecnico,
                        meusChamados,
                        aoAlternarMeusChamados,
                    }: TicketFeedProps) {
    const [
        termoBusca,
        setTermoBusca,
    ] = useState("");

    const ordenandoMaisRecentes =
        ordemData === "MAIS_RECENTES";

    const chamadosPesquisados =
        useMemo(() => {
            const buscaNormalizada =
                normalizarTexto(
                    termoBusca
                );

            if (!buscaNormalizada) {
                return chamados;
            }

            const termos =
                buscaNormalizada
                    .split(/\s+/)
                    .filter(Boolean);

            return chamados.filter(
                (chamado) => {
                    const textoPesquisa =
                        criarTextoPesquisa(
                            chamado
                        );

                    return termos.every(
                        (termo) =>
                            textoPesquisa.includes(
                                termo
                            )
                    );
                }
            );
        }, [
            chamados,
            termoBusca,
        ]);

    const existeBusca =
        termoBusca.trim().length > 0;

    return (
        <section className="card feed-card">
            <div className="feed-header">
                <div className="feed-header-content">
                    <div className="feed-title-row">
                        <h2 className="section-title">
                            Chamados
                        </h2>

                        <span
                            className="feed-count"
                            title={
                                existeBusca
                                    ? `${chamadosPesquisados.length} de ${chamados.length} chamados nesta página`
                                    : `${chamados.length} de ${totalChamados} chamados`
                            }
                        >
                            {
                                chamadosPesquisados.length
                            }
                        </span>
                    </div>

                    <div className="feed-search">
                        <svg
                            viewBox="0 0 24 24"
                            aria-hidden="true"
                        >
                            <circle
                                cx="11"
                                cy="11"
                                r="7"
                            />

                            <path d="m16.5 16.5 4 4" />
                        </svg>

                        <input
                            type="search"
                            value={termoBusca}
                            onChange={(event) =>
                                setTermoBusca(
                                    event.target.value
                                )
                            }
                            placeholder="Buscar chamados..."
                            aria-label="Buscar chamados"
                        />

                        {existeBusca && (
                            <button
                                type="button"
                                className="feed-search-clear"
                                onClick={() =>
                                    setTermoBusca("")
                                }
                                aria-label="Limpar busca"
                                title="Limpar busca"
                            >
                                ×
                            </button>
                        )}
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
                                    Contratos
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

                        {!perfilTecnico && (
                            <label className="feed-inline-select feed-technician-button">
                                <span className="sr-only">
                                    Técnico
                                </span>

                                <select
                                    value={
                                        tecnicoSelecionado
                                    }
                                    onChange={(event) =>
                                        aoAlterarTecnico(
                                            event.target.value
                                        )
                                    }
                                    disabled={
                                        contratoSelecionado ===
                                        "todos"
                                    }
                                    aria-label="Filtrar por técnico"
                                    title={
                                        contratoSelecionado ===
                                        "todos"
                                            ? "Selecione um contrato para filtrar por técnico"
                                            : "Filtrar por técnico"
                                    }
                                >
                                    <option value="todos">
                                        Técnicos
                                    </option>

                                    {tecnicosDoContrato.map(
                                        (tecnico) => (
                                            <option
                                                key={
                                                    tecnico.id
                                                }
                                                value={
                                                    tecnico.id
                                                }
                                            >
                                                {
                                                    tecnico.nome
                                                }
                                            </option>
                                        )
                                    )}
                                </select>
                            </label>
                        )}

                        {perfilTecnico && (
                            <label
                                className="feed-inline-select feed-my-tickets-toggle"
                                title="Mostrar somente chamados relacionados a mim"
                            >
                                <input
                                    type="checkbox"
                                    checked={meusChamados}
                                    onChange={
                                        aoAlternarMeusChamados
                                    }
                                    aria-label="Meus chamados"
                                />

                                <span>Meus chamados</span>
                            </label>
                        )}

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
            ) : chamadosPesquisados.length ===
            0 ? (
                <div className="feed-empty-filter">
                    <strong>
                        {existeBusca
                            ? "Nenhum resultado para a busca"
                            : "Nenhum chamado encontrado"}
                    </strong>

                    <span>
                        {existeBusca
                            ? `Não encontramos chamados relacionados a “${termoBusca.trim()}”.`
                            : "Não existem chamados para os filtros selecionados."}
                    </span>

                    {existeBusca && (
                        <button
                            type="button"
                            className="feed-empty-clear"
                            onClick={() =>
                                setTermoBusca("")
                            }
                        >
                            Limpar busca
                        </button>
                    )}
                </div>
            ) : (
                <div className="ticket-list">
                    {chamadosPesquisados.map(
                        (chamado) => {
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
                        }
                    )}
                </div>
            )}

            {!carregando &&
                totalPaginas > 1 && (
                    <div className="feed-pagination">
                        <button
                            type="button"
                            disabled={
                                paginaAtual === 0
                            }
                            onClick={() =>
                                aoAlterarPagina(
                                    paginaAtual - 1
                                )
                            }
                        >
                            Anterior
                        </button>

            <span className="feed-pagination-info">
{paginaAtual + 1} de {totalPaginas}
</span>

                        <button
                            type="button"
                            disabled={
                                paginaAtual + 1 >=
                                totalPaginas
                            }
                            onClick={() =>
                                aoAlterarPagina(
                                    paginaAtual + 1
                                )
                            }
                        >
                            Próxima
                        </button>
                    </div>
                )}
        </section>
    );
}

export default TicketFeed;