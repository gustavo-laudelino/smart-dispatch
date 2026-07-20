import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import {
    buscarBasesOperacionais,
    buscarTecnicos,
    criarOrdemServico,
} from "../api";

import type {
    BaseOperacional,
    Chamado,
    OrdemServico,
    OrdemServicoRequest,
    Tecnico,
} from "../types";

type CreateServiceOrderFormProps = {
    chamado: Chamado;

    aoCancelar: () => void;

    aoOrdemCriada: (
        ordemServico: OrdemServico
    ) => void | Promise<void>;
};

function CreateServiceOrderForm({
                                    chamado,
                                    aoCancelar,
                                    aoOrdemCriada,
                                }: CreateServiceOrderFormProps) {
    const [
        numeroOrdemServico,
        setNumeroOrdemServico,
    ] = useState("");

    const [
        basesOperacionais,
        setBasesOperacionais,
    ] = useState<BaseOperacional[]>([]);

    const [baseId, setBaseId] =
        useState("");

    const [tecnicos, setTecnicos] =
        useState<Tecnico[]>([]);

    const [tecnicoId, setTecnicoId] =
        useState("");

    const [
        carregandoBases,
        setCarregandoBases,
    ] = useState(false);

    const [
        carregandoTecnicos,
        setCarregandoTecnicos,
    ] = useState(false);

    const [salvando, setSalvando] =
        useState(false);

    const [erro, setErro] =
        useState<string | null>(null);

    useEffect(() => {
        setBasesOperacionais([]);
        setBaseId("");
        setTecnicos([]);
        setTecnicoId("");
        setErro(null);

        setCarregandoBases(true);

        buscarBasesOperacionais(
            chamado.contratoId
        )
            .then((data) => {
                setBasesOperacionais(data);

                if (data.length === 1) {
                    setBaseId(
                        String(data[0].id)
                    );
                }
            })
            .catch((error: Error) => {
                setErro(error.message);
            })
            .finally(() => {
                setCarregandoBases(false);
            });
    }, [chamado.contratoId]);

    useEffect(() => {
        setTecnicos([]);
        setTecnicoId("");

        if (!baseId) {
            return;
        }

        setCarregandoTecnicos(true);
        setErro(null);

        buscarTecnicos(
            chamado.contratoId,
            Number(baseId)
        )
            .then((data) => {
                const tecnicosAtivos =
                    data.filter(
                        (tecnico) =>
                            tecnico.ativo
                    );

                setTecnicos(
                    tecnicosAtivos
                );
            })
            .catch((error: Error) => {
                setErro(error.message);
            })
            .finally(() => {
                setCarregandoTecnicos(false);
            });
    }, [baseId, chamado.contratoId]);

    async function enviarFormulario(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();

        const numero =
            numeroOrdemServico.trim();

        if (!numero) {
            setErro(
                "Informe o número da ordem de serviço"
            );

            return;
        }

        if (!baseId) {
            setErro(
                "Selecione uma base operacional"
            );

            return;
        }

        setErro(null);
        setSalvando(true);

        const ordemServicoRequest:
            OrdemServicoRequest = {
            numeroOrdemServico: numero,

            tecnicoId: tecnicoId
                ? Number(tecnicoId)
                : null,

            unidadeAtendimentoId: null,
        };

        try {
            const ordemServicoCriada =
                await criarOrdemServico(
                    chamado.contratoId,
                    chamado.id,
                    ordemServicoRequest
                );

            setNumeroOrdemServico("");
            setTecnicoId("");
            setTecnicos([]);

            await aoOrdemCriada(
                ordemServicoCriada
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
        <section className="modern-service-order-form">
            <header className="service-order-form-header">
                <div className="service-order-form-heading">
                    <div className="service-order-form-icon">
                        OS
                    </div>

                    <div>
                        <span className="label">
                            Nova execução
                        </span>

                        <h3>
                            Criar ordem de serviço
                        </h3>

                        <p>
                            Registre a ordem, selecione a
                            base operacional e atribua
                            opcionalmente um técnico.
                        </p>
                    </div>
                </div>

                <button
                    type="button"
                    className="service-order-form-close"
                    onClick={aoCancelar}
                    disabled={salvando}
                    aria-label="Fechar formulário"
                >
                    ×
                </button>
            </header>

            {erro && (
                <div className="service-order-form-error">
                    <strong>
                        Não foi possível criar a ordem
                    </strong>

                    <span>{erro}</span>
                </div>
            )}

            <div className="service-order-context">
                <div>
                    <span className="label">
                        Chamado
                    </span>

                    <strong>
                        OSTI {chamado.numeroChamado}
                    </strong>
                </div>

                <div>
                    <span className="label">
                        Unidade
                    </span>

                    <strong>
                        {chamado.unidadeNome}
                    </strong>
                </div>

                <div>
                    <span className="label">
                        Contrato
                    </span>

                    <strong>
                        {chamado.contratoCidade}
                    </strong>
                </div>
            </div>

            <form
                className="service-order-create-form"
                onSubmit={enviarFormulario}
            >
                <div className="service-order-form-body">
                    <label className="form-field service-order-number-field">
                        <span className="form-field-label">
                            Número da ordem de serviço
                            <strong>*</strong>
                        </span>

                        <input
                            type="text"
                            value={
                                numeroOrdemServico
                            }
                            onChange={(event) =>
                                setNumeroOrdemServico(
                                    event.target.value
                                )
                            }
                            placeholder="Ex.: OS-12345"
                            required
                        />

                        <small>
                            Utilize o número correspondente
                            ao registro oficial da ordem.
                        </small>
                    </label>

                    <div className="service-order-assignment-section">
                        <header>
                            <span className="service-order-section-number">
                                2
                            </span>

                            <div>
                                <h4>
                                    Distribuição inicial
                                </h4>

                                <p>
                                    A base operacional é
                                    obrigatória. A atribuição
                                    do técnico pode ser feita
                                    agora ou posteriormente.
                                </p>
                            </div>
                        </header>

                        <div className="service-order-form-grid">
                            <label className="form-field">
                                <span className="form-field-label">
                                    Base operacional
                                    <strong>*</strong>
                                </span>

                                <select
                                    value={baseId}
                                    onChange={(event) =>
                                        setBaseId(
                                            event.target.value
                                        )
                                    }
                                    disabled={
                                        carregandoBases
                                    }
                                    required
                                >
                                    <option value="">
                                        {carregandoBases
                                            ? "Carregando bases..."
                                            : basesOperacionais.length ===
                                            0
                                                ? "Nenhuma base disponível"
                                                : "Selecione uma base operacional"}
                                    </option>

                                    {basesOperacionais.map(
                                        (base) => (
                                            <option
                                                key={
                                                    base.id
                                                }
                                                value={
                                                    base.id
                                                }
                                            >
                                                {
                                                    base.nome
                                                }
                                            </option>
                                        )
                                    )}
                                </select>

                                <small>
                                    Quando existe apenas uma
                                    base, ela é selecionada
                                    automaticamente.
                                </small>
                            </label>

                            <label className="form-field">
                                <span className="form-field-label">
                                    Técnico
                                </span>

                                <select
                                    value={tecnicoId}
                                    onChange={(event) =>
                                        setTecnicoId(
                                            event.target.value
                                        )
                                    }
                                    disabled={
                                        !baseId ||
                                        carregandoTecnicos
                                    }
                                >
                                    <option value="">
                                        {carregandoTecnicos
                                            ? "Carregando técnicos..."
                                            : !baseId
                                                ? "Selecione primeiro uma base"
                                                : tecnicos.length ===
                                                0
                                                    ? "Nenhum técnico ativo encontrado"
                                                    : "Criar sem técnico"}
                                    </option>

                                    {tecnicos.map(
                                        (tecnico) => (
                                            <option
                                                key={
                                                    tecnico.id
                                                }
                                                value={
                                                    tecnico.id
                                                }
                                            >
                                                {
                                                    tecnico.nome
                                                }
                                            </option>
                                        )
                                    )}
                                </select>

                                <small>
                                    Apenas técnicos ativos
                                    da base selecionada são
                                    apresentados.
                                </small>
                            </label>
                        </div>
                    </div>

                    <div className="smart-dispatch-form-tip">
                        <div className="smart-dispatch-tip-icon">
                            SD
                        </div>

                        <div>
                            <strong>
                                Smart Dispatch
                            </strong>

                            <p>
                                A ordem pode ser criada sem
                                técnico. Depois, o sistema
                                compara as melhores opções
                                considerando distância e
                                carga operacional.
                            </p>
                        </div>
                    </div>
                </div>

                <footer className="service-order-form-actions">
                    <div>
                        <strong>
                            Unidade de atendimento
                        </strong>

                        <span>
                            A ordem será criada para{" "}
                            {chamado.unidadeNome}.
                        </span>
                    </div>

                    <div className="service-order-form-buttons">
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
                            disabled={
                                salvando ||
                                !numeroOrdemServico.trim() ||
                                !baseId
                            }
                        >
                            {salvando
                                ? "Criando ordem..."
                                : "Criar ordem de serviço"}
                        </button>
                    </div>
                </footer>
            </form>
        </section>
    );
}

export default CreateServiceOrderForm;