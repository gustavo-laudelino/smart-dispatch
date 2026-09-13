import { apiFetch } from "./auth/apiFetch";

import type {
    BaseOperacional,
    Chamado,
    ChamadoRequest,
    ComentarioChamado,
    Contrato,
    ErroResponse,
    FiltrosOrdemServico,
    HistoricoChamado,
    LoginRequest,
    LoginResponse,
    OrdemServico,
    OrdemServicoRequest,
    StatusChamadoManual,
    SugestaoTecnico,
    Tecnico,
    Unidade,
    Pagina,
} from "./types";


const API_BASE_URL =
    import.meta.env.VITE_API_URL?.replace(
        /\/$/,
        ""
    ) ?? "http://localhost:8080";

async function extrairMensagemErro(response: Response): Promise<string> {
    try {
        const erroResponse: ErroResponse = await response.json();

        return erroResponse.mensagem;
    } catch {
        return "Ocorreu um erro inesperado";
    }
}

export async function login(
    request: LoginRequest
): Promise<LoginResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
    });

    if (!response.ok) {
        const mensagemErro = await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

export async function buscarContratos(): Promise<Contrato[]> {
    const response = await apiFetch(`${API_BASE_URL}/contratos`);

    if (!response.ok) {
        throw new Error("Erro ao buscar contratos");
    }

    return response.json();
}

export async function buscarChamados(
    contratoId: string,
    page: number,
    size: number,
    direction: "asc" | "desc",
    tecnicoId?: number,
    meus?: boolean
): Promise<Pagina<Chamado>> {
    const parametros = new URLSearchParams({
        page: String(page),
        size: String(size),
        direction,
    });

    if (contratoId !== "todos") {
        parametros.set(
            "contratoId",
            contratoId
        );
    }

    if (tecnicoId !== undefined) {
        parametros.set(
            "tecnicoId",
            String(tecnicoId)
        );
    }

    if (meus) {
        parametros.set(
            "meus",
            "true"
        );
    }

    const response = await apiFetch(
        `${API_BASE_URL}/chamados?${parametros.toString()}`
    );

    if (!response.ok) {
        throw new Error(
            "Erro ao buscar chamados"
        );
    }

    return response.json();
}

export async function buscarTecnicosPorContrato(
    contratoId: number
): Promise<Tecnico[]> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/tecnicos`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar técnicos");
    }

    return response.json();
}
export async function buscarDetalhesChamado(
    contratoId: number,
    chamadoId: number
): Promise<Chamado> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar detalhes do chamado");
    }

    return response.json();
}

export async function atualizarChamado(
    contratoId: number,
    chamadoId: number,
    chamadoRequest: ChamadoRequest
): Promise<Chamado> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(chamadoRequest),
        }
    );

    if (!response.ok) {
        const mensagemErro =
            await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

export async function buscarOrdensServico(
    contratoId: number,
    filtros: FiltrosOrdemServico = {}
): Promise<OrdemServico[]> {
    const parametros = new URLSearchParams();

    if (filtros.chamadoId !== undefined) {
        parametros.set("chamadoId", String(filtros.chamadoId));
    }

    if (filtros.tecnicoId !== undefined) {
        parametros.set("tecnicoId", String(filtros.tecnicoId));
    }

    if (filtros.meus) {
        parametros.set("meus", "true");
    }

    if (filtros.data) {
        parametros.set("data", filtros.data);
    }

    if (filtros.dataInicio) {
        parametros.set("dataInicio", filtros.dataInicio);
    }

    if (filtros.dataFim) {
        parametros.set("dataFim", filtros.dataFim);
    }

    const query = parametros.toString();

    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/ordens-servico${
            query ? `?${query}` : ""
        }`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar ordens de serviço");
    }

    return response.json();
}

export async function buscarOrdemServicoPorId(
    contratoId: number,
    ordemServicoId: number
): Promise<OrdemServico> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/ordens-servico/${ordemServicoId}`
    );

    if (!response.ok) {
        const mensagemErro =
            await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

export async function buscarSugestoesTecnicos(
    contratoId: number,
    ordemServicoId: number
): Promise<SugestaoTecnico[]> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/ordens-servico/${ordemServicoId}/sugestoes-tecnicos`
    );

    if (!response.ok) {
        const mensagemErro =
            await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

export async function buscarComentarios(
    contratoId: number,
    chamadoId: number
): Promise<ComentarioChamado[]> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/comentarios`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar comentários");
    }

    return response.json();
}

export async function buscarHistoricoChamado(
    contratoId: number,
    chamadoId: number
): Promise<HistoricoChamado[]> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/historico`
    );

    if (!response.ok) {
        const mensagemErro =
            await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

export async function iniciarAtendimento(
    contratoId: number,
    ordemServicoId: number,
    encerrarCheckInAnterior = false
): Promise<OrdemServico> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/ordens-servico/${ordemServicoId}/check-in`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                encerrarCheckInAnterior,
            }),
        }
    );

    if (!response.ok) {
        const mensagemErro = await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

export async function finalizarAtendimento(
    contratoId: number,
    ordemServicoId: number
): Promise<OrdemServico> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/ordens-servico/${ordemServicoId}/check-out`,
        {
            method: "POST",
        }
    );

    if (!response.ok) {
        const mensagemErro = await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

export async function adicionarComentario(
    contratoId: number,
    chamadoId: number,
    autorId: number,
    ordemServicoId: number | null,
    texto: string
): Promise<void> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/comentarios`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                autorId,
                ordemServicoId,
                texto,
            }),
        }
    );

    if (!response.ok) {
        const mensagemErro = await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }
}

export async function buscarUnidades(
    contratoId: number
): Promise<Unidade[]> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/unidades`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar unidades");
    }

    return response.json();
}

export async function criarChamado(
    contratoId: number,
    chamadoRequest: ChamadoRequest
): Promise<Chamado> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(chamadoRequest),
        }
    );

    if (!response.ok) {
        const mensagemErro = await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

export async function buscarBasesOperacionais(
    contratoId: number
): Promise<BaseOperacional[]> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/bases`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar bases operacionais");
    }

    return response.json();
}

export async function buscarTecnicos(
    contratoId: number,
    baseId: number
): Promise<Tecnico[]> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/bases/${baseId}/tecnicos`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar técnicos");
    }

    return response.json();
}

export async function criarOrdemServico(
    contratoId: number,
    ordemServicoRequest: OrdemServicoRequest
): Promise<OrdemServico> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/ordens-servico`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(ordemServicoRequest),
        }
    );

    if (!response.ok) {
        const mensagemErro =
            await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

export async function atualizarOrdemServico(
    contratoId: number,
    ordemServicoId: number,
    ordemServicoRequest: OrdemServicoRequest
): Promise<OrdemServico> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/ordens-servico/${ordemServicoId}`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(ordemServicoRequest),
        }
    );

    if (!response.ok) {
        const mensagemErro =
            await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}

const MARCADOR_CONFLITO_CHECKIN_ANTERIOR =
    "Deseja encerrar esse atendimento e iniciar a OS";

export function ehConflitoDeCheckInAnterior(
    erro: unknown
): erro is Error {
    return (
        erro instanceof Error &&
        erro.message.includes(
            MARCADOR_CONFLITO_CHECKIN_ANTERIOR
        )
    );
}

export async function atualizarStatusChamado(
    contratoId: number,
    chamadoId: number,
    status: StatusChamadoManual
): Promise<Chamado> {
    const response = await apiFetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/status`,
        {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                status,
            }),
        }
    );

    if (!response.ok) {
        const mensagemErro =
            await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }

    return response.json();
}
