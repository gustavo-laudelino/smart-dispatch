import { useState } from "react";

import CreateServiceOrderForm from "./CreateServiceOrderForm";

import type {
    Chamado,
    OrdemServico,
} from "../types";

type ServiceOrderListProps = {
    chamado: Chamado;
    ordensServico: OrdemServico[];
    aoIniciarAtendimento: (ordemServico: OrdemServico) => void;
    aoFinalizarAtendimento: (ordemServico: OrdemServico) => void;
    aoOrdemCriada: (ordemServico: OrdemServico) => void;
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
                              chamado,
                              ordensServico,
                              aoIniciarAtendimento,
                              aoFinalizarAtendimento,
                              aoOrdemCriada,
                          }: ServiceOrderListProps) {
    const [
        criandoOrdemServico,
        setCriandoOrdemServico,
    ] = useState(false);

    function ordemCriada(
        ordemServico: OrdemServico
    ) {
        setCriandoOrdemServico(false);
        aoOrdemCriada(ordemServico);
    }

    return (
        <section className="card">
            <div className="service-order-header">
                <h2 className="section-title">
                    Ordens de serviço
                </h2>

                {!criandoOrdemServico && (
                    <button
                        type="button"
                        className="primary-button"
                        onClick={() =>
                            setCriandoOrdemServico(true)
                        }
                    >
                        Criar ordem de serviço
                    </button>
                )}
            </div>

            {criandoOrdemServico && (
                <CreateServiceOrderForm
                    chamado={chamado}
                    aoCancelar={() =>
                        setCriandoOrdemServico(false)
                    }
                    aoOrdemCriada={ordemCriada}
                />
            )}

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
                                            type="button"
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
                                            type="button"
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