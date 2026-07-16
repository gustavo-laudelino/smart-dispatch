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
    aoChamadoCriado: (chamado: Chamado) => void;
};

function CreateTicketForm({
                              contratos,
                              aoCancelar,
                              aoChamadoCriado,
                          }: CreateTicketFormProps) {
    const [contratoId, setContratoId] = useState("");
    const [unidades, setUnidades] = useState<Unidade[]>([]);
    const [unidadeId, setUnidadeId] = useState("");

    const [numeroChamado, setNumeroChamado] = useState("");
    const [linkChamadoOsti, setLinkChamadoOsti] = useState("");

    const [solicitanteNome, setSolicitanteNome] = useState("");
    const [solicitanteEmail, setSolicitanteEmail] = useState("");
    const [solicitanteTelefone, setSolicitanteTelefone] = useState("");
    const [solicitanteIdentificacao, setSolicitanteIdentificacao] =
        useState("");

    const [numeroPatrimonio, setNumeroPatrimonio] = useState("");
    const [tipo, setTipo] = useState("INCIDENTE");
    const [categoria, setCategoria] =
        useState("COMPUTADOR_COM_DEFEITO");
    const [prioridade, setPrioridade] = useState("MEDIA");
    const [descricao, setDescricao] = useState("");

    const [carregandoUnidades, setCarregandoUnidades] =
        useState(false);

    const [salvando, setSalvando] = useState(false);
    const [erro, setErro] = useState<string | null>(null);

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
            numeroChamado: numeroChamado.trim(),
            linkChamadoOsti: linkChamadoOsti.trim(),
            unidadeId: Number(unidadeId),

            solicitante: {
                nome: solicitanteNome.trim(),
                email: solicitanteEmail.trim() || null,
                telefone: solicitanteTelefone.trim() || null,
                identificacao:
                    solicitanteIdentificacao.trim() || null,
            },

            numeroPatrimonio:
                numeroPatrimonio.trim() || null,

            tipo,
            categoria,
            prioridade,
            descricao: descricao.trim(),
        };

        try {
            const chamadoCriado = await criarChamado(
                Number(contratoId),
                chamadoRequest
            );

            aoChamadoCriado(chamadoCriado);
        } catch (error) {
            if (error instanceof Error) {
                setErro(error.message);
                return;
            }

            setErro("Ocorreu um erro inesperado");
        } finally {
            setSalvando(false);
        }
    }

    return (
        <section className="card create-ticket-card">
            <header className="card-header">
                <div>
                    <span className="label">
                        Cadastro
                    </span>

                    <h1>Novo chamado</h1>
                </div>
            </header>

            {erro && (
                <div className="error">
                    {erro}
                </div>
            )}

            <form
                className="ticket-form"
                onSubmit={enviarFormulario}
            >
                <div className="grid">
                    <label className="form-field">
                        <span className="label">
                            Contrato
                        </span>

                        <select
                            value={contratoId}
                            onChange={(event) =>
                                setContratoId(event.target.value)
                            }
                            required
                        >
                            <option value="">
                                Selecione um contrato
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
                    </label>

                    <label className="form-field">
                        <span className="label">
                            Unidade
                        </span>

                        <select
                            value={unidadeId}
                            onChange={(event) =>
                                setUnidadeId(event.target.value)
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
                                    : "Selecione uma unidade"}
                            </option>

                            {unidades.map((unidade) => (
                                <option
                                    key={unidade.id}
                                    value={unidade.id}
                                >
                                    {unidade.nome}
                                </option>
                            ))}
                        </select>
                    </label>

                    <label className="form-field">
                        <span className="label">
                            Número do chamado
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
                        <span className="label">
                            Link do chamado no OSTI
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

                    <label className="form-field">
                        <span className="label">
                            Nome do solicitante
                        </span>

                        <input
                            type="text"
                            value={solicitanteNome}
                            onChange={(event) =>
                                setSolicitanteNome(
                                    event.target.value
                                )
                            }
                            required
                        />
                    </label>

                    <label className="form-field">
                        <span className="label">
                            E-mail do solicitante
                        </span>

                        <input
                            type="email"
                            value={solicitanteEmail}
                            onChange={(event) =>
                                setSolicitanteEmail(
                                    event.target.value
                                )
                            }
                        />
                    </label>

                    <label className="form-field">
                        <span className="label">
                            Telefone do solicitante
                        </span>

                        <input
                            type="text"
                            value={solicitanteTelefone}
                            onChange={(event) =>
                                setSolicitanteTelefone(
                                    event.target.value
                                )
                            }
                        />
                    </label>

                    <label className="form-field">
                        <span className="label">
                            Identificação do solicitante
                        </span>

                        <input
                            type="text"
                            value={solicitanteIdentificacao}
                            onChange={(event) =>
                                setSolicitanteIdentificacao(
                                    event.target.value
                                )
                            }
                            placeholder="CPF / RG / Matrícula"
                        />
                    </label>

                    <label className="form-field">
                        <span className="label">
                            Número do patrimônio
                        </span>

                        <input
                            type="text"
                            value={numeroPatrimonio}
                            onChange={(event) =>
                                setNumeroPatrimonio(
                                    event.target.value
                                )
                            }
                        />
                    </label>

                    <label className="form-field">
                        <span className="label">
                            Tipo
                        </span>

                        <select
                            value={tipo}
                            onChange={(event) =>
                                setTipo(event.target.value)
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
                        <span className="label">
                            Categoria
                        </span>

                        <select
                            value={categoria}
                            onChange={(event) =>
                                setCategoria(event.target.value)
                            }
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

                    <label className="form-field">
                        <span className="label">
                            Prioridade
                        </span>

                        <select
                            value={prioridade}
                            onChange={(event) =>
                                setPrioridade(event.target.value)
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
                </div>

                <label className="form-field">
                    <span className="label">
                        Descrição
                    </span>

                    <textarea
                        value={descricao}
                        onChange={(event) =>
                            setDescricao(event.target.value)
                        }
                        rows={5}
                        placeholder="Descreva o problema ou solicitação..."
                        required
                    />
                </label>

                <div className="form-actions">
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
            </form>
        </section>
    );
}

export default CreateTicketForm;