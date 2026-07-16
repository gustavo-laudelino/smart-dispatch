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

function TicketDetails({
                           chamado,
                           aoStatusAtualizado,
                       }: TicketDetailsProps) {
    const [statusAtual, setStatusAtual] =
        useState<StatusChamado>(
            chamado.status
        );

    const [alterandoStatus, setAlterandoStatus] =
        useState(false);

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
            if (error instanceof Error) {
                setErroStatus(error.message);
                return;
            }

            setErroStatus(
                "Ocorreu um erro inesperado"
            );
        } finally {
            setAlterandoStatus(false);
        }
    }

    return (
        <section className="card">
            <header className="card-header">
                <div>
                    <span className="label">
                        Chamado OSTI
                    </span>

                    <h1>
                        {chamado.numeroChamado}
                    </h1>
                </div>

                <select
                    className="status"
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
            </header>

            {erroStatus && (
                <div className="error">
                    {erroStatus}
                </div>
            )}

            {alterandoStatus && (
                <p>Alterando status...</p>
            )}

            <div className="grid">
                <div>
                    <span className="label">
                        Unidade
                    </span>

                    <p>{chamado.unidadeNome}</p>
                </div>

                <div>
                    <span className="label">
                        Contrato
                    </span>

                    <p>
                        {chamado.contratoCidade}
                    </p>
                </div>

                <div>
                    <span className="label">
                        Solicitante
                    </span>

                    <p>
                        {chamado.solicitante.nome}
                    </p>
                </div>

                <div>
                    <span className="label">
                        Patrimônio
                    </span>

                    <p>
                        {chamado.numeroPatrimonio ??
                            "Não informado"}
                    </p>
                </div>

                <div>
                    <span className="label">
                        Tipo
                    </span>

                    <p>{chamado.tipo}</p>
                </div>

                <div>
                    <span className="label">
                        Prioridade
                    </span>

                    <p>{chamado.prioridade}</p>
                </div>
            </div>

            <div className="description">
                <span className="label">
                    Descrição
                </span>

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
    );
}

export default TicketDetails;