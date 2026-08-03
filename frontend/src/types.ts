export type StatusChamado =
    | "ABERTO"
    | "ATRIBUIDO"
    | "EM_ATENDIMENTO"
    | "AGUARDANDO_ANALISE"
    | "PRONTO_PARA_FINALIZAR"
    | "PENDENTE"
    | "AGUARDANDO_CLIENTE"
    | "FINALIZADO"
    | "CANCELADO";

export type StatusChamadoManual =
    | "AGUARDANDO_ANALISE"
    | "PRONTO_PARA_FINALIZAR"
    | "PENDENTE"
    | "AGUARDANDO_CLIENTE"
    | "FINALIZADO"
    | "CANCELADO";

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
    status: StatusChamado;
    descricao: string;
    dataAbertura: string;
    dataFinalizacao: string | null;
};

export type OrdemServico = {
    id: number;
    numeroOrdemServico: string;
    chamadoId: number;
    numeroChamado: string;
    tecnicoId: number | null;
    tecnicoNome: string | null;
    dataAtribuicaoTecnico: string | null;
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

export type Unidade = {
    id: number;
    nome: string;
};

export type ChamadoRequest = {
    numeroChamado: string;
    linkChamadoOsti: string;
    unidadeId: number;
    solicitante: Solicitante;
    numeroPatrimonio: string | null;
    tipo: string;
    categoria: string;
    prioridade: string;
    descricao: string;
};

export type BaseOperacional = {
    id: number;
    nome: string;
};

export type Tecnico = {
    id: number;
    nome: string;
    email: string | null;
    telefone: string | null;
    perfil: string;
    ativo: boolean;
    baseId: number;
    baseNome: string;
    contratoId: number;
    contratoCidade: string;
};

export type OrdemServicoRequest = {
    numeroOrdemServico: string;
    tecnicoId: number | null;
    unidadeAtendimentoId: number | null;
};

export type NivelIndicacao =
    | "LEVE"
    | "MODERADA"
    | "ALTA";

export type SugestaoTecnico = {
    tecnicoId: number;
    tecnicoNome: string;

    pontuacao: number;
    distanciaKm: number;

    quantidadeOsAtivas: number;
    atribuicoesHoje: number;
    atendimentosUltimos15Dias: number;

    nivelIndicacao: NivelIndicacao;
    estrelas: number;
};

export type TipoEventoChamado =
    | "CHAMADO_CRIADO"
    | "DADOS_CHAMADO_ALTERADOS"
    | "STATUS_ALTERADO"
    | "ORDEM_SERVICO_CRIADA"
    | "ORDEM_SERVICO_ALTERADA"
    | "TECNICO_ATRIBUIDO"
    | "TECNICO_ALTERADO"
    | "TECNICO_REMOVIDO"
    | "UNIDADE_ORDEM_ALTERADA"
    | "ATENDIMENTO_INICIADO"
    | "ATENDIMENTO_FINALIZADO"
    | "ATENDIMENTO_FINALIZADO_AUTOMATICAMENTE";

export type HistoricoChamado = {
    id: number;
    chamadoId: number;

    ordemServicoId: number | null;
    numeroOrdemServico: string | null;

    tipoEvento: TipoEventoChamado;
    descricao: string;
    dataEvento: string;


};

export type Pagina<T> = {
    content: T[];
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    numberOfElements: number;
    empty: boolean;
};