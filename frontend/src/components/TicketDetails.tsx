import {
    useEffect,
    useState,
} from "react";

import type {
    FormEvent,
} from "react";

import {
    atualizarChamado,
    atualizarStatusChamado,
    buscarUnidades,
} from "../api";

import type {
    Chamado,
    ChamadoRequest,
    StatusChamado,
    StatusChamadoManual,
    Unidade,
} from "../types";

type TicketDetailsProps = {
    chamado: Chamado;

    aoChamadoAtualizado?: (
        chamado: Chamado
    ) => void;
};

type FormularioChamado = {
    numeroChamado: string;
    linkChamadoOsti: string;
    unidadeId: string;

    solicitanteNome: string;
    solicitanteEmail: string;
    solicitanteTelefone: string;
    solicitanteIdentificacao: string;

    numeroPatrimonio: string;
    tipo: string;
    categoria: string;
    prioridade: string;
    descricao: string;
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

function criarFormulario(
    chamado: Chamado
): FormularioChamado {
    return {
        numeroChamado:
        chamado.numeroChamado,

        linkChamadoOsti:
        chamado.linkChamadoOsti,

        unidadeId:
            String(chamado.unidadeId),

        solicitanteNome:
        chamado.solicitante.nome,

        solicitanteEmail:
            chamado.solicitante.email ?? "",

        solicitanteTelefone:
            chamado.solicitante.telefone ?? "",

        solicitanteIdentificacao:
            chamado.solicitante.identificacao ?? "",

        numeroPatrimonio:
            chamado.numeroPatrimonio ?? "",

        tipo: chamado.tipo,
        categoria: chamado.categoria,
        prioridade: chamado.prioridade,
        descricao: chamado.descricao,
    };
}

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
        EM_ATENDIMENTO:
            "Em atendimento",
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

function formatarValorOperacional(
    valor: string
) {
    return valor
        .toLowerCase()
        .replace(/_/g, " ")
        .replace(
            /(^|\s)\S/g,
            (letra) =>
                letra.toUpperCase()
        );
}

function TicketDetails({
                           chamado,
                           aoChamadoAtualizado,
                       }: TicketDetailsProps) {
    const [
        statusAtual,
        setStatusAtual,
    ] = useState<StatusChamado>(
        chamado.status
    );

    const [
        alterandoStatus,
        setAlterandoStatus,
    ] = useState(false);

    const [
        erroStatus,
        setErroStatus,
    ] = useState<string | null>(null);

    const [
        editando,
        setEditando,
    ] = useState(false);

    const [
        salvando,
        setSalvando,
    ] = useState(false);

    const [
        carregandoUnidades,
        setCarregandoUnidades,
    ] = useState(false);

    const [
        unidades,
        setUnidades,
    ] = useState<Unidade[]>([]);

    const [
        erroEdicao,
        setErroEdicao,
    ] = useState<string | null>(null);

    const [
        formulario,
        setFormulario,
    ] = useState<FormularioChamado>(
        () => criarFormulario(chamado)
    );

    useEffect(() => {
        setStatusAtual(chamado.status);
        setErroStatus(null);
    }, [
        chamado.id,
        chamado.status,
    ]);

    useEffect(() => {
        setFormulario(
            criarFormulario(chamado)
        );

        setEditando(false);
        setErroEdicao(null);
        setUnidades([]);
    }, [chamado]);

    function alterarCampo<
        Campo extends keyof FormularioChamado
    >(
        campo: Campo,
        valor: FormularioChamado[Campo]
    ) {
        setFormulario(
            (formularioAtual) => ({
                ...formularioAtual,
                [campo]: valor,
            })
        );
    }

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

            aoChamadoAtualizado?.(
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

    async function iniciarEdicao() {
        setErroEdicao(null);

        setFormulario(
            criarFormulario(chamado)
        );

        setEditando(true);
        setCarregandoUnidades(true);

        try {
            const unidadesEncontradas =
                await buscarUnidades(
                    chamado.contratoId
                );

            setUnidades(
                unidadesEncontradas
            );
        } catch (error) {
            setErroEdicao(
                error instanceof Error
                    ? error.message
                    : "Não foi possível carregar as unidades"
            );
        } finally {
            setCarregandoUnidades(false);
        }
    }

    function cancelarEdicao() {
        setFormulario(
            criarFormulario(chamado)
        );

        setErroEdicao(null);
        setEditando(false);
        setUnidades([]);
    }

    async function salvarAlteracoes(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();

        if (
            !formulario.numeroChamado.trim()
        ) {
            setErroEdicao(
                "O número do chamado deve ser informado"
            );
            return;
        }

        if (
            !formulario.linkChamadoOsti.trim()
        ) {
            setErroEdicao(
                "O link do OSTI deve ser informado"
            );
            return;
        }

        if (!formulario.unidadeId) {
            setErroEdicao(
                "A unidade deve ser selecionada"
            );
            return;
        }

        if (
            !formulario.solicitanteNome.trim()
        ) {
            setErroEdicao(
                "O nome do solicitante deve ser informado"
            );
            return;
        }

        if (!formulario.descricao.trim()) {
            setErroEdicao(
                "A descrição deve ser informada"
            );
            return;
        }

        const request: ChamadoRequest = {
            numeroChamado:
                formulario.numeroChamado.trim(),

            linkChamadoOsti:
                formulario.linkChamadoOsti.trim(),

            unidadeId:
                Number(formulario.unidadeId),

            solicitante: {
                nome:
                    formulario.solicitanteNome.trim(),

                email:
                    formulario.solicitanteEmail.trim() ||
                    null,

                telefone:
                    formulario.solicitanteTelefone.trim() ||
                    null,

                identificacao:
                    formulario.solicitanteIdentificacao.trim() ||
                    null,
            },

            numeroPatrimonio:
                formulario.numeroPatrimonio.trim() ||
                null,

            tipo: formulario.tipo,
            categoria: formulario.categoria,
            prioridade:
            formulario.prioridade,

            descricao:
                formulario.descricao.trim(),
        };

        setErroEdicao(null);
        setSalvando(true);

        try {
            const chamadoAtualizado =
                await atualizarChamado(
                    chamado.contratoId,
                    chamado.id,
                    request
                );

            setFormulario(
                criarFormulario(
                    chamadoAtualizado
                )
            );

            setEditando(false);
            setUnidades([]);

            aoChamadoAtualizado?.(
                chamadoAtualizado
            );
        } catch (error) {
            setErroEdicao(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        } finally {
            setSalvando(false);
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

                <div className="ticket-detail-header-controls">
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
                            disabled={
                                alterandoStatus ||
                                editando
                            }
                            title={
                                editando
                                    ? "Finalize a edição antes de alterar o status"
                                    : "Clique para alterar o status"
                            }
                        >
                            {!statusEhManual(
                                statusAtual
                            ) && (
                                <option
                                    value={
                                        statusAtual
                                    }
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
                                        key={
                                            opcao.valor
                                        }
                                        value={
                                            opcao.valor
                                        }
                                    >
                                        {
                                            opcao.rotulo
                                        }
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
                </div>
            </header>

            {erroStatus && (
                <div className="error">
                    {erroStatus}
                </div>
            )}

            {erroEdicao && (
                <div className="ticket-edit-error">
                    <strong>
                        Não foi possível salvar
                    </strong>

                    <span>
                        {erroEdicao}
                    </span>
                </div>
            )}

            {editando ? (
                <form
                    className="ticket-edit-form"
                    onSubmit={
                        salvarAlteracoes
                    }
                >
                    <section className="ticket-edit-section">
                        <header className="ticket-edit-section-heading">
                            <span>1</span>

                            <div>
                                <h2>
                                    Origem do chamado
                                </h2>

                                <p>
                                    Corrija a referência
                                    do OSTI e a unidade.
                                </p>
                            </div>
                        </header>

                        <div className="ticket-edit-grid">
                            <label className="ticket-edit-field">
                                <span>
                                    Número do chamado
                                    <strong>*</strong>
                                </span>

                                <input
                                    type="text"
                                    value={
                                        formulario.numeroChamado
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "numeroChamado",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                    required
                                />
                            </label>

                            <label className="ticket-edit-field">
                                <span>
                                    Unidade
                                    <strong>*</strong>
                                </span>

                                <select
                                    value={
                                        formulario.unidadeId
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "unidadeId",
                                            event.target.value
                                        )
                                    }
                                    disabled={
                                        salvando ||
                                        carregandoUnidades
                                    }
                                    required
                                >
                                    {carregandoUnidades && (
                                        <option
                                            value={
                                                formulario.unidadeId
                                            }
                                        >
                                            Carregando unidades...
                                        </option>
                                    )}

                                    {!carregandoUnidades &&
                                        unidades.map(
                                            (unidade) => (
                                                <option
                                                    key={
                                                        unidade.id
                                                    }
                                                    value={
                                                        unidade.id
                                                    }
                                                >
                                                    {
                                                        unidade.nome
                                                    }
                                                </option>
                                            )
                                        )}
                                </select>
                            </label>

                            <label className="ticket-edit-field ticket-edit-field-wide">
                                <span>
                                    Link no OSTI
                                    <strong>*</strong>
                                </span>

                                <input
                                    type="url"
                                    value={
                                        formulario.linkChamadoOsti
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "linkChamadoOsti",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                    required
                                />
                            </label>
                        </div>
                    </section>

                    <section className="ticket-edit-section">
                        <header className="ticket-edit-section-heading">
                            <span>2</span>

                            <div>
                                <h2>
                                    Solicitante
                                </h2>

                                <p>
                                    Atualize os dados de
                                    contato e identificação.
                                </p>
                            </div>
                        </header>

                        <div className="ticket-edit-grid">
                            <label className="ticket-edit-field">
                                <span>
                                    Nome
                                    <strong>*</strong>
                                </span>

                                <input
                                    type="text"
                                    value={
                                        formulario.solicitanteNome
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "solicitanteNome",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                    required
                                />
                            </label>

                            <label className="ticket-edit-field">
                                <span>
                                    E-mail
                                </span>

                                <input
                                    type="email"
                                    value={
                                        formulario.solicitanteEmail
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "solicitanteEmail",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                />
                            </label>

                            <label className="ticket-edit-field">
                                <span>
                                    Telefone
                                </span>

                                <input
                                    type="text"
                                    value={
                                        formulario.solicitanteTelefone
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "solicitanteTelefone",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                />
                            </label>

                            <label className="ticket-edit-field">
                                <span>
                                    Identificação
                                </span>

                                <input
                                    type="text"
                                    value={
                                        formulario.solicitanteIdentificacao
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "solicitanteIdentificacao",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                />
                            </label>
                        </div>
                    </section>

                    <section className="ticket-edit-section">
                        <header className="ticket-edit-section-heading">
                            <span>3</span>

                            <div>
                                <h2>
                                    Classificação
                                </h2>

                                <p>
                                    Ajuste a triagem e a
                                    prioridade operacional.
                                </p>
                            </div>
                        </header>

                        <div className="ticket-edit-grid ticket-edit-grid-four">
                            <label className="ticket-edit-field">
                                <span>
                                    Patrimônio
                                </span>

                                <input
                                    type="text"
                                    value={
                                        formulario.numeroPatrimonio
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "numeroPatrimonio",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                />
                            </label>

                            <label className="ticket-edit-field">
                                <span>
                                    Tipo
                                </span>

                                <select
                                    value={
                                        formulario.tipo
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "tipo",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                >
                                    <option value="INCIDENTE">
                                        Incidente
                                    </option>

                                    <option value="REQUISICAO">
                                        Requisição
                                    </option>
                                </select>
                            </label>

                            <label className="ticket-edit-field">
                                <span>
                                    Prioridade
                                </span>

                                <select
                                    value={
                                        formulario.prioridade
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "prioridade",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                >
                                    <option value="BAIXA">
                                        Baixa
                                    </option>

                                    <option value="MEDIA">
                                        Média
                                    </option>

                                    <option value="ALTA">
                                        Alta
                                    </option>

                                    <option value="URGENTE">
                                        Urgente
                                    </option>
                                </select>
                            </label>

                            <label className="ticket-edit-field">
                                <span>
                                    Categoria
                                </span>

                                <select
                                    value={
                                        formulario.categoria
                                    }
                                    onChange={(event) =>
                                        alterarCampo(
                                            "categoria",
                                            event.target.value
                                        )
                                    }
                                    disabled={salvando}
                                >
                                    <option value="COMPUTADOR_COM_DEFEITO">
                                        Computador com defeito
                                    </option>

                                    <option value="INSTALACAO_DE_PROGRAMAS">
                                        Instalação de programas
                                    </option>

                                    <option value="PROJETOR_TELA_INTERATIVA_COM_DEFEITO">
                                        Projetor ou tela interativa
                                    </option>

                                    <option value="OUTROS">
                                        Outros
                                    </option>
                                </select>
                            </label>
                        </div>
                    </section>

                    <section className="ticket-edit-section">
                        <header className="ticket-edit-section-heading">
                            <span>4</span>

                            <div>
                                <h2>
                                    Descrição
                                </h2>

                                <p>
                                    Mantenha o diagnóstico
                                    recebido pelo suporte.
                                </p>
                            </div>
                        </header>

                        <label className="ticket-edit-field">
                            <span>
                                Detalhes da solicitação
                                <strong>*</strong>
                            </span>

                            <textarea
                                value={
                                    formulario.descricao
                                }
                                onChange={(event) =>
                                    alterarCampo(
                                        "descricao",
                                        event.target.value
                                    )
                                }
                                rows={5}
                                disabled={salvando}
                                required
                            />

                            <small>
                                {
                                    formulario
                                        .descricao
                                        .length
                                }{" "}
                                caracteres
                            </small>
                        </label>
                    </section>

                    <footer className="ticket-edit-footer">
                        <div>
                            <strong>
                                Editando chamado
                            </strong>

                            <span>
                                O contrato e o histórico
                                operacional não serão alterados.
                            </span>
                        </div>

                        <div className="ticket-edit-actions">
                            <button
                                type="button"
                                className="secondary-button"
                                onClick={
                                    cancelarEdicao
                                }
                                disabled={salvando}
                            >
                                Cancelar
                            </button>

                            <button
                                type="submit"
                                className="primary-button"
                                disabled={
                                    salvando ||
                                    carregandoUnidades
                                }
                            >
                                {salvando
                                    ? "Salvando..."
                                    : "Salvar alterações"}
                            </button>
                        </div>
                    </footer>
                </form>
            ) : (
                <>
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
                                {formatarValorOperacional(
                                    chamado.tipo
                                )}
                            </strong>
                        </div>

                        <div className="ticket-detail-field">
                            <span className="label">
                                Categoria
                            </span>

                            <strong>
                                {formatarValorOperacional(
                                    chamado.categoria
                                )}
                            </strong>
                        </div>

                        <div className="ticket-detail-field">
                            <span className="label">
                                Prioridade
                            </span>

                            <span
                                className={`priority-badge priority-${classePrioridade}`}
                            >
                                {formatarValorOperacional(
                                    chamado.prioridade
                                )}
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

                    <div className="requester-details">
                        <header className="requester-details-header">
        <span className="label">
            Dados do solicitante
        </span>
                        </header>

                        <div className="requester-details-grid">
                            <div className="requester-detail-item">
            <span>
                Telefone
            </span>

                                <strong>
                                    {chamado.solicitante.telefone ??
                                        "Não informado"}
                                </strong>
                            </div>

                            <div className="requester-detail-item">
            <span>
                E-mail
            </span>

                                <strong>
                                    {chamado.solicitante.email ??
                                        "Não informado"}
                                </strong>
                            </div>

                            <div className="requester-detail-item">
            <span>
                Identificação
            </span>

                                <strong>
                                    {chamado.solicitante.identificacao ??
                                        "Não informada"}
                                </strong>
                            </div>
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

                        <div className="ticket-detail-footer-actions">
                            <button
                                type="button"
                                className="ticket-edit-button ticket-footer-edit-button"
                                onClick={iniciarEdicao}
                            >
                                <svg
                                    viewBox="0 0 24 24"
                                    aria-hidden="true"
                                >
                                    <path
                                        d="M4 20h4L19 9l-4-4L4 16v4Z"
                                        fill="none"
                                        stroke="currentColor"
                                        strokeWidth="2"
                                        strokeLinejoin="round"
                                    />

                                    <path
                                        d="m13.5 6.5 4 4"
                                        fill="none"
                                        stroke="currentColor"
                                        strokeWidth="2"
                                    />
                                </svg>

                                Editar chamado
                            </button>

                            <a
                                href={chamado.linkChamadoOsti}
                                target="_blank"
                                rel="noreferrer"
                                className="link"
                            >
                                Abrir no OSTI
                                <span>↗</span>
                            </a>
                        </div>
                    </footer>
                </>
            )}
        </section>
    );
}

export default TicketDetails;