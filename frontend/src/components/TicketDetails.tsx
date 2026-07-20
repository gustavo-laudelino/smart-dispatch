import { useEffect, useState } from "react";

import { atualizarStatusChamado } from "../api";

import type {
    Chamado,
    StatusChamado,
    StatusChamadoManual,
} from "../types";

type TicketDetailsProps = {
    chamado: Chamado;

    aoStatusAtualizado?: (
        chamado: Chamado
    ) => void;
};

const STATUS_MANUAIS: {
    valor: StatusChamadoManual;
    rotulo: string;
}[] = [
    {
        valor: "AGUARDANDO_ANALISE",
        rotulo: "Aguardando análise",
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

function formatarStatus(
    status: StatusChamado
): string {
    const statusEncontrado =
        STATUS_MANUAIS.find(
            (opcao) =>
                opcao.valor === status
        );

    if (statusEncontrado) {
        return statusEncontrado.rotulo;
    }

    const rotulosAutomaticos: Record<
        string,
        string
    > = {
        ABERTO: "Aberto",
        ATRIBUIDO: "Atribuído",
        EM_ATENDIMENTO: "Em atendimento",
    };

    return (
        rotulosAutomaticos[status] ??
        status
    );
}

function statusEhManual(
    status: StatusChamado
): status is StatusChamadoManual {
    return STATUS_MANUAIS.some(
        (opcao) =>
            opcao.valor === status
    );
}

function criarClassePrioridade(
    prioridade: string
) {
    return prioridade
        .toLowerCase()
        .replace(/_/g, "-");
}

function TicketDetails({
                           chamado,
                           aoStatusAtualizado,
                       }: TicketDetailsProps) {
    const [statusAtual, setStatusAtual] =
        useState<StatusChamado>(
            chamado.status
        );

    const [
        alterandoStatus,
        setAlterandoStatus,
    ] = useState(false);

    const [erroStatus, setErroStatus] =
        useState<string | null>(null);

    useEffect(() => {
        setStatusAtual(chamado.status);
        setErroStatus(null);
    }, [chamado.id, chamado.status]);

    async function alterarStatus(
        novoStatus: StatusChamadoManual
    ) {
        if (novoStatus === statusAtual) {
            return;
        }

        setErroStatus(null);
        setAlterandoStatus(true);

        try {
            const chamadoAtualizado =
                await atualizarStatusChamado(
                    chamado.contratoId,
                    chamado.id,
                    novoStatus
                );

            setStatusAtual(
                chamadoAtualizado.status
            );

            aoStatusAtualizado?.(
                chamadoAtualizado
            );
        } catch (error) {
            setErroStatus(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        } finally {
            setAlterandoStatus(false);
        }
    }

    const classePrioridade =
        criarClassePrioridade(
            chamado.prioridade
        );

    return (
        <section className="card ticket-detail-card">
            <header className="ticket-detail-header">
                <div className="ticket-detail-heading">
                    <span className="label">
                        Chamado OSTI
                    </span>

                    <h1>
                        {chamado.numeroChamado}
                    </h1>

                    <p>
                        {chamado.unidadeNome}
                        <span>•</span>
                        {chamado.contratoCidade}
                    </p>
                </div>

                <div className="ticket-status-control">
                    <span className="label">
                        Status do chamado
                    </span>

                    <select
                        className="ticket-status-select"
                        value={statusAtual}
                        onChange={(event) =>
                            alterarStatus(
                                event.target
                                    .value as StatusChamadoManual
                            )
                        }
                        disabled={alterandoStatus}
                        title="Clique para alterar o status"
                    >
                        {!statusEhManual(
                            statusAtual
                        ) && (
                            <option
                                value={statusAtual}
                                disabled
                            >
                                {formatarStatus(
                                    statusAtual
                                )}
                            </option>
                        )}

                        {STATUS_MANUAIS.map(
                            (opcao) => (
                                <option
                                    key={opcao.valor}
                                    value={opcao.valor}
                                >
                                    {opcao.rotulo}
                                </option>
                            )
                        )}
                    </select>

                    {alterandoStatus && (
                        <small>
                            Atualizando status...
                        </small>
                    )}
                </div>
            </header>

            {erroStatus && (
                <div className="error">
                    {erroStatus}
                </div>
            )}

            <div className="ticket-detail-grid">
                <div className="ticket-detail-field">
                    <span className="label">
                        Solicitante
                    </span>

                    <strong>
                        {chamado.solicitante.nome}
                    </strong>
                </div>

                <div className="ticket-detail-field">
                    <span className="label">
                        Patrimônio
                    </span>

                    <strong>
                        {chamado.numeroPatrimonio ??
                            "Não informado"}
                    </strong>
                </div>

                <div className="ticket-detail-field">
                    <span className="label">
                        Tipo
                    </span>

                    <strong>
                        {chamado.tipo}
                    </strong>
                </div>

                <div className="ticket-detail-field">
                    <span className="label">
                        Categoria
                    </span>

                    <strong>
                        {chamado.categoria}
                    </strong>
                </div>

                <div className="ticket-detail-field">
                    <span className="label">
                        Prioridade
                    </span>

                    <span
                        className={`priority-badge priority-${classePrioridade}`}
                    >
                        {chamado.prioridade}
                    </span>
                </div>

                <div className="ticket-detail-field">
                    <span className="label">
                        Unidade
                    </span>

                    <strong>
                        {chamado.unidadeNome}
                    </strong>
                </div>
            </div>

            <div className="ticket-description">
                <div>
                    <span className="label">
                        Descrição
                    </span>

                    <p>
                        {chamado.descricao}
                    </p>
                </div>
            </div>

            <footer className="ticket-detail-footer">
                <span>
                    Chamado sincronizado com o OSTI
                </span>

                <a
                    href={chamado.linkChamadoOsti}
                    target="_blank"
                    rel="noreferrer"
                    className="link"
                >
                    Abrir no OSTI
                    <span>↗</span>
                </a>
            </footer>
        </section>
    );
}

export default TicketDetails;