import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import {
    atualizarOrdemServico,
    buscarBasesOperacionais,
    buscarTecnicos,
} from "../api";

import type {
    BaseOperacional,
    Chamado,
    OrdemServico,
    OrdemServicoRequest,
    Tecnico,
} from "../types";

type AssignTechnicianFormProps = {
    chamado: Chamado;
    ordemServico: OrdemServico;

    aoCancelar: () => void;

    aoTecnicoAtribuido: (
        ordemServico: OrdemServico
    ) => void | Promise<void>;
};

function AssignTechnicianForm({
                                  chamado,
                                  ordemServico,
                                  aoCancelar,
                                  aoTecnicoAtribuido,
                              }: AssignTechnicianFormProps) {
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
        setCarregandoBases(true);
        setErro(null);

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
                        (tecnico) =>
                            tecnico.ativo
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

        if (!baseId) {
            setErro(
                "Selecione uma base operacional"
            );

            return;
        }

        if (!tecnicoId) {
            setErro("Selecione um técnico");
            return;
        }

        setErro(null);
        setSalvando(true);

        const ordemServicoRequest:
            OrdemServicoRequest = {
            numeroOrdemServico:
            ordemServico.numeroOrdemServico,

            tecnicoId: Number(tecnicoId),

            unidadeAtendimentoId: null,
        };

        try {
            const ordemServicoAtualizada =
                await atualizarOrdemServico(
                    chamado.contratoId,
                    chamado.id,
                    ordemServico.id,
                    ordemServicoRequest
                );

            await aoTecnicoAtribuido(
                ordemServicoAtualizada
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
        <section className="assign-technician-form">
            <h3>
                Atribuir técnico à OS{" "}
                {ordemServico.numeroOrdemServico}
            </h3>

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
                            Base operacional
                        </span>

                        <select
                            value={baseId}
                            onChange={(event) =>
                                setBaseId(
                                    event.target.value
                                )
                            }
                            disabled={
                                carregandoBases ||
                                salvando
                            }
                            required
                        >
                            <option value="">
                                {carregandoBases
                                    ? "Carregando bases..."
                                    : "Selecione uma base"}
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
                                carregandoTecnicos ||
                                salvando
                            }
                            required
                        >
                            <option value="">
                                {carregandoTecnicos
                                    ? "Carregando técnicos..."
                                    : !baseId
                                        ? "Selecione uma base primeiro"
                                        : "Selecione um técnico"}
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
                            !baseId ||
                            !tecnicoId
                        }
                    >
                        {salvando
                            ? "Atribuindo..."
                            : "Atribuir técnico"}
                    </button>
                </div>
            </form>
        </section>
    );
}

export default AssignTechnicianForm;