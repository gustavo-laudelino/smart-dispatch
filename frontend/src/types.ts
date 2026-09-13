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
    numeroChamadoInterno: number;
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
    numeroOrdemServico: number;

    contratoId: number;
    contratoCidade: string;

    chamadoId: number | null;
    numeroChamado: string | null;

    descricao: string | null;
    numeroPatrimonio: string | null;

    tecnicoId: number | null;
    tecnicoNome: string | null;
    dataAtribuicaoTecnico: string | null;

    data: string;
    hora: string | null;

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
    numeroOrdemServico: number | null;
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
    chamadoId: number | null;
    tecnicoId: number | null;
    unidadeAtendimentoId: number | null;
    descricao: string | null;
    numeroPatrimonio: string | null;
    data: string | null;
    hora: string | null;
};

export type FiltrosOrdemServico = {
    chamadoId?: number;
    tecnicoId?: number;
    meus?: boolean;
    data?: string;
    dataInicio?: string;
    dataFim?: string;
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
    numeroOrdemServico: number | null;

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

export type PerfilUsuario =
    | "ADMIN"
    | "CTO"
    | "TECNICO"
    | "TECNICO_INTERNO";

export type LoginRequest = {
    email: string;
    senha: string;
};

export type LoginResponse = {
    token: string;
    tipo: string;
    expiraEmSegundos: number;
    usuarioId: number;
    nome: string;
    email: string;
    perfil: PerfilUsuario;
};

export type AuthSession = {
    token: string;
    usuarioId: number;
    nome: string;
    email: string;
    perfil: PerfilUsuario;
};