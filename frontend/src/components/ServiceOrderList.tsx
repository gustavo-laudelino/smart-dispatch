import { useState } from "react";

import AssignTechnicianForm from "./AssignTechnicianForm";
import CreateServiceOrderForm from "./CreateServiceOrderForm";

import type {
    Chamado,
    OrdemServico,
} from "../types";

type ServiceOrderListProps = {
    chamado: Chamado;
    ordensServico: OrdemServico[];

    aoIniciarAtendimento: (
        ordemServico: OrdemServico
    ) => void;

    aoFinalizarAtendimento: (
        ordemServico: OrdemServico
    ) => void;

    aoOrdemCriada: (
        ordemServico: OrdemServico
    ) => void;
};

type StatusVisualOrdem = {
    rotulo: string;
    classe: string;
};

function formatarData(data: string | null) {
    if (!data) {
        return "Aguardando";
    }

    return new Date(data).toLocaleString(
        "pt-BR",
        {
            dateStyle: "short",
            timeStyle: "short",
        }
    );
}

function definirStatusOrdemServico(
    ordemServico: OrdemServico
): StatusVisualOrdem {
    if (
        ordemServico.dataCheckIn &&
        ordemServico.dataCheckOut
    ) {
        return {
            rotulo: "Encerrada",
            classe: "encerrada",
        };
    }

    if (
        ordemServico.dataCheckIn &&
        !ordemServico.dataCheckOut
    ) {
        return {
            rotulo: "Em atendimento",
            classe: "em-atendimento",
        };
    }

    if (ordemServico.tecnicoId === null) {
        return {
            rotulo: "Aguardando atribuição",
            classe: "sem-tecnico",
        };
    }

    return {
        rotulo: "Aguardando início",
        classe: "aguardando-inicio",
    };
}

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

    const [
        ordemEmAtribuicaoId,
        setOrdemEmAtribuicaoId,
    ] = useState<number | null>(null);

    function ordemCriada(
        ordemServico: OrdemServico
    ) {
        setCriandoOrdemServico(false);
        aoOrdemCriada(ordemServico);
    }

    function tecnicoAtualizado(
        ordemServico: OrdemServico
    ) {
        setOrdemEmAtribuicaoId(null);
        aoOrdemCriada(ordemServico);
    }

    return (
        <section className="card service-orders-card">
            <div className="service-orders-header">
                <div>
                    <span className="label">
                        Execução operacional
                    </span>

                    <div className="service-orders-title-row">
                        <h2 className="section-title">
                            Ordens de serviço
                        </h2>

                        <span className="service-orders-count">
                            {ordensServico.length}
                        </span>
                    </div>

                    <p>
                        Acompanhe a distribuição e a
                        execução dos atendimentos.
                    </p>
                </div>

                {!criandoOrdemServico && (
                    <button
                        type="button"
                        className="primary-button"
                        onClick={() =>
                            setCriandoOrdemServico(true)
                        }
                    >
                        + Nova Ordem de Serviço
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
                <div className="service-orders-empty">
                    <div className="service-orders-empty-icon">
                        OS
                    </div>

                    <h3>
                        Nenhuma ordem de serviço
                    </h3>

                    <p>
                        Crie uma ordem para iniciar a
                        distribuição do atendimento.
                    </p>
                </div>
            ) : (
                <div className="orders">
                    {ordensServico.map(
                        (ordemServico) => {
                            const status =
                                definirStatusOrdemServico(
                                    ordemServico
                                );

                            const atendimentoNaoIniciado =
                                !ordemServico.dataCheckIn &&
                                !ordemServico.dataCheckOut;

                            const formularioAberto =
                                ordemEmAtribuicaoId ===
                                ordemServico.id;

                            const atribuida =
                                ordemServico.tecnicoId !==
                                null;

                            const iniciada =
                                ordemServico.dataCheckIn !==
                                null;

                            const encerrada =
                                ordemServico.dataCheckOut !==
                                null;

                            return (
                                <article
                                    key={ordemServico.id}
                                    className={`order-card order-card-${status.classe}`}
                                >
                                    <header className="order-summary-header">
                                        <div className="order-identity">
                                            <div className="order-identity-icon">
                                                OS
                                            </div>

                                            <div>
                                                <span className="label">
                                                    Ordem de serviço
                                                </span>

                                                <h3>
                                                    {
                                                        ordemServico.numeroOrdemServico
                                                    }
                                                </h3>
                                            </div>
                                        </div>

                                        <span
                                            className={`order-status order-status-${status.classe}`}
                                        >
                                            <span className="order-status-dot" />

                                            {status.rotulo}
                                        </span>
                                    </header>

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
                                                Unidade de atendimento
                                            </span>

                                            <strong>
                                                {
                                                    ordemServico.unidadeAtendimentoNome
                                                }
                                            </strong>
                                        </div>

                                        <div className="order-information-field">
                                            <span className="label">
                                                Check-in
                                            </span>

                                            <strong>
                                                {formatarData(
                                                    ordemServico.dataCheckIn
                                                )}
                                            </strong>
                                        </div>

                                        <div className="order-information-field">
                                            <span className="label">
                                                Check-out
                                            </span>

                                            <strong>
                                                {formatarData(
                                                    ordemServico.dataCheckOut
                                                )}
                                            </strong>
                                        </div>
                                    </div>

                                    <div className="order-progress">
                                        <div
                                            className={`order-progress-step ${
                                                atribuida
                                                    ? "completed"
                                                    : "current"
                                            }`}
                                        >
                                            <span className="order-progress-marker">
                                                {atribuida
                                                    ? "✓"
                                                    : "1"}
                                            </span>

                                            <span>
                                                Técnico atribuído
                                            </span>
                                        </div>

                                        <div
                                            className={`order-progress-line ${
                                                iniciada
                                                    ? "completed"
                                                    : ""
                                            }`}
                                        />

                                        <div
                                            className={`order-progress-step ${
                                                iniciada
                                                    ? "completed"
                                                    : atribuida
                                                        ? "current"
                                                        : ""
                                            }`}
                                        >
                                            <span className="order-progress-marker">
                                                {iniciada
                                                    ? "✓"
                                                    : "2"}
                                            </span>

                                            <span>
                                                Atendimento iniciado
                                            </span>
                                        </div>

                                        <div
                                            className={`order-progress-line ${
                                                encerrada
                                                    ? "completed"
                                                    : ""
                                            }`}
                                        />

                                        <div
                                            className={`order-progress-step ${
                                                encerrada
                                                    ? "completed"
                                                    : iniciada
                                                        ? "current"
                                                        : ""
                                            }`}
                                        >
                                            <span className="order-progress-marker">
                                                {encerrada
                                                    ? "✓"
                                                    : "3"}
                                            </span>

                                            <span>
                                                Atendimento concluído
                                            </span>
                                        </div>
                                    </div>

                                    {atendimentoNaoIniciado &&
                                        !formularioAberto && (
                                            <div className="order-actions">
                                                <button
                                                    type="button"
                                                    className="secondary-button"
                                                    onClick={() =>
                                                        setOrdemEmAtribuicaoId(
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
                                                                aoIniciarAtendimento(
                                                                    ordemServico
                                                                )
                                                            }
                                                        >
                                                            Iniciar atendimento
                                                        </button>
                                                    )}
                                            </div>
                                        )}

                                    {formularioAberto && (
                                        <AssignTechnicianForm
                                            chamado={chamado}
                                            ordemServico={
                                                ordemServico
                                            }
                                            aoCancelar={() =>
                                                setOrdemEmAtribuicaoId(
                                                    null
                                                )
                                            }
                                            aoTecnicoAtribuido={
                                                tecnicoAtualizado
                                            }
                                        />
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
                            );
                        }
                    )}
                </div>
            )}
        </section>
    );
}

export default ServiceOrderList;