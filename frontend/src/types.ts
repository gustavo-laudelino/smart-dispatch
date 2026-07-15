export type Contrato = {
    id: number;
    cidade: string;
};

export type Solicitante = {
    nome: string;
    email: string | null;
    telefone: string | null;
    identificacao: string | null;
};

export type Chamado = {
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

export type OrdemServico = {
    id: number;
    numeroOrdemServico: string;
    chamadoId: number;
    numeroChamado: string;
    tecnicoId: number;
    tecnicoNome: string;
    unidadeAtendimentoId: number;
    unidadeAtendimentoNome: string;
    dataCheckIn: string | null;
    dataCheckOut: string | null;
};

export type ComentarioChamado = {
    id: number;
    chamadoId: number;
    autorId: number;
    autorNome: string;
    ordemServicoId: number | null;
    numeroOrdemServico: string | null;
    texto: string;
    dataCriacao: string;
};

export type ErroResponse = {
    dataHora: string;
    status: number;
    erro: string;
    mensagem: string;
    caminho: string;
};