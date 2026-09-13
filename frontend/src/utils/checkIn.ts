import {
    ehConflitoDeCheckInAnterior,
    iniciarAtendimento,
} from "../api";

import type { OrdemServico } from "../types";

// Se o técnico já possui um check-in ativo em outra OS, o backend recusa
// o check-in com uma mensagem pedindo confirmação para encerrar o
// atendimento anterior. Retorna null quando o usuário cancela a troca.
export async function iniciarAtendimentoComConfirmacao(
    contratoId: number,
    ordemServicoId: number
): Promise<OrdemServico | null> {
    try {
        return await iniciarAtendimento(
            contratoId,
            ordemServicoId,
            false
        );
    } catch (error) {
        if (!ehConflitoDeCheckInAnterior(error)) {
            throw error;
        }

        const confirmou = window.confirm(
            error.message
        );

        if (!confirmou) {
            return null;
        }

        return await iniciarAtendimento(
            contratoId,
            ordemServicoId,
            true
        );
    }
}
