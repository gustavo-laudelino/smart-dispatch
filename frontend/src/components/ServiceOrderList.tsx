import type { OrdemServico } from "../types";

type ServiceOrderListProps = {
    ordensServico: OrdemServico[];
    aoIniciarAtendimento: (ordemServico: OrdemServico) => void;
    aoFinalizarAtendimento: (ordemServico: OrdemServico) => void;
};

function formatarData(data: string | null) {
    if (!data) {
        return "Não informado";
    }

    return new Date(data).toLocaleString("pt-BR");
}

function definirStatusOrdemServico(
    ordemServico: OrdemServico
) {
    if (
        ordemServico.dataCheckIn &&
        ordemServico.dataCheckOut
    ) {
        return "Encerrada";
    }

    if (
        ordemServico.dataCheckIn &&
        !ordemServico.dataCheckOut
    ) {
        return "Em atendimento";
    }

    return "Aguardando início";
}

function ServiceOrderList({
                              ordensServico,
                              aoIniciarAtendimento,
                              aoFinalizarAtendimento,
                          }: ServiceOrderListProps) {
    return (
        <section className="card">
            <h2 className="section-title">
                Ordens de serviço
            </h2>

            {ordensServico.length === 0 ? (
                <p>Nenhuma ordem de serviço criada.</p>
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
                                        {ordemServico.tecnicoNome}
                                    </p>
                                </div>

                                <div>
                                    <span className="label">
                                        Unidade de atendimento
                                    </span>

                                    <p>
                                        {
                                            ordemServico.unidadeAtendimentoNome
                                        }
                                    </p>
                                </div>

                                <div>
                                    <span className="label">
                                        Início do atendimento
                                    </span>

                                    <p>
                                        {formatarData(
                                            ordemServico.dataCheckIn
                                        )}
                                    </p>
                                </div>

                                <div>
                                    <span className="label">
                                        Finalização do atendimento
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
                                                aoIniciarAtendimento(
                                                    ordemServico
                                                )
                                            }
                                        >
                                            Iniciar atendimento
                                        </button>
                                    </div>
                                )}

                            {ordemServico.dataCheckIn &&
                                !ordemServico.dataCheckOut && (
                                    <div className="order-actions">
                                        <button
                                            className="danger-button"
                                            onClick={() =>
                                                aoFinalizarAtendimento(
                                                    ordemServico
                                                )
                                            }
                                        >
                                            Finalizar atendimento
                                        </button>
                                    </div>
                                )}
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
}

export default ServiceOrderList;