import { useEffect, useState } from "react";

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

function App() {
    const [contratos, setContratos] = useState<Contrato[]>([]);
    const [contratoSelecionado, setContratoSelecionado] =
        useState("todos");

    const [chamados, setChamados] = useState<Chamado[]>([]);
    const [chamadoSelecionado, setChamadoSelecionado] =
        useState<Chamado | null>(null);

    const [ordensServico, setOrdensServico] =
        useState<OrdemServico[]>([]);

    const [comentarios, setComentarios] =
        useState<ComentarioChamado[]>([]);

    const [novoComentarioTexto, setNovoComentarioTexto] =
        useState("");

    const [
        novaComentarioOrdemServicoId,
        setNovaComentarioOrdemServicoId,
    ] = useState("sem-os");

    const [carregandoFeed, setCarregandoFeed] =
        useState(true);

    const [carregandoDetalhe, setCarregandoDetalhe] =
        useState(false);

    const [criandoChamado, setCriandoChamado] =
        useState(false);

    const [erro, setErro] =
        useState<string | null>(null);

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

    function selecionarChamado(chamado: Chamado) {
        setCarregandoDetalhe(true);
        setErro(null);

        setNovoComentarioTexto("");
        setNovaComentarioOrdemServicoId("sem-os");

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
                    setChamadoSelecionado(chamadoData);
                    setOrdensServico(ordensServicoData);
                    setComentarios(comentariosData);
                }
            )
            .catch((error: Error) => {
                setErro(error.message);
            })
            .finally(() => {
                setCarregandoDetalhe(false);
            });
    }

    function chamadoCriado(chamado: Chamado) {
        setCriandoChamado(false);

        const contratoDoChamado =
            String(chamado.contratoId);

        if (
            contratoSelecionado === "todos" ||
            contratoSelecionado === contratoDoChamado
        ) {
            carregarChamados();
            return;
        }

        setContratoSelecionado(contratoDoChamado);
    }



    function ordemServicoCriada() {
        if (!chamadoSelecionado) {
            return;
        }

        carregarChamados(false);
        selecionarChamado(chamadoSelecionado);
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
            selecionarChamado(chamadoSelecionado);
        } catch (error) {
            if (error instanceof Error) {
                setErro(error.message);
                return;
            }

            setErro("Ocorreu um erro inesperado");
        }
    }

    function statusChamadoAtualizado(
        chamadoAtualizado: Chamado
    ) {
        setChamadoSelecionado(
            chamadoAtualizado
        );

        setChamados((chamadosAtuais) =>
            chamadosAtuais.map((chamado) =>
                chamado.id === chamadoAtualizado.id
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
            selecionarChamado(chamadoSelecionado);
        } catch (error) {
            if (error instanceof Error) {
                setErro(error.message);
                return;
            }

            setErro("Ocorreu um erro inesperado");
        }
    }

    async function adicionarComentario() {
        if (!chamadoSelecionado) {
            return;
        }

        const texto = novoComentarioTexto.trim();

        if (!texto) {
            setErro("O comentário não pode estar vazio");
            return;
        }

        setErro(null);

        const ordemServicoId =
            novaComentarioOrdemServicoId === "sem-os"
                ? null
                : Number(novaComentarioOrdemServicoId);

        try {
            await adicionarComentarioApi(
                chamadoSelecionado.contratoId,
                chamadoSelecionado.id,
                1,
                ordemServicoId,
                texto
            );

            setNovoComentarioTexto("");
            setNovaComentarioOrdemServicoId("sem-os");

            selecionarChamado(chamadoSelecionado);
        } catch (error) {
            if (error instanceof Error) {
                setErro(error.message);
                return;
            }

            setErro("Ocorreu um erro inesperado");
        }
    }

    return (
        <main className="page">
            <header className="app-header">
                <div>
                    <span className="label">
                        Smart Dispatch
                    </span>

                    <h1>Feed de chamados</h1>
                </div>

                <div className="header-actions">
                    {!criandoChamado && (
                        <>
                            <select
                                className="select"
                                value={contratoSelecionado}
                                onChange={(event) =>
                                    setContratoSelecionado(
                                        event.target.value
                                    )
                                }
                            >
                                <option value="todos">
                                    Todos os contratos
                                </option>

                                {contratos.map((contrato) => (
                                    <option
                                        key={contrato.id}
                                        value={contrato.id}
                                    >
                                        {contrato.cidade}
                                    </option>
                                ))}
                            </select>

                            <button
                                type="button"
                                className="primary-button"
                                onClick={() => {
                                    setErro(null);
                                    setCriandoChamado(true);
                                }}
                            >
                                Novo chamado
                            </button>
                        </>
                    )}
                </div>
            </header>

            {erro && (
                <div className="error">
                    {erro}
                </div>
            )}

            {criandoChamado ? (
                <div className="create-ticket-area">
                    <CreateTicketForm
                        contratos={contratos}
                        aoCancelar={() =>
                            setCriandoChamado(false)
                        }
                        aoChamadoCriado={chamadoCriado}
                    />
                </div>
            ) : (
                <div className="layout">
                    <TicketFeed
                        chamados={chamados}
                        chamadoSelecionado={
                            chamadoSelecionado
                        }
                        carregando={carregandoFeed}
                        aoSelecionarChamado={
                            selecionarChamado
                        }
                    />

                    <section className="detail-area">
                        {carregandoDetalhe && (
                            <section className="card">
                                <p>
                                    Carregando detalhes do chamado...
                                </p>
                            </section>
                        )}

                        {!carregandoDetalhe &&
                            !chamadoSelecionado && (
                                <section className="card empty-detail">
                                    <h2>
                                        Selecione um chamado
                                    </h2>

                                    <p>
                                        Escolha um chamado no
                                        feed para visualizar os
                                        detalhes.
                                    </p>
                                </section>
                            )}

                        {!carregandoDetalhe &&
                            chamadoSelecionado && (
                                <>
                                    <TicketDetails
                                        chamado={chamadoSelecionado}
                                        aoStatusAtualizado={
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
    );
}

export default App;