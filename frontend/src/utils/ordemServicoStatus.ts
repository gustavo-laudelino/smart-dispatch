import type { OrdemServico } from "../types";

export type StatusVisualOrdem = {
    rotulo: string;
    classe: string;
};

export function definirStatusOrdemServico(
    ordemServico: OrdemServico
): StatusVisualOrdem {
    if (
        ordemServico.dataCheckIn &&
        ordemServico.dataCheckOut
    ) {
        return {
            rotulo: "Encerrada",
            classe: "encerrada",
        };
    }

    if (
        ordemServico.dataCheckIn &&
        !ordemServico.dataCheckOut
    ) {
        return {
            rotulo: "Em atendimento",
            classe: "em-atendimento",
        };
    }

    if (ordemServico.tecnicoId === null) {
        return {
            rotulo: "Aguardando atribuição",
            classe: "sem-tecnico",
        };
    }

    return {
        rotulo: "Aguardando início",
        classe: "aguardando-inicio",
    };
}
