import {
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    adicionarComentario as adicionarComentarioApi,
    buscarChamados,
    buscarComentarios,
    buscarContratos,
    buscarDetalhesChamado,
    buscarOrdensServico,
    finalizarAtendimento as finalizarAtendimentoApi,
    iniciarAtendimento as iniciarAtendimentoApi,
} from "./api";

import CommentTimeline from "./components/CommentTimeline";
import CreateTicketForm from "./components/CreateTicketForm";
import {
    DetailSkeleton,
} from "./components/LoadingSkeletons";
import ServiceOrderList from "./components/ServiceOrderList";
import TicketDetails from "./components/TicketDetails";
import TicketFeed from "./components/TicketFeed";

import type {
    Chamado,
    ComentarioChamado,
    Contrato,
    OrdemServico,
} from "./types";

import "./App.css";

type FiltroStatus =
    | Chamado["status"]
    | "TODOS";

type OrdemData =
    | "MAIS_RECENTES"
    | "MAIS_ANTIGOS";

function App() {
    const [contratos, setContratos] =
        useState<Contrato[]>([]);

    const [
        contratoSelecionado,
        setContratoSelecionado,
    ] = useState("todos");

    const [
        filtroStatus,
        setFiltroStatus,
    ] = useState<FiltroStatus>("TODOS");

    const [
        ordemData,
        setOrdemData,
    ] = useState<OrdemData>(
        "MAIS_RECENTES"
    );



    const [chamados, setChamados] =
        useState<Chamado[]>([]);

    const [
        chamadoSelecionado,
        setChamadoSelecionado,
    ] = useState<Chamado | null>(null);

    const [
        ordensServico,
        setOrdensServico,
    ] = useState<OrdemServico[]>([]);

    const [
        comentarios,
        setComentarios,
    ] = useState<ComentarioChamado[]>([]);

    const [
        novoComentarioTexto,
        setNovoComentarioTexto,
    ] = useState("");

    const [
        novaComentarioOrdemServicoId,
        setNovaComentarioOrdemServicoId,
    ] = useState("sem-os");

    const [
        carregandoFeed,
        setCarregandoFeed,
    ] = useState(true);

    const [
        carregandoDetalhe,
        setCarregandoDetalhe,
    ] = useState(false);

    const [
        criandoChamado,
        setCriandoChamado,
    ] = useState(false);

    const [erro, setErro] =
        useState<string | null>(null);

    const chamadosFiltrados =
        useMemo(() => {
            const resultado =
                filtroStatus === "TODOS"
                    ? [...chamados]
                    : chamados.filter(
                        (chamado) =>
                            chamado.status ===
                            filtroStatus
                    );

            resultado.sort(
                (chamadoA, chamadoB) => {
                    const dataA =
                        new Date(
                            chamadoA.dataAbertura
                        ).getTime();

                    const dataB =
                        new Date(
                            chamadoB.dataAbertura
                        ).getTime();

                    const timestampA =
                        Number.isNaN(dataA)
                            ? 0
                            : dataA;

                    const timestampB =
                        Number.isNaN(dataB)
                            ? 0
                            : dataB;

                    if (
                        ordemData ===
                        "MAIS_ANTIGOS"
                    ) {
                        return (
                            timestampA -
                            timestampB
                        );
                    }

                    return (
                        timestampB -
                        timestampA
                    );
                }
            );

            return resultado;
        }, [
            chamados,
            filtroStatus,
            ordemData,
        ]);


    useEffect(() => {
        buscarContratos()
            .then((data) => {
                setContratos(data);
            })
            .catch((error: Error) => {
                setErro(error.message);
            });
    }, []);

    useEffect(() => {
        carregarChamados();
    }, [contratoSelecionado]);



    function carregarChamados(
        limparDetalhe = true
    ) {
        setCarregandoFeed(true);
        setErro(null);

        if (limparDetalhe) {
            setChamadoSelecionado(null);
            setOrdensServico([]);
            setComentarios([]);
        }

        buscarChamados(contratoSelecionado)
            .then((data) => {
                setChamados(data);
            })
            .catch((error: Error) => {
                setErro(error.message);
            })
            .finally(() => {
                setCarregandoFeed(false);
            });
    }

    function selecionarChamado(
        chamado: Chamado
    ) {
        setCarregandoDetalhe(true);
        setErro(null);

        setNovoComentarioTexto("");

        setNovaComentarioOrdemServicoId(
            "sem-os"
        );

        Promise.all([
            buscarDetalhesChamado(
                chamado.contratoId,
                chamado.id
            ),

            buscarOrdensServico(
                chamado.contratoId,
                chamado.id
            ),

            buscarComentarios(
                chamado.contratoId,
                chamado.id
            ),
        ])
            .then(
                ([
                     chamadoData,
                     ordensServicoData,
                     comentariosData,
                 ]) => {
                    setChamadoSelecionado(
                        chamadoData
                    );

                    setOrdensServico(
                        ordensServicoData
                    );

                    setComentarios(
                        comentariosData
                    );
                }
            )
            .catch((error: Error) => {
                setErro(error.message);
            })
            .finally(() => {
                setCarregandoDetalhe(false);
            });
    }

    function chamadoCriado(
        chamado: Chamado
    ) {
        setCriandoChamado(false);

        const contratoDoChamado =
            String(chamado.contratoId);

        if (
            contratoSelecionado ===
            "todos" ||
            contratoSelecionado ===
            contratoDoChamado
        ) {
            carregarChamados();
            return;
        }

        setContratoSelecionado(
            contratoDoChamado
        );
    }

    function ordemServicoCriada() {
        if (!chamadoSelecionado) {
            return;
        }

        carregarChamados(false);

        selecionarChamado(
            chamadoSelecionado
        );
    }

    async function iniciarAtendimento(
        ordemServico: OrdemServico
    ) {
        if (!chamadoSelecionado) {
            return;
        }

        setErro(null);

        try {
            await iniciarAtendimentoApi(
                chamadoSelecionado.contratoId,
                chamadoSelecionado.id,
                ordemServico.id
            );

            carregarChamados(false);

            selecionarChamado(
                chamadoSelecionado
            );
        } catch (error) {
            setErro(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        }
    }

    function statusChamadoAtualizado(
        chamadoAtualizado: Chamado
    ) {
        setChamadoSelecionado(
            chamadoAtualizado
        );

        setChamados((chamadosAtuais) =>
            chamadosAtuais.map(
                (chamado) =>
                    chamado.id ===
                    chamadoAtualizado.id
                        ? chamadoAtualizado
                        : chamado
            )
        );
    }

    async function finalizarAtendimento(
        ordemServico: OrdemServico
    ) {
        if (!chamadoSelecionado) {
            return;
        }

        setErro(null);

        try {
            await finalizarAtendimentoApi(
                chamadoSelecionado.contratoId,
                chamadoSelecionado.id,
                ordemServico.id
            );

            carregarChamados(false);

            selecionarChamado(
                chamadoSelecionado
            );
        } catch (error) {
            setErro(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        }
    }

    async function adicionarComentario() {
        if (!chamadoSelecionado) {
            return;
        }

        const texto =
            novoComentarioTexto.trim();

        if (!texto) {
            setErro(
                "O comentário não pode estar vazio"
            );
            return;
        }

        setErro(null);

        const ordemServicoId =
            novaComentarioOrdemServicoId ===
            "sem-os"
                ? null
                : Number(
                    novaComentarioOrdemServicoId
                );

        try {
            await adicionarComentarioApi(
                chamadoSelecionado.contratoId,
                chamadoSelecionado.id,
                1,
                ordemServicoId,
                texto
            );

            setNovoComentarioTexto("");

            setNovaComentarioOrdemServicoId(
                "sem-os"
            );

            selecionarChamado(
                chamadoSelecionado
            );
        } catch (error) {
            setErro(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        }
    }

    return (
        <div className="app-shell">
            <aside className="app-sidebar">
                <div className="sidebar-brand">
                    <div className="sidebar-brand-mark">
                        SD
                    </div>

                    <div className="sidebar-brand-text">
                        <strong>
                            Smart Dispatch
                        </strong>

                        <span>
                            Operação inteligente
                        </span>
                    </div>
                </div>

                <nav className="sidebar-navigation">
                    <span className="sidebar-section-label">
                        Operação
                    </span>

                    <div className="sidebar-nav-item active">
                        <span className="sidebar-nav-icon">
                            ◆
                        </span>

                        <span>Chamados</span>
                    </div>

                    <div className="sidebar-nav-item disabled">
                        <span className="sidebar-nav-icon">
                            ◉
                        </span>

                        <span>Técnicos</span>

                        <small>Em breve</small>
                    </div>

                    <div className="sidebar-nav-item disabled">
                        <span className="sidebar-nav-icon">
                            ◈
                        </span>

                        <span>Unidades</span>

                        <small>Em breve</small>
                    </div>

                    <div className="sidebar-nav-item disabled">
                        <span className="sidebar-nav-icon">
                            ▣
                        </span>

                        <span>Contratos</span>

                        <small>Em breve</small>
                    </div>
                </nav>

                <div className="sidebar-footer">
                    <div className="sidebar-profile-avatar">
                        CTO
                    </div>

                    <div className="sidebar-profile-content">
                        <strong>
                            Visão operacional
                        </strong>

                        <span>
                            Centro de Tecnologia
                            Operacional
                        </span>
                    </div>
                </div>
            </aside>

            <div className="app-content">
                <header className="app-header compact-app-header">
                    <div className="compact-header-main">
                        <div className="app-header-title">
            <span className="label">
                Central de operações
            </span>

                            <div className="app-header-title-row">
                                <h1>
                                    Chamados
                                </h1>

                                <span
                                    className="header-ticket-count"
                                    title={`${chamadosFiltrados.length} de ${chamados.length} chamados`}
                                >
                    {
                        chamadosFiltrados.length
                    }
                </span>
                            </div>
                        </div>

                        {!criandoChamado && (
                            <div className="compact-header-actions">
                                <button
                                    type="button"
                                    className="primary-button"
                                    onClick={() => {
                                        setErro(null);

                                        setCriandoChamado(
                                            true
                                        );
                                    }}
                                >
                                    + Novo chamado
                                </button>
                            </div>
                        )}
                    </div>
                </header>

                <main className="page">
                    {erro && (
                        <div className="error">
                            {erro}
                        </div>
                    )}

                    {criandoChamado ? (
                        <div className="create-ticket-area">
                            <CreateTicketForm
                                contratos={
                                    contratos
                                }
                                aoCancelar={() =>
                                    setCriandoChamado(
                                        false
                                    )
                                }
                                aoChamadoCriado={
                                    chamadoCriado
                                }
                            />
                        </div>
                    ) : (
                        <div className="layout">
                            <TicketFeed
                                chamados={
                                    chamadosFiltrados
                                }
                                contratos={
                                    contratos
                                }
                                contratoSelecionado={
                                    contratoSelecionado
                                }
                                filtroStatus={
                                    filtroStatus
                                }
                                ordemData={
                                    ordemData
                                }
                                chamadoSelecionado={
                                    chamadoSelecionado
                                }
                                carregando={
                                    carregandoFeed
                                }
                                aoAlterarContrato={
                                    setContratoSelecionado
                                }
                                aoAlterarStatus={
                                    setFiltroStatus
                                }
                                aoAlternarOrdenacao={() =>
                                    setOrdemData(
                                        ordemAtual =>
                                            ordemAtual ===
                                            "MAIS_RECENTES"
                                                ? "MAIS_ANTIGOS"
                                                : "MAIS_RECENTES"
                                    )
                                }
                                aoSelecionarChamado={
                                    selecionarChamado
                                }
                            />

                            <section className="detail-area">
                                {carregandoDetalhe && (
                                    <DetailSkeleton />
                                )}

                                {!carregandoDetalhe &&
                                    !chamadoSelecionado && (
                                        <section className="card empty-detail">
                                            <div className="empty-detail-icon">
                                                ◆
                                            </div>

                                            <h2>
                                                Selecione
                                                um chamado
                                            </h2>

                                            <p>
                                                Escolha um
                                                chamado no
                                                feed para
                                                visualizar
                                                os detalhes
                                                da operação.
                                            </p>
                                        </section>
                                    )}

                                {!carregandoDetalhe &&
                                    chamadoSelecionado && (
                                        <>
                                            <TicketDetails
                                                chamado={
                                                    chamadoSelecionado
                                                }
                                                aoChamadoAtualizado={
                                                    statusChamadoAtualizado
                                                }
                                            />

                                            <ServiceOrderList
                                                chamado={
                                                    chamadoSelecionado
                                                }
                                                ordensServico={
                                                    ordensServico
                                                }
                                                aoIniciarAtendimento={
                                                    iniciarAtendimento
                                                }
                                                aoFinalizarAtendimento={
                                                    finalizarAtendimento
                                                }
                                                aoOrdemCriada={
                                                    ordemServicoCriada
                                                }
                                            />

                                            <CommentTimeline
                                                comentarios={
                                                    comentarios
                                                }
                                                ordensServico={
                                                    ordensServico
                                                }
                                                novoComentarioTexto={
                                                    novoComentarioTexto
                                                }
                                                novaComentarioOrdemServicoId={
                                                    novaComentarioOrdemServicoId
                                                }
                                                aoAlterarTexto={
                                                    setNovoComentarioTexto
                                                }
                                                aoAlterarOrdemServico={
                                                    setNovaComentarioOrdemServicoId
                                                }
                                                aoAdicionarComentario={
                                                    adicionarComentario
                                                }
                                            />
                                        </>
                                    )}
                            </section>
                        </div>
                    )}
                </main>
            </div>
        </div>
    );
}

export default App;