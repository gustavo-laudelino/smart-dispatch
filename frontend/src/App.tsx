import { useEffect, useState } from "react";
import "./App.css";

type Solicitante = {
  nome: string;
  email: string | null;
  telefone: string | null;
  identificacao: string | null;
};

type Chamado = {
  id: number;
  numeroChamado: string;
  linkChamadoOsti: string;
  unidadeId: number;
  unidadeNome: string;
  contratoId: number;
  contratoCidade: string;
  solicitante: Solicitante;
  numeroPatrimonio: string | null;
  tipo: string;
  categoria: string;
  prioridade: string;
  status: string;
  descricao: string;
  dataAbertura: string;
  dataFinalizacao: string | null;
};

function App() {
  const [chamado, setChamado] = useState<Chamado | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    fetch("http://localhost:8080/contratos/1/chamados/3")
        .then((response) => {
          if (!response.ok) {
            throw new Error("Erro ao buscar chamado");
          }

          return response.json();
        })
        .then((data: Chamado) => {
          setChamado(data);
        })
        .catch((error) => {
          setErro(error.message);
        })
        .finally(() => {
          setCarregando(false);
        });
  }, []);

  if (carregando) {
    return <p>Carregando chamado...</p>;
  }

  if (erro) {
    return <p>Erro: {erro}</p>;
  }

  if (!chamado) {
    return <p>Chamado não encontrado.</p>;
  }

  return (
      <main className="page">
        <section className="card">
          <header className="card-header">
            <div>
              <span className="label">Chamado OSTI</span>
              <h1>{chamado.numeroChamado}</h1>
            </div>

            <span className="status">{chamado.status}</span>
          </header>

          <div className="grid">
            <div>
              <span className="label">Unidade</span>
              <p>{chamado.unidadeNome}</p>
            </div>

            <div>
              <span className="label">Contrato</span>
              <p>{chamado.contratoCidade}</p>
            </div>

            <div>
              <span className="label">Solicitante</span>
              <p>{chamado.solicitante.nome}</p>
            </div>

            <div>
              <span className="label">Patrimônio</span>
              <p>{chamado.numeroPatrimonio ?? "Não informado"}</p>
            </div>

            <div>
              <span className="label">Tipo</span>
              <p>{chamado.tipo}</p>
            </div>

            <div>
              <span className="label">Prioridade</span>
              <p>{chamado.prioridade}</p>
            </div>
          </div>

          <div className="description">
            <span className="label">Descrição</span>
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
      </main>
  );
}

export default App;