import { useEffect, useState } from "react";
import "./App.css";

type Solicitante = {
    nome: string;
    email: string | null;
    telefone: string | null;
    identificacao: string | null;
};

type Chamado = {
    id: number;
    numeroChamado: string;
    linkChamadoOsti: string;
    unidadeId: number;
    unidadeNome: string;
    contratoId: number;
    contratoCidade: string;
    solicitante: Solicitante;
    numeroPatrimonio: string | null;
    tipo: string;
    categoria: string;
    prioridade: string;
    status: string;
    descricao: string;
    dataAbertura: string;
    dataFinalizacao: string | null;
};

type OrdemServico = {
    id: number;
    numeroOrdemServico: string;
    chamadoId: number;
    numeroChamado: string;
    tecnicoId: number;
    tecnicoNome: string;
    unidadeAtendimentoId: number;
    unidadeAtendimentoNome: string;
    dataCheckIn: string | null;
    dataCheckOut: string | null;
};

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
    const [chamado, setChamado] = useState<Chamado | null>(null);
    const [ordensServico, setOrdensServico] = useState<OrdemServico[]>([]);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState<string | null>(null);

    useEffect(() => {
        Promise.all([
            fetch("http://localhost:8080/contratos/1/chamados/3"),
            fetch("http://localhost:8080/contratos/1/chamados/3/ordens-servico"),
        ])
            .then(([chamadoResponse, ordensResponse]) => {
                if (!chamadoResponse.ok) {
                    throw new Error("Erro ao buscar chamado");
                }

                if (!ordensResponse.ok) {
                    throw new Error("Erro ao buscar ordens de serviço");
                }

                return Promise.all([
                    chamadoResponse.json(),
                    ordensResponse.json(),
                ]);
            })
            .then(([chamadoData, ordensData]: [Chamado, OrdemServico[]]) => {
                setChamado(chamadoData);
                setOrdensServico(ordensData);
            })
            .catch((error) => {
                setErro(error.message);
            })
            .finally(() => {
                setCarregando(false);
            });
    }, []);

    if (carregando) {
        return <p>Carregando chamado...</p>;
    }

    if (erro) {
        return <p>Erro: {erro}</p>;
    }

    if (!chamado) {
        return <p>Chamado não encontrado.</p>;
    }

    return (
        <main className="page">
            <section className="card">
                <header className="card-header">
                    <div>
                        <span className="label">Chamado OSTI</span>
                        <h1>{chamado.numeroChamado}</h1>
                    </div>

                    <span className="status">{chamado.status}</span>
                </header>

                <div className="grid">
                    <div>
                        <span className="label">Unidade</span>
                        <p>{chamado.unidadeNome}</p>
                    </div>

                    <div>
                        <span className="label">Contrato</span>
                        <p>{chamado.contratoCidade}</p>
                    </div>

                    <div>
                        <span className="label">Solicitante</span>
                        <p>{chamado.solicitante.nome}</p>
                    </div>

                    <div>
                        <span className="label">Patrimônio</span>
                        <p>{chamado.numeroPatrimonio ?? "Não informado"}</p>
                    </div>

                    <div>
                        <span className="label">Tipo</span>
                        <p>{chamado.tipo}</p>
                    </div>

                    <div>
                        <span className="label">Prioridade</span>
                        <p>{chamado.prioridade}</p>
                    </div>
                </div>

                <div className="description">
                    <span className="label">Descrição</span>
                    <p>{chamado.descricao}</p>
                </div>

                <a
                    href={chamado.linkChamadoOsti}
                    target="_blank"
                    rel="noreferrer"
                    className="link"
                >
                    Abrir chamado no OSTI
                </a>
            </section>

            <section className="card">
                <h2 className="section-title">Ordens de serviço</h2>

                {ordensServico.length === 0 ? (
                    <p>Nenhuma ordem de serviço criada.</p>
                ) : (
                    <div className="orders">
                        {ordensServico.map((ordemServico) => (
                            <article key={ordemServico.id} className="order-card">
                                <div className="order-header">
                                    <div>
                                        <span className="label">Ordem de Serviço</span>
                                        <h3>{ordemServico.numeroOrdemServico}</h3>
                                    </div>

                                    <span className="order-status">
                    {definirStatusOrdemServico(ordemServico)}
                  </span>
                                </div>

                                <div className="grid">
                                    <div>
                                        <span className="label">Técnico</span>
                                        <p>{ordemServico.tecnicoNome}</p>
                                    </div>

                                    <div>
                                        <span className="label">Unidade de atendimento</span>
                                        <p>{ordemServico.unidadeAtendimentoNome}</p>
                                    </div>

                                    <div>
                                        <span className="label">Início do atendimento</span>
                                        <p>{formatarData(ordemServico.dataCheckIn)}</p>
                                    </div>

                                    <div>
                                        <span className="label">Finalização do atendimento</span>
                                        <p>{formatarData(ordemServico.dataCheckOut)}</p>
                                    </div>
                                </div>
                            </article>
                        ))}
                    </div>
                )}
            </section>
        </main>
    );
}

export default App;