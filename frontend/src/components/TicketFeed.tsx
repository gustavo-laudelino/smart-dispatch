import type { Chamado } from "../types";

type TicketFeedProps = {
    chamados: Chamado[];
    chamadoSelecionado: Chamado | null;
    carregando: boolean;
    aoSelecionarChamado: (chamado: Chamado) => void;
};

function TicketFeed({
                        chamados,
                        chamadoSelecionado,
                        carregando,
                        aoSelecionarChamado,
                    }: TicketFeedProps) {
    return (
        <section className="card feed-card">
            <h2 className="section-title">Chamados</h2>

            {carregando ? (
                <p>Carregando chamados...</p>
            ) : chamados.length === 0 ? (
                <p>Nenhum chamado encontrado.</p>
            ) : (
                <div className="ticket-list">
                    {chamados.map((chamado) => (
                        <button
                            key={chamado.id}
                            className={
                                chamadoSelecionado?.id === chamado.id
                                    ? "ticket-item selected"
                                    : "ticket-item"
                            }
                            onClick={() =>
                                aoSelecionarChamado(chamado)
                            }
                        >
                            <div>
                                <strong>
                                    OSTI {chamado.numeroChamado}
                                </strong>

                                <p>{chamado.unidadeNome}</p>

                                <small>
                                    {chamado.contratoCidade}
                                </small>
                            </div>

                            <span className="status">
                                {chamado.status}
                            </span>
                        </button>
                    ))}
                </div>
            )}
        </section>
    );
}

export default TicketFeed;