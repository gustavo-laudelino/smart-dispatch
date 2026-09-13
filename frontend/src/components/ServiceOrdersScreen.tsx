import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    buscarOrdensServico,
    buscarTecnicosPorContrato,
    finalizarAtendimento as finalizarAtendimentoApi,
} from "../api";

import { iniciarAtendimentoComConfirmacao } from "../utils/checkIn";

import {
    adicionarDias,
    formatarDataCurta,
    formatarDiaSemana,
    formatarHora,
    hojeISO,
    inicioDaSemana,
} from "../utils/datas";

import { definirStatusOrdemServico } from "../utils/ordemServicoStatus";

import OrderForm from "./OrderForm";
import TechnicianDrawer from "./TechnicianDrawer";

import type {
    AuthSession,
    Contrato,
    OrdemServico,
    Tecnico,
} from "../types";

type ServiceOrdersScreenProps = {
    sessao: AuthSession;
    contratos: Contrato[];
};

type ModoVisualizacao = "DIA" | "SEMANA";

function obterIniciais(nome: string | null) {
    if (!nome) {
        return "?";
    }

    return nome
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map((parte) => parte.charAt(0))
        .join("")
        .toUpperCase();
}

function compararOrdens(
    ordemA: OrdemServico,
    ordemB: OrdemServico
) {
    if (ordemA.data !== ordemB.data) {
        return ordemA.data.localeCompare(ordemB.data);
    }

    if (ordemA.hora !== ordemB.hora) {
        if (!ordemA.hora) return 1;
        if (!ordemB.hora) return -1;

        return ordemA.hora.localeCompare(ordemB.hora);
    }

    return ordemA.id - ordemB.id;
}

