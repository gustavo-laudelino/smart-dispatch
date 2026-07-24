import { useEffect } from "react";
import { createPortal } from "react-dom";

import AssignTechnicianForm from "./AssignTechnicianForm";

import type {
    Chamado,
    OrdemServico,
} from "../types";

type TechnicianDrawerProps = {
    chamado: Chamado;
    ordemServico: OrdemServico;

    aoCancelar: () => void;

    aoTecnicoAtribuido: (
        ordemServico: OrdemServico
    ) => void | Promise<void>;
};

function TechnicianDrawer({
                              chamado,
                              ordemServico,
                              aoCancelar,
                              aoTecnicoAtribuido,
                          }: TechnicianDrawerProps) {
    useEffect(() => {
        const overflowAnterior =
            document.body.style.overflow;

        document.body.style.overflow =
            "hidden";

        function fecharComEscape(
            event: KeyboardEvent
        ) {
            if (event.key === "Escape") {
                aoCancelar();
            }
        }

        document.addEventListener(
            "keydown",
            fecharComEscape
        );

        return () => {
            document.body.style.overflow =
                overflowAnterior;

            document.removeEventListener(
                "keydown",
                fecharComEscape
            );
        };
    }, [aoCancelar]);

    return createPortal(
        <div
            className="technician-drawer-overlay"
            onMouseDown={(event) => {
                if (
                    event.target ===
                    event.currentTarget
                ) {
                    aoCancelar();
                }
            }}
        >
            <aside
                className="technician-drawer"
                role="dialog"
                aria-modal="true"
                aria-label={`Selecionar técnico para a ordem ${ordemServico.numeroOrdemServico}`}
            >
                <AssignTechnicianForm
                    chamado={chamado}
                    ordemServico={
                        ordemServico
                    }
                    aoCancelar={aoCancelar}
                    aoTecnicoAtribuido={
                        aoTecnicoAtribuido
                    }
                />
            </aside>
        </div>,
        document.body
    );
}

export default TechnicianDrawer;