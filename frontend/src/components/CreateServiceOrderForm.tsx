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

    const [baseId, setBaseId] = useState("");

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
                        (tecnico) => tecnico.ativo
                    );

                setTecnicos(tecnicosAtivos);
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
            setBaseId("");
            setTecnicoId("");
            setTecnicos([]);

            await aoOrdemCriada(
                ordemServicoCriada
            );
        } catch (error) {
            if (error instanceof Error) {
                setErro(error.message);
                return;
            }

            setErro(
                "Ocorreu um erro inesperado"
            );
        } finally {
            setSalvando(false);
        }
    }

    return (
        <section className="create-service-order-form">
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
                            Número da OS
                        </span>

                        <input
                            type="text"
                            value={numeroOrdemServico}
                            onChange={(event) =>
                                setNumeroOrdemServico(
                                    event.target.value
                                )
                            }
                            placeholder="Ex.: OS-12345"
                            required
                        />
                    </label>

                    <label className="form-field">
                        <span className="label">
                            Base operacional (opcional)
                        </span>

                        <select
                            value={baseId}
                            onChange={(event) =>
                                setBaseId(
                                    event.target.value
                                )
                            }
                            disabled={carregandoBases}
                        >
                            <option value="">
                                {carregandoBases
                                    ? "Carregando bases..."
                                    : "Nenhuma base selecionada"}
                            </option>

                            {basesOperacionais.map(
                                (base) => (
                                    <option
                                        key={base.id}
                                        value={base.id}
                                    >
                                        {base.nome}
                                    </option>
                                )
                            )}
                        </select>
                    </label>

                    <label className="form-field">
                        <span className="label">
                            Técnico (opcional)
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
                                        ? "Selecione uma base para visualizar técnicos"
                                        : "Criar sem técnico"}
                            </option>

                            {tecnicos.map(
                                (tecnico) => (
                                    <option
                                        key={tecnico.id}
                                        value={tecnico.id}
                                    >
                                        {tecnico.nome}
                                    </option>
                                )
                            )}
                        </select>
                    </label>
                </div>

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
                        disabled={
                            salvando ||
                            !numeroOrdemServico.trim()
                        }
                    >
                        {salvando
                            ? "Criando ordem..."
                            : "Criar ordem de serviço"}
                    </button>
                </div>
            </form>
        </section>
    );
}

export default CreateServiceOrderForm;