import { useEffect, useState } from "react";
import type {
    Chamado,
    ComentarioChamado,
    Contrato,
    ErroResponse,
    OrdemServico,
} from "./types";
import "./App.css";

const API_BASE_URL = "http://localhost:8080";

function formatarData(data: string | null) {
    if (!data) {
        return "Não informado";
    }

    return new Date(data).toLocaleString("pt-BR");
}

function definirStatusOrdemServico(ordemServico: OrdemServico) {
    if (ordemServico.dataCheckIn && ordemServico.dataCheckOut) {
        return "Encerrada";
    }

    if (ordemServico.dataCheckIn && !ordemServico.dataCheckOut) {
        return "Em atendimento";
    }

    return "Aguardando início";
}

function App() {
    const [contratos, setContratos] = useState<Contrato[]>([]);
    const [contratoSelecionado, setContratoSelecionado] = useState("todos");

    const [chamados, setChamados] = useState<Chamado[]>([]);
    const [chamadoSelecionado, setChamadoSelecionado] =
        useState<Chamado | null>(null);

    const [ordensServico, setOrdensServico] = useState<OrdemServico[]>([]);
    const [comentarios, setComentarios] = useState<ComentarioChamado[]>([]);
    const [novoComentarioTexto, setNovoComentarioTexto] = useState("");
    const [novaComentarioOrdemServicoId, setNovaComentarioOrdemServicoId] =
        useState("sem-os");

    const [carregandoFeed, setCarregandoFeed] = useState(true);
    const [carregandoDetalhe, setCarregandoDetalhe] = useState(false);
    const [erro, setErro] = useState<string | null>(null);

    useEffect(() => {
        fetch(`${API_BASE_URL}/contratos`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Erro ao buscar contratos");
                }

                return response.json();
            })
            .then((data: Contrato[]) => {
                setContratos(data);
            })
            .catch((error) => {
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

        const url =
            contratoSelecionado === "todos"
                ? `${API_BASE_URL}/chamados`
                : `${API_BASE_URL}/chamados?contratoId=${contratoSelecionado}`;

        fetch(url)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Erro ao buscar chamados");
                }

                return response.json();
            })
            .then((data: Chamado[]) => {
                setChamados(data);
            })
            .catch((error) => {
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
            fetch(
                `${API_BASE_URL}/contratos/${chamado.contratoId}/chamados/${chamado.id}`
            ),
            fetch(
                `${API_BASE_URL}/contratos/${chamado.contratoId}/chamados/${chamado.id}/ordens-servico`
            ),
            fetch(
                `${API_BASE_URL}/contratos/${chamado.contratoId}/chamados/${chamado.id}/comentarios`
            ),
        ])
            .then(([chamadoResponse, ordensResponse, comentariosResponse]) => {
                if (!chamadoResponse.ok) {
                    throw new Error("Erro ao buscar detalhes do chamado");
                }

                if (!ordensResponse.ok) {
                    throw new Error("Erro ao buscar ordens de serviço");
                }

                if (!comentariosResponse.ok) {
                    throw new Error("Erro ao buscar comentários");
                }

                return Promise.all([
                    chamadoResponse.json(),
                    ordensResponse.json(),
                    comentariosResponse.json(),
                ]);
            })
            .then(
                ([chamadoData, ordensData, comentariosData]: [
                    Chamado,
                    OrdemServico[],
                    ComentarioChamado[]
                ]) => {
                    setChamadoSelecionado(chamadoData);
                    setOrdensServico(ordensData);
                    setComentarios(comentariosData);
                }
            )
            .catch((error) => {
                setErro(error.message);
            })
            .finally(() => {
                setCarregandoDetalhe(false);
            });
    }

    async function extrairMensagemErro(
        response: Response
    ) {
        try {
            const erroResponse: ErroResponse = await response.json();

            return erroResponse.mensagem;
        } catch {
            return "Ocorreu um erro inesperado";
        }
    }

    async function iniciarAtendimento(
        ordemServico: OrdemServico
    ) {
        if (!chamadoSelecionado) {
            return;
        }

        setErro(null);

        const response = await fetch(
            `${API_BASE_URL}/contratos/${chamadoSelecionado.contratoId}/chamados/${chamadoSelecionado.id}/ordens-servico/${ordemServico.id}/check-in`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    encerrarCheckInAnterior: false,
                }),
            }
        );

        if (!response.ok) {
            const mensagemErro = await extrairMensagemErro(response);
            setErro(mensagemErro);
            return;
        }

        carregarChamados(false);
        selecionarChamado(chamadoSelecionado);
    }

    async function finalizarAtendimento(
        ordemServico: OrdemServico
    ) {
        if (!chamadoSelecionado) {
            return;
        }

        setErro(null);

        const response = await fetch(
            `${API_BASE_URL}/contratos/${chamadoSelecionado.contratoId}/chamados/${chamadoSelecionado.id}/ordens-servico/${ordemServico.id}/check-out`,
            {
                method: "POST",
            }
        );

        if (!response.ok) {
            const mensagemErro = await extrairMensagemErro(response);
            setErro(mensagemErro);
            return;
        }

        carregarChamados(false);
        selecionarChamado(chamadoSelecionado);
    }

    async function adicionarComentario() {
        if (!chamadoSelecionado) {
            return;
        }

        if (!novoComentarioTexto.trim()) {
            setErro("O comentário não pode estar vazio");
            return;
        }

        setErro(null);

        const ordemServicoId =
            novaComentarioOrdemServicoId === "sem-os"
                ? null
                : Number(novaComentarioOrdemServicoId);

        const response = await fetch(
            `${API_BASE_URL}/contratos/${chamadoSelecionado.contratoId}/chamados/${chamadoSelecionado.id}/comentarios`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    autorId: 1,
                    ordemServicoId,
                    texto: novoComentarioTexto.trim(),
                }),
            }
        );

        if (!response.ok) {
            const mensagemErro = await extrairMensagemErro(response);
            setErro(mensagemErro);
            return;
        }

        setNovoComentarioTexto("");
        setNovaComentarioOrdemServicoId("sem-os");

        selecionarChamado(chamadoSelecionado);
    }

    return (
        <main className="page">
            <header className="app-header">
                <div>
                    <span className="label">Smart Dispatch</span>
                    <h1>Feed de chamados</h1>
                </div>

                <select
                    className="select"
                    value={contratoSelecionado}
                    onChange={(event) =>
                        setContratoSelecionado(event.target.value)
                    }
                >
                    <option value="todos">Todos os contratos</option>

                    {contratos.map((contrato) => (
                        <option key={contrato.id} value={contrato.id}>
                            {contrato.cidade}
                        </option>
                    ))}
                </select>
            </header>

            {erro && <div className="error">{erro}</div>}

            <div className="layout">
                <section className="card feed-card">
                    <h2 className="section-title">Chamados</h2>

                    {carregandoFeed ? (
                        <p>Carregando chamados...</p>
                    ) : chamados.length === 0 ? (
                        <p>Nenhum chamado encontrado.</p>
                    ) : (
                        <div className="ticket-list">
                            {chamados.map((chamado) => (
                                <button
                                    key={chamado.id}
                                    className={
                                        chamadoSelecionado?.id === chamado.id
                                            ? "ticket-item selected"
                                            : "ticket-item"
                                    }
                                    onClick={() => selecionarChamado(chamado)}
                                >
                                    <div>
                                        <strong>
                                            OSTI {chamado.numeroChamado}
                                        </strong>

                                        <p>{chamado.unidadeNome}</p>

                                        <small>
                                            {chamado.contratoCidade}
                                        </small>
                                    </div>

                                    <span className="status">
                                        {chamado.status}
                                    </span>
                                </button>
                            ))}
                        </div>
                    )}
                </section>

                <section className="detail-area">
                    {carregandoDetalhe && (
                        <section className="card">
                            <p>Carregando detalhes do chamado...</p>
                        </section>
                    )}

                    {!carregandoDetalhe && !chamadoSelecionado && (
                        <section className="card empty-detail">
                            <h2>Selecione um chamado</h2>

                            <p>
                                Escolha um chamado no feed para visualizar os
                                detalhes.
                            </p>
                        </section>
                    )}

                    {!carregandoDetalhe && chamadoSelecionado && (
                        <>
                            <section className="card">
                                <header className="card-header">
                                    <div>
                                        <span className="label">
                                            Chamado OSTI
                                        </span>

                                        <h1>
                                            {chamadoSelecionado.numeroChamado}
                                        </h1>
                                    </div>

                                    <span className="status">
                                        {chamadoSelecionado.status}
                                    </span>
                                </header>

                                <div className="grid">
                                    <div>
                                        <span className="label">Unidade</span>

                                        <p>
                                            {chamadoSelecionado.unidadeNome}
                                        </p>
                                    </div>

                                    <div>
                                        <span className="label">Contrato</span>

                                        <p>
                                            {
                                                chamadoSelecionado.contratoCidade
                                            }
                                        </p>
                                    </div>

                                    <div>
                                        <span className="label">
                                            Solicitante
                                        </span>

                                        <p>
                                            {
                                                chamadoSelecionado.solicitante
                                                    .nome
                                            }
                                        </p>
                                    </div>

                                    <div>
                                        <span className="label">
                                            Patrimônio
                                        </span>

                                        <p>
                                            {chamadoSelecionado.numeroPatrimonio ??
                                                "Não informado"}
                                        </p>
                                    </div>

                                    <div>
                                        <span className="label">Tipo</span>

                                        <p>{chamadoSelecionado.tipo}</p>
                                    </div>

                                    <div>
                                        <span className="label">
                                            Prioridade
                                        </span>

                                        <p>
                                            {chamadoSelecionado.prioridade}
                                        </p>
                                    </div>
                                </div>

                                <div className="description">
                                    <span className="label">Descrição</span>

                                    <p>{chamadoSelecionado.descricao}</p>
                                </div>

                                <a
                                    href={chamadoSelecionado.linkChamadoOsti}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="link"
                                >
                                    Abrir chamado no OSTI
                                </a>
                            </section>

                            <section className="card">
                                <h2 className="section-title">
                                    Ordens de serviço
                                </h2>

                                {ordensServico.length === 0 ? (
                                    <p>
                                        Nenhuma ordem de serviço criada.
                                    </p>
                                ) : (
                                    <div className="orders">
                                        {ordensServico.map((ordemServico) => (
                                            <article
                                                key={ordemServico.id}
                                                className="order-card"
                                            >
                                                <div className="order-header">
                                                    <div>
                                                        <span className="label">
                                                            Ordem de Serviço
                                                        </span>

                                                        <h3>
                                                            {
                                                                ordemServico.numeroOrdemServico
                                                            }
                                                        </h3>
                                                    </div>

                                                    <span className="order-status">
                                                        {definirStatusOrdemServico(
                                                            ordemServico
                                                        )}
                                                    </span>
                                                </div>

                                                <div className="grid">
                                                    <div>
                                                        <span className="label">
                                                            Técnico
                                                        </span>

                                                        <p>
                                                            {
                                                                ordemServico.tecnicoNome
                                                            }
                                                        </p>
                                                    </div>

                                                    <div>
                                                        <span className="label">
                                                            Unidade de
                                                            atendimento
                                                        </span>

                                                        <p>
                                                            {
                                                                ordemServico.unidadeAtendimentoNome
                                                            }
                                                        </p>
                                                    </div>

                                                    <div>
                                                        <span className="label">
                                                            Início do
                                                            atendimento
                                                        </span>

                                                        <p>
                                                            {formatarData(
                                                                ordemServico.dataCheckIn
                                                            )}
                                                        </p>
                                                    </div>

                                                    <div>
                                                        <span className="label">
                                                            Finalização do
                                                            atendimento
                                                        </span>

                                                        <p>
                                                            {formatarData(
                                                                ordemServico.dataCheckOut
                                                            )}
                                                        </p>
                                                    </div>
                                                </div>

                                                {!ordemServico.dataCheckIn &&
                                                    !ordemServico.dataCheckOut && (
                                                        <div className="order-actions">
                                                            <button
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
                                                        </div>
                                                    )}

                                                {ordemServico.dataCheckIn &&
                                                    !ordemServico.dataCheckOut && (
                                                        <div className="order-actions">
                                                            <button
                                                                className="danger-button"
                                                                onClick={() =>
                                                                    finalizarAtendimento(
                                                                        ordemServico
                                                                    )
                                                                }
                                                            >
                                                                Finalizar
                                                                atendimento
                                                            </button>
                                                        </div>
                                                    )}
                                            </article>
                                        ))}
                                    </div>
                                )}
                            </section>

                            <section className="card">
                                <h2 className="section-title">
                                    Linha do tempo
                                </h2>

                                <div className="comment-form">
                                    <textarea
                                        value={novoComentarioTexto}
                                        onChange={(event) =>
                                            setNovoComentarioTexto(
                                                event.target.value
                                            )
                                        }
                                        placeholder="Escreva um comentário sobre o chamado..."
                                        rows={4}
                                    />

                                    <div className="comment-form-footer">
                                        <select
                                            value={
                                                novaComentarioOrdemServicoId
                                            }
                                            onChange={(event) =>
                                                setNovaComentarioOrdemServicoId(
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
                                                        key={ordemServico.id}
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

                                        <button
                                            className="primary-button"
                                            onClick={adicionarComentario}
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
                        </>
                    )}
                </section>
            </div>
        </main>
    );
}

export default App;