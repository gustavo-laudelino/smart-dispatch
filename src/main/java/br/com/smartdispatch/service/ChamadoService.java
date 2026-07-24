package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.ChamadoRequest;
import br.com.smartdispatch.dto.ChamadoResponse;
import br.com.smartdispatch.dto.StatusChamadoRequest;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoEventoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Solicitante;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.repository.ChamadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UnidadeService unidadeService;
    private final ContratoService contratoService;
    private final HistoricoChamadoService historicoChamadoService;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            UnidadeService unidadeService,
            ContratoService contratoService,
            HistoricoChamadoService historicoChamadoService
    ) {
        this.chamadoRepository =
                chamadoRepository;

        this.unidadeService =
                unidadeService;

        this.contratoService =
                contratoService;

        this.historicoChamadoService =
                historicoChamadoService;
    }

    @Transactional
    public ChamadoResponse criar(
            Long contratoId,
            ChamadoRequest request
    ) {
        Unidade unidade =
                unidadeService.buscarPorId(
                        contratoId,
                        request.getUnidadeId()
                );

        boolean numeroJaCadastrado =
                chamadoRepository
                        .existsByNumeroChamadoAndUnidadeContratoId(
                                request.getNumeroChamado(),
                                contratoId
                        );

        if (numeroJaCadastrado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um chamado com este número neste contrato"
            );
        }

        Chamado chamado =
                new Chamado();

        aplicarDados(
                chamado,
                request,
                unidade
        );

        Chamado chamadoSalvo =
                chamadoRepository.save(
                        chamado
                );

        historicoChamadoService.registrar(
                chamadoSalvo,
                null,
                TipoEventoChamado.CHAMADO_CRIADO,
                "Chamado "
                        + chamadoSalvo.getNumeroChamado()
                        + " criado para a unidade "
                        + unidade.getNome()
                        + "."
        );

        return converterParaResponse(
                chamadoSalvo
        );
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> listarPorContrato(
            Long contratoId
    ) {
        contratoService.buscarPorId(
                contratoId
        );

        return chamadoRepository
                .findByUnidadeContratoId(
                        contratoId
                )
                .stream()
                .sorted(
                        Comparator
                                .comparing(
                                        Chamado::getDataAbertura
                                )
                                .reversed()
                )
                .map(this::converterParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> listarFeed(
            Long contratoId
    ) {
        if (contratoId != null) {
            return listarPorContrato(
                    contratoId
            );
        }

        return chamadoRepository
                .findAll()
                .stream()
                .sorted(
                        Comparator
                                .comparing(
                                        Chamado::getDataAbertura
                                )
                                .reversed()
                )
                .map(this::converterParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscarPorId(
            Long contratoId,
            Long chamadoId
    ) {
        Chamado chamado =
                buscarEntidadePorId(
                        contratoId,
                        chamadoId
                );

        return converterParaResponse(
                chamado
        );
    }

    @Transactional
    public ChamadoResponse atualizar(
            Long contratoId,
            Long chamadoId,
            ChamadoRequest request
    ) {
        Chamado chamado =
                buscarEntidadePorId(
                        contratoId,
                        chamadoId
                );

        Unidade unidade =
                unidadeService.buscarPorId(
                        contratoId,
                        request.getUnidadeId()
                );

        boolean numeroPertenceAOutroChamado =
                chamadoRepository
                        .existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                                request.getNumeroChamado(),
                                contratoId,
                                chamadoId
                        );

        if (numeroPertenceAOutroChamado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe outro chamado com este número neste contrato"
            );
        }

        String descricaoAlteracoes =
                criarDescricaoAlteracoes(
                        chamado,
                        request,
                        unidade
                );

        aplicarDados(
                chamado,
                request,
                unidade
        );

        Chamado chamadoAtualizado =
                chamadoRepository.save(
                        chamado
                );

        if (descricaoAlteracoes != null) {
            historicoChamadoService.registrar(
                    chamadoAtualizado,
                    null,
                    TipoEventoChamado.DADOS_CHAMADO_ALTERADOS,
                    descricaoAlteracoes
            );
        }

        return converterParaResponse(
                chamadoAtualizado
        );
    }

    @Transactional
    public ChamadoResponse atualizarStatus(
            Long contratoId,
            Long chamadoId,
            StatusChamadoRequest request
    ) {
        if (
                request == null
                        || request.getStatus() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O status do chamado deve ser informado"
            );
        }

        StatusChamado novoStatus =
                request.getStatus();

        if (
                novoStatus == StatusChamado.ABERTO
                        || novoStatus == StatusChamado.ATRIBUIDO
                        || novoStatus == StatusChamado.EM_ATENDIMENTO
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Este status é controlado automaticamente pelo sistema"
            );
        }

        Chamado chamado =
                buscarEntidadePorId(
                        contratoId,
                        chamadoId
                );

        StatusChamado statusAnterior =
                chamado.getStatus();

        if (statusAnterior == novoStatus) {
            return converterParaResponse(
                    chamado
            );
        }

        chamado.setStatus(
                novoStatus
        );

        if (
                novoStatus
                        == StatusChamado.FINALIZADO
        ) {
            chamado.setDataFinalizacao(
                    LocalDateTime.now()
            );
        } else {
            chamado.setDataFinalizacao(
                    null
            );
        }

        Chamado chamadoAtualizado =
                chamadoRepository.save(
                        chamado
                );

        historicoChamadoService.registrar(
                chamadoAtualizado,
                null,
                TipoEventoChamado.STATUS_ALTERADO,
                "Status alterado de "
                        + formatarEnum(statusAnterior)
                        + " para "
                        + formatarEnum(novoStatus)
                        + "."
        );

        return converterParaResponse(
                chamadoAtualizado
        );
    }

    @Transactional(readOnly = true)
    public Chamado buscarEntidadePorId(
            Long contratoId,
            Long chamadoId
    ) {
        return chamadoRepository
                .findByIdAndUnidadeContratoId(
                        chamadoId,
                        contratoId
                )
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Chamado não encontrado neste contrato"
                        )
                );
    }

    private void aplicarDados(
            Chamado chamado,
            ChamadoRequest request,
            Unidade unidade
    ) {
        chamado.setNumeroChamado(
                request.getNumeroChamado()
        );

        chamado.setLinkChamadoOsti(
                request.getLinkChamadoOsti()
        );

        chamado.setUnidade(
                unidade
        );

        chamado.setSolicitante(
                request.getSolicitante()
        );

        chamado.setNumeroPatrimonio(
                request.getNumeroPatrimonio()
        );

        chamado.setTipo(
                request.getTipo()
        );

        chamado.setCategoria(
                request.getCategoria()
        );

        chamado.setPrioridade(
                request.getPrioridade()
        );

        chamado.setDescricao(
                request.getDescricao()
        );
    }

    private String criarDescricaoAlteracoes(
            Chamado chamado,
            ChamadoRequest request,
            Unidade novaUnidade
    ) {
        List<String> alteracoes =
                new ArrayList<>();

        if (
                !Objects.equals(
                        chamado.getNumeroChamado(),
                        request.getNumeroChamado()
                )
        ) {
            alteracoes.add(
                    "número do chamado de "
                            + valorOuNaoInformado(
                            chamado.getNumeroChamado()
                    )
                            + " para "
                            + valorOuNaoInformado(
                            request.getNumeroChamado()
                    )
            );
        }

        if (
                chamado.getUnidade() == null
                        || !Objects.equals(
                        chamado.getUnidade().getId(),
                        novaUnidade.getId()
                )
        ) {
            String unidadeAnterior =
                    chamado.getUnidade() == null
                            ? "não informada"
                            : chamado
                              .getUnidade()
                              .getNome();

            alteracoes.add(
                    "unidade de "
                            + unidadeAnterior
                            + " para "
                            + novaUnidade.getNome()
            );
        }

        if (
                !Objects.equals(
                        chamado.getLinkChamadoOsti(),
                        request.getLinkChamadoOsti()
                )
        ) {
            alteracoes.add(
                    "link do OSTI atualizado"
            );
        }

        adicionarAlteracoesSolicitante(
                alteracoes,
                chamado.getSolicitante(),
                request.getSolicitante()
        );

        if (
                !Objects.equals(
                        chamado.getNumeroPatrimonio(),
                        request.getNumeroPatrimonio()
                )
        ) {
            alteracoes.add(
                    "patrimônio de "
                            + valorOuNaoInformado(
                            chamado.getNumeroPatrimonio()
                    )
                            + " para "
                            + valorOuNaoInformado(
                            request.getNumeroPatrimonio()
                    )
            );
        }

        if (
                !Objects.equals(
                        chamado.getTipo(),
                        request.getTipo()
                )
        ) {
            alteracoes.add(
                    "tipo de "
                            + formatarEnum(
                            chamado.getTipo()
                    )
                            + " para "
                            + formatarEnum(
                            request.getTipo()
                    )
            );
        }

        if (
                !Objects.equals(
                        chamado.getCategoria(),
                        request.getCategoria()
                )
        ) {
            alteracoes.add(
                    "categoria de "
                            + formatarEnum(
                            chamado.getCategoria()
                    )
                            + " para "
                            + formatarEnum(
                            request.getCategoria()
                    )
            );
        }

        if (
                !Objects.equals(
                        chamado.getPrioridade(),
                        request.getPrioridade()
                )
        ) {
            alteracoes.add(
                    "prioridade de "
                            + formatarEnum(
                            chamado.getPrioridade()
                    )
                            + " para "
                            + formatarEnum(
                            request.getPrioridade()
                    )
            );
        }

        if (
                !Objects.equals(
                        chamado.getDescricao(),
                        request.getDescricao()
                )
        ) {
            alteracoes.add(
                    "descrição atualizada"
            );
        }

        if (alteracoes.isEmpty()) {
            return null;
        }

        return "Dados do chamado alterados: "
                + String.join(
                "; ",
                alteracoes
        )
                + ".";
    }

    private void adicionarAlteracoesSolicitante(
            List<String> alteracoes,
            Solicitante solicitanteAnterior,
            Solicitante novoSolicitante
    ) {
        String nomeAnterior =
                obterNomeSolicitante(
                        solicitanteAnterior
                );

        String novoNome =
                obterNomeSolicitante(
                        novoSolicitante
                );

        if (
                !Objects.equals(
                        nomeAnterior,
                        novoNome
                )
        ) {
            alteracoes.add(
                    "solicitante de "
                            + valorOuNaoInformado(
                            nomeAnterior
                    )
                            + " para "
                            + valorOuNaoInformado(
                            novoNome
                    )
            );
        }

        if (
                !Objects.equals(
                        obterEmailSolicitante(
                                solicitanteAnterior
                        ),
                        obterEmailSolicitante(
                                novoSolicitante
                        )
                )
        ) {
            alteracoes.add(
                    "e-mail do solicitante atualizado"
            );
        }

        if (
                !Objects.equals(
                        obterTelefoneSolicitante(
                                solicitanteAnterior
                        ),
                        obterTelefoneSolicitante(
                                novoSolicitante
                        )
                )
        ) {
            alteracoes.add(
                    "telefone do solicitante atualizado"
            );
        }

        if (
                !Objects.equals(
                        obterIdentificacaoSolicitante(
                                solicitanteAnterior
                        ),
                        obterIdentificacaoSolicitante(
                                novoSolicitante
                        )
                )
        ) {
            alteracoes.add(
                    "identificação do solicitante atualizada"
            );
        }
    }

    private String obterNomeSolicitante(
            Solicitante solicitante
    ) {
        return solicitante == null
                ? null
                : solicitante.getNome();
    }

    private String obterEmailSolicitante(
            Solicitante solicitante
    ) {
        return solicitante == null
                ? null
                : solicitante.getEmail();
    }

    private String obterTelefoneSolicitante(
            Solicitante solicitante
    ) {
        return solicitante == null
                ? null
                : solicitante.getTelefone();
    }

    private String obterIdentificacaoSolicitante(
            Solicitante solicitante
    ) {
        return solicitante == null
                ? null
                : solicitante.getIdentificacao();
    }

    private String valorOuNaoInformado(
            String valor
    ) {
        if (
                valor == null
                        || valor.isBlank()
        ) {
            return "não informado";
        }

        return valor;
    }

    private String formatarEnum(
            Enum<?> valor
    ) {
        if (valor == null) {
            return "não informado";
        }

        return switch (valor.name()) {
            case "ABERTO" ->
                    "Aberto";

            case "ATRIBUIDO" ->
                    "Atribuído";

            case "EM_ATENDIMENTO" ->
                    "Em atendimento";

            case "AGUARDANDO_ANALISE" ->
                    "Aguardando análise";

            case "PRONTO_PARA_FINALIZAR" ->
                    "Pronto para finalizar";

            case "PENDENTE" ->
                    "Pendente";

            case "AGUARDANDO_CLIENTE" ->
                    "Aguardando cliente";

            case "FINALIZADO" ->
                    "Finalizado";

            case "CANCELADO" ->
                    "Cancelado";

            case "INCIDENTE" ->
                    "Incidente";

            case "REQUISICAO" ->
                    "Requisição";

            case "BAIXA" ->
                    "Baixa";

            case "MEDIA" ->
                    "Média";

            case "ALTA" ->
                    "Alta";

            case "URGENTE" ->
                    "Urgente";

            case "COMPUTADOR_COM_DEFEITO" ->
                    "Computador com defeito";

            case "INSTALACAO_DE_PROGRAMAS" ->
                    "Instalação de programas";

            case "PROJETOR_TELA_INTERATIVA_COM_DEFEITO" ->
                    "Projetor ou tela interativa com defeito";

            case "OUTROS" ->
                    "Outros";

            default -> {
                String texto =
                        valor.name()
                                .toLowerCase(
                                        Locale.ROOT
                                )
                                .replace(
                                        '_',
                                        ' '
                                );

                yield texto.substring(0, 1)
                        .toUpperCase(
                                Locale.ROOT
                        )
                        + texto.substring(1);
            }
        };
    }

    private ChamadoResponse converterParaResponse(
            Chamado chamado
    ) {
        Unidade unidade =
                chamado.getUnidade();

        Contrato contrato =
                unidade.getContrato();

        return new ChamadoResponse(
                chamado.getId(),
                chamado.getNumeroChamado(),
                chamado.getLinkChamadoOsti(),
                unidade.getId(),
                unidade.getNome(),
                contrato.getId(),
                contrato.getCidade(),
                chamado.getSolicitante(),
                chamado.getNumeroPatrimonio(),
                chamado.getTipo(),
                chamado.getCategoria(),
                chamado.getPrioridade(),
                chamado.getStatus(),
                chamado.getDescricao(),
                chamado.getDataAbertura(),
                chamado.getDataFinalizacao()
        );
    }
}