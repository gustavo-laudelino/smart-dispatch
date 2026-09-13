import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import {
    atualizarOrdemServico,
    buscarTecnicosPorContrato,
    buscarUnidades,
    criarOrdemServico,
} from "../api";

import { hojeISO } from "../utils/datas";

import type {
    Chamado,
    OrdemServico,
    OrdemServicoRequest,
    Tecnico,
    Unidade,
} from "../types";

type OrderFormProps = {
    contratoId: number;

    // Presente ao criar uma OS a partir de um chamado — a unidade é
    // herdada do chamado e o vínculo não é editável neste formulário.
    chamadoOrigem?: Chamado;

    // Presente ao editar uma OS já existente (avulsa ou vinculada).
    ordemServico?: OrdemServico;

    aoCancelar: () => void;

    aoSalvo: (
        ordemServico: OrdemServico
    ) => void | Promise<void>;
};

function OrderForm({
                        contratoId,
                        chamadoOrigem,
                        ordemServico,
                        aoCancelar,
                        aoSalvo,
                    }: OrderFormProps) {
    const modoEdicao = ordemServico !== undefined;
    const checkedIn = ordemServico?.dataCheckIn != null;

    // Em criação a partir de chamado, a unidade é sempre herdada. Em
    // edição, só é editável enquanto não houver check-in.
    const unidadeEditavel = chamadoOrigem
        ? false
        : !checkedIn;

    const tecnicoEditavel = !checkedIn;

    const [unidades, setUnidades] =
        useState<Unidade[]>([]);

    const [
        carregandoUnidades,
        setCarregandoUnidades,
    ] = useState(false);

    const [tecnicos, setTecnicos] =
        useState<Tecnico[]>([]);

    const [
        carregandoTecnicos,
        setCarregandoTecnicos,
    ] = useState(false);

    const [
        unidadeAtendimentoId,
        setUnidadeAtendimentoId,
    ] = useState(
        ordemServico
            ? String(
            ordemServico.unidadeAtendimentoId
            )
            : ""
    );

    const [tecnicoId, setTecnicoId] = useState(
        ordemServico?.tecnicoId
            ? String(ordemServico.tecnicoId)
            : ""
    );

    const [descricao, setDescricao] = useState(
        ordemServico?.descricao ??
        chamadoOrigem?.descricao ??
        ""
    );

    const [
        numeroPatrimonio,
        setNumeroPatrimonio,
    ] = useState(
        ordemServico?.numeroPatrimonio ??
        chamadoOrigem?.numeroPatrimonio ??
        ""
    );

    const [data, setData] = useState(
        ordemServico?.data ?? hojeISO()
    );

    const [hora, setHora] = useState(
        ordemServico?.hora
            ? ordemServico.hora.slice(0, 5)
            : ""
    );

    const [salvando, setSalvando] = useState(false);
    const [erro, setErro] = useState<string | null>(null);

    useEffect(() => {
        if (!unidadeEditavel) {
            return;
        }

        setCarregandoUnidades(true);

        buscarUnidades(contratoId)
            .then((dados) => setUnidades(dados))
            .catch((error: Error) => setErro(error.message))
            .finally(() => setCarregandoUnidades(false));
    }, [contratoId, unidadeEditavel]);

    useEffect(() => {
        if (!tecnicoEditavel) {
            return;
        }

        setCarregandoTecnicos(true);

        buscarTecnicosPorContrato(contratoId)
            .then((dados) =>
                setTecnicos(
                    dados.filter(
                        (tecnico) => tecnico.ativo
                    )
                )
            )
            .catch((error: Error) => setErro(error.message))
            .finally(() => setCarregandoTecnicos(false));
    }, [contratoId, tecnicoEditavel]);

    async function enviarFormulario(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();

        if (unidadeEditavel && !unidadeAtendimentoId) {
            setErro(
                "Selecione a unidade de atendimento"
            );
            return;
        }

        setErro(null);
        setSalvando(true);

        const chamadoIdEfetivo = chamadoOrigem
            ? chamadoOrigem.id
            : modoEdicao
                ? ordemServico!.chamadoId
                : null;

        const request: OrdemServicoRequest = {
            chamadoId: chamadoIdEfetivo,

            tecnicoId: tecnicoId
                ? Number(tecnicoId)
                : null,

            unidadeAtendimentoId: unidadeEditavel
                ? Number(unidadeAtendimentoId)
                : null,

            descricao: descricao.trim() || null,
            numeroPatrimonio:
                numeroPatrimonio.trim() || null,

            data: data || null,
            hora: hora || null,
        };

        try {
            const ordemSalva = modoEdicao
                ? await atualizarOrdemServico(
                    contratoId,
                    ordemServico!.id,
                    request
                )
                : await criarOrdemServico(
                    contratoId,
                    request
                );

            await aoSalvo(ordemSalva);
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
                            {modoEdicao
                                ? "Editar execução"
                                : "Nova execução"}
                        </span>

                        <h3>
                            {modoEdicao
                                ? `Ordem de serviço ${ordemServico!.numeroOrdemServico}`
                                : "Criar ordem de serviço"}
                        </h3>

                        <p>
                            {chamadoOrigem
                                ? "A ordem será vinculada ao chamado selecionado."
                                : "Preencha os dados da execução. O número é gerado automaticamente."}
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
                        Não foi possível salvar a ordem
                    </strong>

                    <span>{erro}</span>
                </div>
            )}

            <div className="service-order-context">
                {chamadoOrigem && (
                    <>
                        <div>
                            <span className="label">
                                Chamado
                            </span>

                            <strong>
                                OSTI{" "}
                                {chamadoOrigem.numeroChamado}
                            </strong>
                        </div>

                        <div>
                            <span className="label">
                                Unidade
                            </span>

                            <strong>
                                {chamadoOrigem.unidadeNome}
                            </strong>
                        </div>
                    </>
                )}

                {!chamadoOrigem &&
                    modoEdicao &&
                    ordemServico!.numeroChamado && (
                        <div>
                            <span className="label">
                                Chamado vinculado
                            </span>

                            <strong>
                                OSTI{" "}
                                {ordemServico!.numeroChamado}
                            </strong>
                        </div>
                    )}

                {!chamadoOrigem &&
                    !(
                        modoEdicao &&
                        ordemServico!.numeroChamado
                    ) && (
                        <div>
                            <span className="label">
                                Vínculo
                            </span>

                            <strong>
                                Ordem avulsa (sem chamado)
                            </strong>
                        </div>
                    )}

                {(chamadoOrigem?.contratoCidade ??
                    ordemServico?.contratoCidade) && (
                    <div>
                        <span className="label">
                            Contrato
                        </span>

                        <strong>
                            {chamadoOrigem?.contratoCidade ??
                                ordemServico?.contratoCidade}
                        </strong>
                    </div>
                )}
            </div>

            <form
                className="service-order-create-form"
                onSubmit={enviarFormulario}
            >
                <div className="service-order-form-body">
                    <div className="service-order-form-grid">
                        {unidadeEditavel ? (
                            <label className="form-field">
                                <span className="form-field-label">
                                    Unidade de atendimento
                                    <strong>*</strong>
                                </span>

                                <select
                                    value={
                                        unidadeAtendimentoId
                                    }
                                    onChange={(event) =>
                                        setUnidadeAtendimentoId(
                                            event.target
                                                .value
                                        )
                                    }
                                    disabled={
                                        carregandoUnidades
                                    }
                                    required
                                >
                                    <option value="">
                                        {carregandoUnidades
                                            ? "Carregando unidades..."
                                            : "Selecione a unidade"}
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
                            </label>
                        ) : (
                            <label className="form-field">
                                <span className="form-field-label">
                                    Unidade de atendimento
                                </span>

                                <input
                                    type="text"
                                    value={
                                        chamadoOrigem?.unidadeNome ??
                                        ordemServico?.unidadeAtendimentoNome ??
                                        ""
                                    }
                                    disabled
                                />

                                <small>
                                    {checkedIn
                                        ? "Não é possível alterar a unidade após o check-in."
                                        : "Herdada do chamado vinculado."}
                                </small>
                            </label>
                        )}

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
                                    !tecnicoEditavel ||
                                    carregandoTecnicos
                                }
                            >
                                <option value="">
                                    {!tecnicoEditavel
                                        ? (ordemServico?.tecnicoNome ??
                                            "Sem técnico")
                                        : carregandoTecnicos
                                            ? "Carregando técnicos..."
                                            : "Sem técnico atribuído"}
                                </option>

                                {tecnicoEditavel &&
                                    tecnicos.map(
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

                            {!tecnicoEditavel && (
                                <small>
                                    Não é possível alterar o
                                    técnico após o check-in.
                                </small>
                            )}
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Data
                            </span>

                            <input
                                type="date"
                                value={data}
                                onChange={(event) =>
                                    setData(
                                        event.target.value
                                    )
                                }
                                required
                            />
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Horário previsto
                            </span>

                            <input
                                type="time"
                                value={hora}
                                onChange={(event) =>
                                    setHora(
                                        event.target.value
                                    )
                                }
                            />
                        </label>

                        <label className="form-field">
                            <span className="form-field-label">
                                Número de patrimônio
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

                        <label className="form-field form-field-wide">
                            <span className="form-field-label">
                                Descrição
                            </span>

                            <textarea
                                value={descricao}
                                onChange={(event) =>
                                    setDescricao(
                                        event.target.value
                                    )
                                }
                                rows={3}
                                placeholder="Detalhes da execução..."
                            />
                        </label>
                    </div>
                </div>

                <footer className="service-order-form-actions">
                    <div>
                        <strong>
                            {modoEdicao
                                ? "Editando ordem de serviço"
                                : "Distribuição inicial"}
                        </strong>

                        <span>
                            {modoEdicao
                                ? "As alterações são aplicadas imediatamente."
                                : "A ordem pode ser criada sem técnico e atribuída depois."}
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
                            disabled={salvando}
                        >
                            {salvando
                                ? "Salvando..."
                                : modoEdicao
                                    ? "Salvar alterações"
                                    : "Criar ordem de serviço"}
                        </button>
                    </div>
                </footer>
            </form>
        </section>
    );
}

export default OrderForm;
