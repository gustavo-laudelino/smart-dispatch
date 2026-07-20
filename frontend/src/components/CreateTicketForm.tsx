import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import {
    buscarUnidades,
    criarChamado,
} from "../api";

import type {
    Chamado,
    ChamadoRequest,
    Contrato,
    Unidade,
} from "../types";

type CreateTicketFormProps = {
    contratos: Contrato[];
    aoCancelar: () => void;
    aoChamadoCriado: (
        chamado: Chamado
    ) => void;
};

function CreateTicketForm({
                              contratos,
                              aoCancelar,
                              aoChamadoCriado,
                          }: CreateTicketFormProps) {
    const [contratoId, setContratoId] =
        useState("");

    const [unidades, setUnidades] =
        useState<Unidade[]>([]);

    const [unidadeId, setUnidadeId] =
        useState("");

    const [numeroChamado, setNumeroChamado] =
        useState("");

    const [linkChamadoOsti, setLinkChamadoOsti] =
        useState("");

    const [solicitanteNome, setSolicitanteNome] =
        useState("");

    const [solicitanteEmail, setSolicitanteEmail] =
        useState("");

    const [
        solicitanteTelefone,
        setSolicitanteTelefone,
    ] = useState("");

    const [
        solicitanteIdentificacao,
        setSolicitanteIdentificacao,
    ] = useState("");

    const [
        numeroPatrimonio,
        setNumeroPatrimonio,
    ] = useState("");

    const [tipo, setTipo] =
        useState("INCIDENTE");

    const [categoria, setCategoria] =
        useState("COMPUTADOR_COM_DEFEITO");

    const [prioridade, setPrioridade] =
        useState("MEDIA");

    const [descricao, setDescricao] =
        useState("");

    const [
        carregandoUnidades,
        setCarregandoUnidades,
    ] = useState(false);

    const [salvando, setSalvando] =
        useState(false);

    const [erro, setErro] =
        useState<string | null>(null);

    useEffect(() => {
        setUnidades([]);
        setUnidadeId("");
        setErro(null);

        if (!contratoId) {
            return;
        }

        setCarregandoUnidades(true);

        buscarUnidades(Number(contratoId))
            .then((data) => {
                setUnidades(data);
            })
            .catch((error: Error) => {
                setErro(error.message);
            })
            .finally(() => {
                setCarregandoUnidades(false);
            });
    }, [contratoId]);

    async function enviarFormulario(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();

        if (!contratoId) {
            setErro("Selecione um contrato");
            return;
        }

        if (!unidadeId) {
            setErro("Selecione uma unidade");
            return;
        }

        setErro(null);
        setSalvando(true);

        const chamadoRequest: ChamadoRequest = {
            numeroChamado:
                numeroChamado.trim(),

            linkChamadoOsti:
                linkChamadoOsti.trim(),

            unidadeId:
                Number(unidadeId),

            solicitante: {
                nome:
                    solicitanteNome.trim(),

                email:
                    solicitanteEmail.trim() ||
                    null,

                telefone:
                    solicitanteTelefone.trim() ||
                    null,

                identificacao:
                    solicitanteIdentificacao.trim() ||
                    null,
            },

            numeroPatrimonio:
                numeroPatrimonio.trim() ||
                null,

            tipo,
            categoria,
            prioridade,

            descricao:
                descricao.trim(),
        };

        try {
            const chamadoCriado =
                await criarChamado(
                    Number(contratoId),
                    chamadoRequest
                );

            aoChamadoCriado(
                chamadoCriado
            );
        } catch (error) {
            setErro(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );
        } finally {
            setSalvando(false);
        }
    }

    return (
        <section className="card create-ticket-card">
            <header className="create-ticket-header">
                <div className="create-ticket-heading">
                    <div className="create-ticket-icon">
                        +
                    </div>

                    <div>
                        <span className="label">
                            Cadastro operacional
                        </span>

                        <h1>
                            Novo chamado
                        </h1>

                        <p>
                            Registre os dados recebidos
                            pelo OSTI para iniciar o
                            atendimento.
                        </p>
                    </div>
                </div>

                <button
                    type="button"
                    className="create-ticket-close"
                    onClick={aoCancelar}
                    disabled={salvando}
                    aria-label="Fechar formulário"
                >
                    ×
                </button>
            </header>

            {erro && (
                <div className="create-ticket-error">
                    <div>
                        <strong>
                            Não foi possível continuar
                        </strong>

                        <span>
                            {erro}
                        </span>
                    </div>
                </div>
            )}

            <form
                className="ticket-form modern-ticket-form"
                onSubmit={enviarFormulario}
            >
                <section className="form-section">
                    <header className="form-section-header">
                        <span className="form-section-number">
                            1
                        </span>

                        <div>
                            <h2>
                                Origem do chamado
                            </h2>

                            <p>
                                Informe o contrato, a
                                unidade e a referência
                                registrada no OSTI.
                            </p>
                        </div>
                    </header>

                    <div className="form-section-grid">
                        <label className="form-field">
                            <span className="form-field-label">
                                Contrato
                                <strong>*</strong>
                            </span>

                            <select
                                value={contratoId}
                                onChange={(event) =>
                                    setContratoId(
                                        event.target.value
                                    )
                                }
                                required
                            >
                                <option value="">
                                    Selecione um contrato
                                </option>

                                {contratos.map(
                                    (contrato) => (
                                        <option
                                            key={
                                                contrato.id
                                            }
                                            value={
                                                contrato.id
                                            }
                                        >
                                            {
                                                contrato.cidade
                                            }
                                        </option>
                                    )
                                )}
                            </select>

                            <small>
                                Define o escopo operacional
                                do chamado.
                            </small>
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Unidade
                                <strong>*</strong>
                            </span>

                            <select
                                value={unidadeId}
                                onChange={(event) =>
                                    setUnidadeId(
                                        event.target.value
                                    )
                                }
                                disabled={
                                    !contratoId ||
                                    carregandoUnidades
                                }
                                required
                            >
                                <option value="">
                                    {carregandoUnidades
                                        ? "Carregando unidades..."
                                        : !contratoId
                                            ? "Selecione primeiro o contrato"
                                            : "Selecione uma unidade"}
                                </option>

                                {unidades.map(
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

                            <small>
                                Local onde o atendimento
                                será realizado.
                            </small>
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Número do chamado
                                <strong>*</strong>
                            </span>

                            <input
                                type="text"
                                value={numeroChamado}
                                onChange={(event) =>
                                    setNumeroChamado(
                                        event.target.value
                                    )
                                }
                                placeholder="Ex.: 12345"
                                required
                            />
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Link no OSTI
                                <strong>*</strong>
                            </span>

                            <input
                                type="url"
                                value={linkChamadoOsti}
                                onChange={(event) =>
                                    setLinkChamadoOsti(
                                        event.target.value
                                    )
                                }
                                placeholder="https://..."
                                required
                            />
                        </label>
                    </div>
                </section>

                <section className="form-section">
                    <header className="form-section-header">
                        <span className="form-section-number">
                            2
                        </span>

                        <div>
                            <h2>
                                Solicitante
                            </h2>

                            <p>
                                Dados da pessoa que
                                registrou ou acompanha a
                                solicitação.
                            </p>
                        </div>
                    </header>

                    <div className="form-section-grid">
                        <label className="form-field">
                            <span className="form-field-label">
                                Nome
                                <strong>*</strong>
                            </span>

                            <input
                                type="text"
                                value={solicitanteNome}
                                onChange={(event) =>
                                    setSolicitanteNome(
                                        event.target.value
                                    )
                                }
                                placeholder="Nome completo"
                                required
                            />
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                E-mail
                            </span>

                            <input
                                type="email"
                                value={solicitanteEmail}
                                onChange={(event) =>
                                    setSolicitanteEmail(
                                        event.target.value
                                    )
                                }
                                placeholder="email@exemplo.com"
                            />
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Telefone
                            </span>

                            <input
                                type="text"
                                value={
                                    solicitanteTelefone
                                }
                                onChange={(event) =>
                                    setSolicitanteTelefone(
                                        event.target.value
                                    )
                                }
                                placeholder="(00) 00000-0000"
                            />
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Identificação
                            </span>

                            <input
                                type="text"
                                value={
                                    solicitanteIdentificacao
                                }
                                onChange={(event) =>
                                    setSolicitanteIdentificacao(
                                        event.target.value
                                    )
                                }
                                placeholder="CPF, RG ou matrícula"
                            />
                        </label>
                    </div>
                </section>

                <section className="form-section">
                    <header className="form-section-header">
                        <span className="form-section-number">
                            3
                        </span>

                        <div>
                            <h2>
                                Classificação
                            </h2>

                            <p>
                                Classifique o chamado para
                                apoiar a triagem e a
                                priorização.
                            </p>
                        </div>
                    </header>

                    <div className="form-section-grid form-section-grid-three">
                        <label className="form-field">
                            <span className="form-field-label">
                                Patrimônio
                            </span>

                            <input
                                type="text"
                                value={
                                    numeroPatrimonio
                                }
                                onChange={(event) =>
                                    setNumeroPatrimonio(
                                        event.target.value
                                    )
                                }
                                placeholder="Número do equipamento"
                            />
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Tipo
                            </span>

                            <select
                                value={tipo}
                                onChange={(event) =>
                                    setTipo(
                                        event.target.value
                                    )
                                }
                            >
                                <option value="INCIDENTE">
                                    Incidente
                                </option>

                                <option value="REQUISICAO">
                                    Requisição
                                </option>
                            </select>
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Prioridade
                            </span>

                            <select
                                value={prioridade}
                                onChange={(event) =>
                                    setPrioridade(
                                        event.target.value
                                    )
                                }
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

                        <label className="form-field form-field-wide">
                            <span className="form-field-label">
                                Categoria
                            </span>

                            <select
                                value={categoria}
                                onChange={(event) =>
                                    setCategoria(
                                        event.target.value
                                    )
                                }
                            >
                                <option value="COMPUTADOR_COM_DEFEITO">
                                    Computador com defeito
                                </option>

                                <option value="INSTALACAO_DE_PROGRAMAS">
                                    Instalação de programas
                                </option>

                                <option value="PROJETOR_TELA_INTERATIVA_COM_DEFEITO">
                                    Projetor ou tela
                                    interativa
                                </option>

                                <option value="OUTROS">
                                    Outros
                                </option>
                            </select>
                        </label>
                    </div>
                </section>

                <section className="form-section">
                    <header className="form-section-header">
                        <span className="form-section-number">
                            4
                        </span>

                        <div>
                            <h2>
                                Descrição
                            </h2>

                            <p>
                                Descreva o problema com as
                                informações necessárias
                                para o técnico.
                            </p>
                        </div>
                    </header>

                    <label className="form-field">
                        <span className="form-field-label">
                            Detalhes da solicitação
                            <strong>*</strong>
                        </span>

                        <textarea
                            value={descricao}
                            onChange={(event) =>
                                setDescricao(
                                    event.target.value
                                )
                            }
                            rows={6}
                            placeholder="Descreva o problema, sintomas apresentados e informações importantes para o atendimento..."
                            required
                        />

                        <small className="form-character-count">
                            {descricao.length} caracteres
                        </small>
                    </label>
                </section>

                <footer className="modern-form-actions">
                    <div>
                        <strong>
                            Pronto para registrar?
                        </strong>

                        <span>
                            Os campos marcados com * são
                            obrigatórios.
                        </span>
                    </div>

                    <div className="modern-form-buttons">
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={aoCancelar}
                            disabled={salvando}
                        >
                            Cancelar
                        </button>

                        <button
                            type="submit"
                            className="primary-button"
                            disabled={salvando}
                        >
                            {salvando
                                ? "Criando chamado..."
                                : "Criar chamado"}
                        </button>
                    </div>
                </footer>
            </form>
        </section>
    );
}

export default CreateTicketForm;