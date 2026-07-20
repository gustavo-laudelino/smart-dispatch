export function FeedSkeleton() {
    return (
        <div
            className="feed-skeleton"
            aria-label="Carregando chamados"
            aria-busy="true"
        >
            {[1, 2, 3, 4].map((item) => (
                <div
                    key={item}
                    className="feed-skeleton-item"
                >
                    <div className="skeleton-row">
                        <span className="skeleton-block skeleton-ticket-number" />

                        <span className="skeleton-block skeleton-status" />
                    </div>

                    <span className="skeleton-block skeleton-unit" />

                    <div className="skeleton-row">
                        <span className="skeleton-block skeleton-city" />

                        <span className="skeleton-block skeleton-priority" />
                    </div>
                </div>
            ))}
        </div>
    );
}

export function DetailSkeleton() {
    return (
        <section
            className="card detail-skeleton"
            aria-label="Carregando detalhes do chamado"
            aria-busy="true"
        >
            <header className="detail-skeleton-header">
                <div>
                    <span className="skeleton-block skeleton-detail-label" />

                    <span className="skeleton-block skeleton-detail-title" />

                    <span className="skeleton-block skeleton-detail-subtitle" />
                </div>

                <span className="skeleton-block skeleton-detail-status" />
            </header>

            <div className="detail-skeleton-grid">
                {[1, 2, 3, 4, 5, 6].map(
                    (item) => (
                        <div
                            key={item}
                            className="detail-skeleton-field"
                        >
                            <span className="skeleton-block skeleton-field-label" />

                            <span className="skeleton-block skeleton-field-value" />
                        </div>
                    )
                )}
            </div>

            <div className="detail-skeleton-description">
                <span className="skeleton-block skeleton-field-label" />

                <span className="skeleton-block skeleton-description-line" />

                <span className="skeleton-block skeleton-description-line short" />
            </div>

            <footer className="detail-skeleton-footer">
                <span className="skeleton-block skeleton-footer-text" />

                <span className="skeleton-block skeleton-footer-button" />
            </footer>
        </section>
    );
}