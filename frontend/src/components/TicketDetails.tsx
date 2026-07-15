import type { Chamado } from "../types";

type TicketDetailsProps = {
    chamado: Chamado;
};

function TicketDetails({
                           chamado,
                       }: TicketDetailsProps) {
    return (
        <section className="card">
            <header className="card-header">
                <div>
                    <span className="label">
                        Chamado OSTI
                    </span>

                    <h1>{chamado.numeroChamado}</h1>
                </div>

                <span className="status">
                    {chamado.status}
                </span>
            </header>

            <div className="grid">
                <div>
                    <span className="label">
                        Unidade
                    </span>

                    <p>{chamado.unidadeNome}</p>
                </div>

                <div>
                    <span className="label">
                        Contrato
                    </span>

                    <p>{chamado.contratoCidade}</p>
                </div>

                <div>
                    <span className="label">
                        Solicitante
                    </span>

                    <p>{chamado.solicitante.nome}</p>
                </div>

                <div>
                    <span className="label">
                        Patrimônio
                    </span>

                    <p>
                        {chamado.numeroPatrimonio ??
                            "Não informado"}
                    </p>
                </div>

                <div>
                    <span className="label">
                        Tipo
                    </span>

                    <p>{chamado.tipo}</p>
                </div>

                <div>
                    <span className="label">
                        Prioridade
                    </span>

                    <p>{chamado.prioridade}</p>
                </div>
            </div>

            <div className="description">
                <span className="label">
                    Descrição
                </span>

                <p>{chamado.descricao}</p>
            </div>

            <a
                href={chamado.linkChamadoOsti}
                target="_blank"
                rel="noreferrer"
                className="link"
            >
                Abrir chamado no OSTI
            </a>
        </section>
    );
}

export default TicketDetails;