function ServiceOrdersScreen({
                                  sessao,
                                  contratos,
                              }: ServiceOrdersScreenProps) {
    const perfilTecnico =
        sessao.perfil === "TECNICO" ||
        sessao.perfil === "TECNICO_INTERNO";

    const [contratoSelecionado, setContratoSelecionado] =
        useState<number | null>(null);

    const [modoVisualizacao, setModoVisualizacao] =
        useState<ModoVisualizacao>("DIA");

    const [dataReferencia, setDataReferencia] =
        useState(hojeISO());

    const [tecnicosDoContrato, setTecnicosDoContrato] =
        useState<Tecnico[]>([]);

    const [tecnicoSelecionado, setTecnicoSelecionado] =
        useState("todos");

    const [meusOrdens, setMeusOrdens] = useState(false);

    const [ordens, setOrdens] = useState<OrdemServico[]>(
        []
    );

    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState<string | null>(null);

    const [criandoOrdem, setCriandoOrdem] =
        useState(false);

    const [ordemExpandidaId, setOrdemExpandidaId] =
        useState<number | null>(null);

    const [editandoOrdemId, setEditandoOrdemId] =
        useState<number | null>(null);

    const [atribuindoOrdemId, setAtribuindoOrdemId] =
        useState<number | null>(null);

    useEffect(() => {
        if (
            contratoSelecionado === null &&
            contratos.length > 0
        ) {
            setContratoSelecionado(contratos[0].id);
        }
    }, [contratos, contratoSelecionado]);

    useEffect(() => {
        if (perfilTecnico || contratoSelecionado === null) {
            setTecnicosDoContrato([]);
            return;
        }

        buscarTecnicosPorContrato(contratoSelecionado)
            .then((dados) => setTecnicosDoContrato(dados))
            .catch((error: Error) => setErro(error.message));
    }, [contratoSelecionado, perfilTecnico]);

    const carregarOrdens = useCallback(() => {
        if (contratoSelecionado === null) {
            setOrdens([]);
            setCarregando(false);
            return;
        }

        setCarregando(true);
        setErro(null);

        const tecnicoIdParaBusca =
            !perfilTecnico && tecnicoSelecionado !== "todos"
                ? Number(tecnicoSelecionado)
                : undefined;

        const filtros =
            modoVisualizacao === "DIA"
                ? { data: dataReferencia }
                : {
                    dataInicio: inicioDaSemana(
                        dataReferencia
                    ),
                    dataFim: adicionarDias(
                        inicioDaSemana(dataReferencia),
                        6
                    ),
                };

        buscarOrdensServico(contratoSelecionado, {
            ...filtros,
            tecnicoId: tecnicoIdParaBusca,
            meus: perfilTecnico && meusOrdens,
        })
            .then((dados) => setOrdens(dados))
            .catch((error: Error) => setErro(error.message))
            .finally(() => setCarregando(false));
    }, [
        contratoSelecionado,
        modoVisualizacao,
        dataReferencia,
        perfilTecnico,
        tecnicoSelecionado,
        meusOrdens,
    ]);

    useEffect(() => {
        carregarOrdens();
    }, [carregarOrdens]);

    const diasDaSemana = useMemo(() => {
        if (modoVisualizacao !== "SEMANA") {
            return [];
        }

        const inicio = inicioDaSemana(dataReferencia);

        return Array.from({ length: 7 }, (_, indice) =>
            adicionarDias(inicio, indice)
        );
    }, [modoVisualizacao, dataReferencia]);

    const ordensPorDia = useMemo(() => {
        const mapa = new Map<string, OrdemServico[]>();

        for (const ordem of ordens) {
            const lista = mapa.get(ordem.data) ?? [];
            lista.push(ordem);
            mapa.set(ordem.data, lista);
        }

        for (const lista of mapa.values()) {
            lista.sort(compararOrdens);
        }

        return mapa;
    }, [ordens]);

    const ordensDoDia = useMemo(
        () => [...ordens].sort(compararOrdens),
        [ordens]
    );

    function navegar(passos: number) {
        const quantidadeDias =
            modoVisualizacao === "DIA" ? passos : passos * 7;

        setDataReferencia((atual) =>
            adicionarDias(atual, quantidadeDias)
        );
    }

    function alternarExpansao(ordemId: number) {
        setEditandoOrdemId(null);
        setAtribuindoOrdemId(null);

        setOrdemExpandidaId((atual) =>
            atual === ordemId ? null : ordemId
        );
    }

    function ordemSalva() {
        setCriandoOrdem(false);
        setEditandoOrdemId(null);
        carregarOrdens();
    }

    function tecnicoAtribuido() {
        setAtribuindoOrdemId(null);
        carregarOrdens();
    }

    async function iniciarAtendimento(
        ordemServico: OrdemServico
    ) {
        if (contratoSelecionado === null) {
            return;
        }

        setErro(null);

        try {
            const resultado =
                await iniciarAtendimentoComConfirmacao(
                    contratoSelecionado,
                    ordemServico.id
                );

            if (resultado === null) {
                return;
            }

            carregarOrdens();
        } catch (error) {
            setErro(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        }
    }

    async function finalizarAtendimento(
        ordemServico: OrdemServico
    ) {
        if (contratoSelecionado === null) {
            return;
        }

        setErro(null);

        try {
            await finalizarAtendimentoApi(
                contratoSelecionado,
                ordemServico.id
            );

            carregarOrdens();
        } catch (error) {
            setErro(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        }
    }

    function renderizarCard(ordemServico: OrdemServico) {
        const status = definirStatusOrdemServico(
            ordemServico
        );

        const aberta = ordemExpandidaId === ordemServico.id;
        const editando = editandoOrdemId === ordemServico.id;
        const atribuindo =
            atribuindoOrdemId === ordemServico.id;

        const atendimentoNaoIniciado =
            !ordemServico.dataCheckIn &&
            !ordemServico.dataCheckOut;

        return (
            <article
                key={ordemServico.id}
                className={`order-card order-card-${status.classe} ${
                    aberta ? "expanded" : ""
                }`}
            >
                <button
                    type="button"
                    className="order-collapse-trigger"
                    onClick={() =>
                        alternarExpansao(ordemServico.id)
                    }
                    aria-expanded={aberta}
                >
                    <div className="order-collapsed-top">
                        <div className="order-collapsed-identity">
                            <span className="label">
                                OS {ordemServico.numeroOrdemServico}
                                {" · "}
                                {formatarHora(
                                    ordemServico.hora
                                )}
                            </span>

                            <h3>
                                {ordemServico.unidadeAtendimentoNome}
                            </h3>
                        </div>

                        <span
                            className={`order-status order-status-${status.classe}`}
                        >
                            <span className="order-status-dot" />
                            {status.rotulo}
                        </span>
                    </div>
                </button>

                {aberta && (
                    <div className="order-expanded-content">
                        <div className="order-information-grid">
                            <div className="order-information-field technician-field">
                                <span className="label">
                                    Técnico responsável
                                </span>

                                <div className="order-technician">
                                    <span className="order-technician-avatar">
                                        {obterIniciais(
                                            ordemServico.tecnicoNome
                                        )}
                                    </span>

                                    <strong>
                                        {ordemServico.tecnicoNome ??
                                            "Não atribuído"}
                                    </strong>
                                </div>
                            </div>

                            <div className="order-information-field">
                                <span className="label">
                                    Chamado vinculado
                                </span>

                                <strong>
                                    {ordemServico.numeroChamado
                                        ? `OSTI ${ordemServico.numeroChamado}`
                                        : "Avulsa (sem chamado)"}
                                </strong>
                            </div>

                            <div className="order-information-field">
                                <span className="label">
                                    Patrimônio
                                </span>

                                <strong>
                                    {ordemServico.numeroPatrimonio ??
                                        "Não informado"}
                                </strong>
                            </div>

                            <div className="order-information-field">
                                <span className="label">
                                    Check-in
                                </span>

                                <strong>
                                    {ordemServico.dataCheckIn
                                        ? new Date(
                                            ordemServico.dataCheckIn
                                        ).toLocaleString(
                                            "pt-BR",
                                            {
                                                dateStyle:
                                                    "short",
                                                timeStyle:
                                                    "short",
                                            }
                                        )
                                        : "Aguardando"}
                                </strong>
                            </div>

                            <div className="order-information-field">
                                <span className="label">
                                    Check-out
                                </span>

                                <strong>
                                    {ordemServico.dataCheckOut
                                        ? new Date(
                                            ordemServico.dataCheckOut
                                        ).toLocaleString(
                                            "pt-BR",
                                            {
                                                dateStyle:
                                                    "short",
                                                timeStyle:
                                                    "short",
                                            }
                                        )
                                        : "Aguardando"}
                                </strong>
                            </div>
                        </div>

                        {ordemServico.descricao && (
                            <p className="order-description">
                                {ordemServico.descricao}
                            </p>
                        )}

                        {editando && contratoSelecionado && (
                            <OrderForm
                                contratoId={
                                    contratoSelecionado
                                }
                                ordemServico={ordemServico}
                                aoCancelar={() =>
                                    setEditandoOrdemId(null)
                                }
                                aoSalvo={ordemSalva}
                            />
                        )}

                        {atribuindo && contratoSelecionado && (
                            <TechnicianDrawer
                                contratoId={
                                    contratoSelecionado
                                }
                                ordemServico={ordemServico}
                                aoCancelar={() =>
                                    setAtribuindoOrdemId(
                                        null
                                    )
                                }
                                aoTecnicoAtribuido={
                                    tecnicoAtribuido
                                }
                            />
                        )}

                        {!editando && !atribuindo && (
                            <div className="order-actions">
                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={() =>
                                        setEditandoOrdemId(
                                            ordemServico.id
                                        )
                                    }
                                >
                                    Editar dados
                                </button>

                                {atendimentoNaoIniciado && (
                                    <>
                                        <button
                                            type="button"
                                            className="secondary-button"
                                            onClick={() =>
                                                setAtribuindoOrdemId(
                                                    ordemServico.id
                                                )
                                            }
                                        >
                                            {ordemServico.tecnicoId ===
                                            null
                                                ? "Atribuir técnico"
                                                : "Alterar técnico"}
                                        </button>

                                        {ordemServico.tecnicoId !==
                                            null && (
                                                <button
                                                    type="button"
                                                    className="primary-button"
                                                    onClick={() =>
                                                        iniciarAtendimento(
                                                            ordemServico
                                                        )
                                                    }
                                                >
                                                    Iniciar
                                                    atendimento
                                                </button>
                                            )}
                                    </>
                                )}

                                {ordemServico.dataCheckIn &&
                                    !ordemServico.dataCheckOut && (
                                        <button
                                            type="button"
                                            className="danger-button"
                                            onClick={() =>
                                                finalizarAtendimento(
                                                    ordemServico
                                                )
                                            }
                                        >
                                            Finalizar atendimento
                                        </button>
                                    )}
                            </div>
                        )}
                    </div>
                )}
            </article>
        );
    }

    if (contratos.length === 0) {
        return (
            <main className="page">
                <section className="card empty-detail">
                    <h2>Nenhum contrato disponível</h2>

                    <p>
                        Não há contratos cadastrados para
                        exibir ordens de serviço.
                    </p>
                </section>
            </main>
        );
    }

    return (
        <main className="page orders-screen">
            <header className="app-header compact-app-header">
                <div className="compact-header-main">
                    <div className="app-header-title">
                        <span className="label">
                            Execução operacional
                        </span>

                        <div className="app-header-title-row">
                            <h1>Ordens de serviço</h1>

                            <span
                                className="header-ticket-count"
                                title={`${ordens.length} ordens no período`}
                            >
                                {ordens.length}
                            </span>
                        </div>
                    </div>

                    {!perfilTecnico && (
                        <div className="compact-header-actions">
                            <button
                                type="button"
                                className="primary-button"
                                onClick={() => {
                                    setErro(null);
                                    setCriandoOrdem(true);
                                }}
                            >
                                + Nova OS
                            </button>
                        </div>
                    )}
                </div>
            </header>

            {erro && <div className="error">{erro}</div>}

            <section className="card orders-toolbar">
                <label className="feed-inline-select feed-contract-button">
                    <span className="sr-only">
                        Contrato
                    </span>

                    <select
                        value={contratoSelecionado ?? ""}
                        onChange={(event) =>
                            setContratoSelecionado(
                                Number(event.target.value)
                            )
                        }
                        aria-label="Selecionar contrato"
                    >
                        {contratos.map((contrato) => (
                            <option
                                key={contrato.id}
                                value={contrato.id}
                            >
                                {contrato.cidade}
                            </option>
                        ))}
                    </select>
                </label>

                {!perfilTecnico && (
                    <label className="feed-inline-select feed-technician-button">
                        <span className="sr-only">
                            Técnico
                        </span>

                        <select
                            value={tecnicoSelecionado}
                            onChange={(event) =>
                                setTecnicoSelecionado(
                                    event.target.value
                                )
                            }
                            aria-label="Filtrar por técnico"
                        >
                            <option value="todos">
                                Técnicos
                            </option>

                            {tecnicosDoContrato.map(
                                (tecnico) => (
                                    <option
                                        key={tecnico.id}
                                        value={tecnico.id}
                                    >
                                        {tecnico.nome}
                                    </option>
                                )
                            )}
                        </select>
                    </label>
                )}

                {perfilTecnico && (
                    <label
                        className="feed-inline-select feed-my-tickets-toggle"
                        title="Mostrar somente minhas ordens de serviço"
                    >
                        <input
                            type="checkbox"
                            checked={meusOrdens}
                            onChange={() =>
                                setMeusOrdens((atual) => !atual)
                            }
                        />

                        <span>Minhas OS</span>
                    </label>
                )}

                <div className="orders-view-toggle">
                    <button
                        type="button"
                        className={
                            modoVisualizacao === "DIA"
                                ? "active"
                                : ""
                        }
                        onClick={() =>
                            setModoVisualizacao("DIA")
                        }
                    >
                        Dia
                    </button>

                    <button
                        type="button"
                        className={
                            modoVisualizacao === "SEMANA"
                                ? "active"
                                : ""
                        }
                        onClick={() =>
                            setModoVisualizacao("SEMANA")
                        }
                    >
                        Semana
                    </button>
                </div>

                <div className="orders-date-nav">
                    <button
                        type="button"
                        onClick={() => navegar(-1)}
                        aria-label="Período anterior"
                    >
                        ‹
                    </button>

                    <button
                        type="button"
                        className="orders-date-today"
                        onClick={() =>
                            setDataReferencia(hojeISO())
                        }
                    >
                        Hoje
                    </button>

                    <button
                        type="button"
                        onClick={() => navegar(1)}
                        aria-label="Próximo período"
                    >
                        ›
                    </button>

                    <strong className="orders-date-label">
                        {modoVisualizacao === "DIA"
                            ? formatarDiaSemana(
                                dataReferencia
                            ) +
                            " · " +
                            formatarDataCurta(
                                dataReferencia
                            )
                            : `${formatarDataCurta(
                                inicioDaSemana(
                                    dataReferencia
                                )
                            )} – ${formatarDataCurta(
                                adicionarDias(
                                    inicioDaSemana(
                                        dataReferencia
                                    ),
                                    6
                                )
                            )}`}
                    </strong>
                </div>
            </section>

            {criandoOrdem && contratoSelecionado && (
                <OrderForm
                    contratoId={contratoSelecionado}
                    aoCancelar={() =>
                        setCriandoOrdem(false)
                    }
                    aoSalvo={ordemSalva}
                />
            )}

            {carregando ? (
                <section className="card">
                    <p>Carregando ordens de serviço...</p>
                </section>
            ) : modoVisualizacao === "DIA" ? (
                ordensDoDia.length === 0 ? (
                    <section className="card service-orders-empty">
                        <div className="service-orders-empty-icon">
                            OS
                        </div>

                        <h3>
                            Nenhuma ordem de serviço neste
                            dia
                        </h3>

                        <p>
                            Ajuste os filtros ou crie uma
                            nova ordem.
                        </p>
                    </section>
                ) : (
                    <div className="orders">
                        {ordensDoDia.map(renderizarCard)}
                    </div>
                )
            ) : (
                <div className="orders-week-grid">
                    {diasDaSemana.map((dia) => {
                        const ordensDesteDia =
                            ordensPorDia.get(dia) ?? [];

                        return (
                            <section
                                key={dia}
                                className="orders-day-group"
                            >
                                <header className="orders-day-group-header">
                                    <strong>
                                        {formatarDiaSemana(
                                            dia
                                        )}
                                    </strong>

                                    <span>
                                        {formatarDataCurta(
                                            dia
                                        )}
                                    </span>
                                </header>

                                {ordensDesteDia.length ===
                                0 ? (
                                    <p className="orders-day-group-empty">
                                        Nenhuma OS
                                    </p>
                                ) : (
                                    <div className="orders">
                                        {ordensDesteDia.map(
                                            renderizarCard
                                        )}
                                    </div>
                                )}
                            </section>
                        );
                    })}
                </div>
            )}
        </main>
    );
}

export default ServiceOrdersScreen;
