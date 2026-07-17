import type {
    BaseOperacional,
    Chamado,
    ChamadoRequest,
    ComentarioChamado,
    Contrato,
    ErroResponse,
    OrdemServico,
    OrdemServicoRequest,
    StatusChamadoManual,
    Tecnico,
    Unidade,
    SugestaoTecnico,
} from "./types";

const API_BASE_URL = "http://localhost:8080";

async function extrairMensagemErro(response: Response): Promise<string> {
    try {
        const erroResponse: ErroResponse = await response.json();

        return erroResponse.mensagem;
    } catch {
        return "Ocorreu um erro inesperado";
    }
}

export async function buscarContratos(): Promise<Contrato[]> {
    const response = await fetch(`${API_BASE_URL}/contratos`);

    if (!response.ok) {
        throw new Error("Erro ao buscar contratos");
    }

    return response.json();
}

export async function buscarChamados(
    contratoId: string
): Promise<Chamado[]> {
    const url =
        contratoId === "todos"
            ? `${API_BASE_URL}/chamados`
            : `${API_BASE_URL}/chamados?contratoId=${contratoId}`;

    const response = await fetch(url);

    if (!response.ok) {
        throw new Error("Erro ao buscar chamados");
    }

    return response.json();
}

export async function buscarDetalhesChamado(
    contratoId: number,
    chamadoId: number
): Promise<Chamado> {
    const response = await fetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar detalhes do chamado");
    }

    return response.json();
}

export async function buscarOrdensServico(
    contratoId: number,
    chamadoId: number
): Promise<OrdemServico[]> {
    const response = await fetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/ordens-servico`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar ordens de serviço");
    }

    return response.json();
}

export async function buscarSugestoesTecnicos(
    contratoId: number,
    chamadoId: number,
    ordemServicoId: number
): Promise<SugestaoTecnico[]> {
    const response = await fetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/ordens-servico/${ordemServicoId}/sugestoes-tecnicos`
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
    const response = await fetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/comentarios`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar comentários");
    }

    return response.json();
}

export async function iniciarAtendimento(
    contratoId: number,
    chamadoId: number,
    ordemServicoId: number
): Promise<void> {
    const response = await fetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/ordens-servico/${ordemServicoId}/check-in`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                encerrarCheckInAnterior: false,
            }),
        }
    );

    if (!response.ok) {
        const mensagemErro = await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }
}

export async function finalizarAtendimento(
    contratoId: number,
    chamadoId: number,
    ordemServicoId: number
): Promise<void> {
    const response = await fetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/ordens-servico/${ordemServicoId}/check-out`,
        {
            method: "POST",
        }
    );

    if (!response.ok) {
        const mensagemErro = await extrairMensagemErro(response);

        throw new Error(mensagemErro);
    }
}

export async function adicionarComentario(
    contratoId: number,
    chamadoId: number,
    autorId: number,
    ordemServicoId: number | null,
    texto: string
): Promise<void> {
    const response = await fetch(
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
    const response = await fetch(
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
    const response = await fetch(
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
    const response = await fetch(
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
    const response = await fetch(
        `${API_BASE_URL}/contratos/${contratoId}/bases/${baseId}/tecnicos`
    );

    if (!response.ok) {
        throw new Error("Erro ao buscar técnicos");
    }

    return response.json();
}

export async function criarOrdemServico(
    contratoId: number,
    chamadoId: number,
    ordemServicoRequest: OrdemServicoRequest
): Promise<OrdemServico> {
    const response = await fetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/ordens-servico`,
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
    chamadoId: number,
    ordemServicoId: number,
    ordemServicoRequest: OrdemServicoRequest
): Promise<OrdemServico> {
    const response = await fetch(
        `${API_BASE_URL}/contratos/${contratoId}/chamados/${chamadoId}/ordens-servico/${ordemServicoId}`,
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

export async function atualizarStatusChamado(
    contratoId: number,
    chamadoId: number,
    status: StatusChamadoManual
): Promise<Chamado> {
    const response = await fetch(
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