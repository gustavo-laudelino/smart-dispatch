package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.CheckInRequest;
import br.com.smartdispatch.dto.OrdemServicoRequest;
import br.com.smartdispatch.dto.OrdemServicoResponse;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoEventoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.repository.OrdemServicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ChamadoService chamadoService;
    private final TecnicoService tecnicoService;
    private final UnidadeService unidadeService;
    private final HistoricoChamadoService historicoChamadoService;

    public OrdemServicoService(
            OrdemServicoRepository ordemServicoRepository,
            ChamadoService chamadoService,
            TecnicoService tecnicoService,
            UnidadeService unidadeService,
            HistoricoChamadoService historicoChamadoService
    ) {
        this.ordemServicoRepository =
                ordemServicoRepository;

        this.chamadoService =
                chamadoService;

        this.tecnicoService =
                tecnicoService;

        this.unidadeService =
                unidadeService;

        this.historicoChamadoService =
                historicoChamadoService;
    }

    @Transactional
    public OrdemServicoResponse criar(
            Long contratoId,
            Long chamadoId,
            OrdemServicoRequest request
    ) {
        validarRequest(request);

        Chamado chamado =
                chamadoService.buscarEntidadePorId(
                        contratoId,
                        chamadoId
                );

        validarChamadoPermiteNovaOrdem(
                chamado
        );

        String numeroOrdemServico =
                request
                        .getNumeroOrdemServico()
                        .trim();

        boolean numeroJaCadastrado =
                ordemServicoRepository
                        .existsByNumeroOrdemServico(
                                numeroOrdemServico
                        );

        if (numeroJaCadastrado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma ordem de serviço com este número"
            );
        }

        Tecnico tecnico = null;

        if (request.getTecnicoId() != null) {
            tecnico =
                    tecnicoService.buscarEntidadePorId(
                            contratoId,
                            request.getTecnicoId()
                    );
        }

        Unidade unidadeAtendimento =
                definirUnidadeAtendimento(
                        contratoId,
                        chamado,
                        request.getUnidadeAtendimentoId()
                );

        OrdemServico ordemServico =
                new OrdemServico();

        ordemServico.setNumeroOrdemServico(
                numeroOrdemServico
        );

        ordemServico.setChamado(
                chamado
        );

        ordemServico.setTecnico(
                tecnico
        );

        ordemServico.setUnidadeAtendimento(
                unidadeAtendimento
        );

        if (tecnico != null) {
            ordemServico.setDataAtribuicaoTecnico(
                    LocalDateTime.now()
            );
        }

        OrdemServico ordemServicoSalva =
                ordemServicoRepository.saveAndFlush(
                        ordemServico
                );

        historicoChamadoService.registrar(
                chamado,
                ordemServicoSalva,
                TipoEventoChamado.ORDEM_SERVICO_CRIADA,
                "Ordem de serviço "
                        + numeroOrdemServico
                        + " criada para a unidade "
                        + unidadeAtendimento.getNome()
                        + "."
        );

        if (tecnico != null) {
            historicoChamadoService.registrar(
                    chamado,
                    ordemServicoSalva,
                    TipoEventoChamado.TECNICO_ATRIBUIDO,
                    "Técnico "
                            + obterNomeTecnico(tecnico)
                            + " atribuído à OS "
                            + numeroOrdemServico
                            + "."
            );
        }

        recalcularStatusOperacionalDoChamado(
                chamado
        );

        return converterParaResponse(
                ordemServicoSalva
        );
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listarPorChamado(
            Long contratoId,
            Long chamadoId
    ) {
        chamadoService.buscarEntidadePorId(
                contratoId,
                chamadoId
        );

        return ordemServicoRepository
                .findByChamadoId(
                        chamadoId
                )
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    @Transactional
    public OrdemServicoResponse atualizar(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId,
            OrdemServicoRequest request
    ) {
        validarRequest(request);

        OrdemServico ordemServico =
                buscarEntidadePorId(
                        contratoId,
                        chamadoId,
                        ordemServicoId
                );

        String numeroAnterior =
                ordemServico
                        .getNumeroOrdemServico();

        Tecnico tecnicoAnterior =
                ordemServico.getTecnico();

        Long tecnicoAnteriorId =
                tecnicoAnterior == null
                        ? null
                        : tecnicoAnterior.getId();

        Unidade unidadeAnterior =
                ordemServico
                        .getUnidadeAtendimento();

        String numeroOrdemServico =
                request
                        .getNumeroOrdemServico()
                        .trim();

        boolean numeroPertenceAOutraOrdem =
                ordemServicoRepository
                        .existsByNumeroOrdemServicoAndIdNot(
                                numeroOrdemServico,
                                ordemServicoId
                        );

        if (numeroPertenceAOutraOrdem) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe outra ordem de serviço com este número"
            );
        }

        Tecnico tecnico;
        Unidade unidadeAtendimento;

        if (ordemServico.getDataCheckIn() == null) {

            if (request.getTecnicoId() == null) {
                tecnico = null;
            } else {
                tecnico =
                        tecnicoService.buscarEntidadePorId(
                                contratoId,
                                request.getTecnicoId()
                        );
            }

            if (
                    request.getUnidadeAtendimentoId()
                            == null
            ) {
                unidadeAtendimento =
                        ordemServico
                                .getUnidadeAtendimento();
            } else {
                unidadeAtendimento =
                        unidadeService.buscarPorId(
                                contratoId,
                                request.getUnidadeAtendimentoId()
                        );
            }

        } else {

            if (
                    request.getTecnicoId() == null
                            || ordemServico.getTecnico() == null
                            || !ordemServico
                            .getTecnico()
                            .getId()
                            .equals(
                                    request.getTecnicoId()
                            )
            ) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível alterar o técnico após o check-in"
                );
            }

            if (
                    request.getUnidadeAtendimentoId() != null
                            && !ordemServico
                            .getUnidadeAtendimento()
                            .getId()
                            .equals(
                                    request
                                            .getUnidadeAtendimentoId()
                            )
            ) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível alterar a unidade após o check-in"
                );
            }

            tecnico =
                    ordemServico.getTecnico();

            unidadeAtendimento =
                    ordemServico
                            .getUnidadeAtendimento();
        }

        ordemServico.setNumeroOrdemServico(
                numeroOrdemServico
        );

        ordemServico.setTecnico(
                tecnico
        );

        ordemServico.setUnidadeAtendimento(
                unidadeAtendimento
        );

        atualizarDataAtribuicaoTecnico(
                ordemServico,
                tecnicoAnteriorId,
                tecnico
        );

        OrdemServico ordemServicoAtualizada =
                ordemServicoRepository.saveAndFlush(
                        ordemServico
                );

        registrarEventosAtualizacao(
                ordemServicoAtualizada,
                numeroAnterior,
                tecnicoAnterior,
                unidadeAnterior
        );

        recalcularStatusOperacionalDoChamado(
                ordemServicoAtualizada.getChamado()
        );

        return converterParaResponse(
                ordemServicoAtualizada
        );
    }

    @Transactional
    public OrdemServicoResponse realizarCheckIn(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId,
            CheckInRequest request
    ) {
        OrdemServico ordemServico =
                buscarEntidadePorId(
                        contratoId,
                        chamadoId,
                        ordemServicoId
                );

        if (ordemServico.getDataCheckOut() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta ordem de serviço já foi encerrada"
            );
        }

        if (ordemServico.getDataCheckIn() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta ordem de serviço já possui um check-in ativo"
            );
        }

        Tecnico tecnico =
                ordemServico.getTecnico();

        if (tecnico == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível efetuar check-in sem um técnico atribuído"
            );
        }

        if (!tecnico.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível efetuar check-in para um técnico inativo"
            );
        }

        OrdemServico ordemAtivaAnterior =
                ordemServicoRepository
                        .findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                                tecnico.getId()
                        )
                        .orElse(null);

        LocalDateTime momentoCheckIn =
                LocalDateTime.now();

        if (ordemAtivaAnterior != null) {

            boolean encerrarAnterior =
                    request != null
                            && Boolean.TRUE.equals(
                            request
                                    .getEncerrarCheckInAnterior()
                    );

            if (!encerrarAnterior) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível efetuar o check-in, pois você possui "
                                + "um check-in ativo na OS "
                                + ordemAtivaAnterior
                                .getNumeroOrdemServico()
                                + ", na unidade "
                                + ordemAtivaAnterior
                                .getUnidadeAtendimento()
                                .getNome()
                                + ". Deseja encerrar esse atendimento e iniciar a OS "
                                + ordemServico
                                .getNumeroOrdemServico()
                                + "?"
                );
            }

            concluirAtendimento(
                    ordemAtivaAnterior,
                    momentoCheckIn,
                    true
            );
        }

        ordemServico.setDataCheckIn(
                momentoCheckIn
        );

        ordemServico
                .getChamado()
                .setStatus(
                        StatusChamado.EM_ATENDIMENTO
                );

        OrdemServico ordemServicoAtualizada =
                ordemServicoRepository.saveAndFlush(
                        ordemServico
                );

        historicoChamadoService.registrar(
                ordemServicoAtualizada.getChamado(),
                ordemServicoAtualizada,
                TipoEventoChamado.ATENDIMENTO_INICIADO,
                "Atendimento iniciado na OS "
                        + ordemServicoAtualizada
                        .getNumeroOrdemServico()
                        + " pelo técnico "
                        + obterNomeTecnico(tecnico)
                        + "."
        );

        return converterParaResponse(
                ordemServicoAtualizada
        );
    }

    @Transactional
    public OrdemServicoResponse realizarCheckOut(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId
    ) {
        OrdemServico ordemServico =
                buscarEntidadePorId(
                        contratoId,
                        chamadoId,
                        ordemServicoId
                );

        if (ordemServico.getDataCheckIn() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível realizar check-out sem um check-in"
            );
        }

        if (ordemServico.getDataCheckOut() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta ordem de serviço já possui check-out"
            );
        }

        concluirAtendimento(
                ordemServico,
                LocalDateTime.now(),
                false
        );

        return converterParaResponse(
                ordemServico
        );
    }

    @Transactional(readOnly = true)
    public OrdemServico buscarEntidadePorId(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId
    ) {
        return ordemServicoRepository
                .findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId,
                        chamadoId,
                        contratoId
                )
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Ordem de serviço não encontrada neste chamado"
                        )
                );
    }

    private Unidade definirUnidadeAtendimento(
            Long contratoId,
            Chamado chamado,
            Long unidadeAtendimentoId
    ) {
        if (unidadeAtendimentoId == null) {
            return chamado.getUnidade();
        }

        return unidadeService.buscarPorId(
                contratoId,
                unidadeAtendimentoId
        );
    }

    private void validarRequest(
            OrdemServicoRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os dados da ordem de serviço devem ser informados"
            );
        }

        if (
                request.getNumeroOrdemServico() == null
                        || request
                        .getNumeroOrdemServico()
                        .isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O número da ordem de serviço deve ser informado"
            );
        }
    }

    private void validarChamadoPermiteNovaOrdem(
            Chamado chamado
    ) {
        if (
                chamado.getStatus()
                        == StatusChamado.FINALIZADO
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível criar uma ordem de serviço para um chamado finalizado"
            );
        }

        if (
                chamado.getStatus()
                        == StatusChamado.CANCELADO
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível criar uma ordem de serviço para um chamado cancelado"
            );
        }
    }

    private void atualizarDataAtribuicaoTecnico(
            OrdemServico ordemServico,
            Long tecnicoAnteriorId,
            Tecnico novoTecnico
    ) {
        if (novoTecnico == null) {
            ordemServico.setDataAtribuicaoTecnico(
                    null
            );
            return;
        }

        boolean tecnicoFoiAlterado =
                tecnicoAnteriorId == null
                        || !tecnicoAnteriorId.equals(
                        novoTecnico.getId()
                );

        if (tecnicoFoiAlterado) {
            ordemServico.setDataAtribuicaoTecnico(
                    LocalDateTime.now()
            );
        }
    }

    private void registrarEventosAtualizacao(
            OrdemServico ordemServico,
            String numeroAnterior,
            Tecnico tecnicoAnterior,
            Unidade unidadeAnterior
    ) {
        Chamado chamado =
                ordemServico.getChamado();

        String numeroAtual =
                ordemServico
                        .getNumeroOrdemServico();

        Tecnico tecnicoAtual =
                ordemServico.getTecnico();

        Unidade unidadeAtual =
                ordemServico
                        .getUnidadeAtendimento();

        if (
                !Objects.equals(
                        numeroAnterior,
                        numeroAtual
                )
        ) {
            historicoChamadoService.registrar(
                    chamado,
                    ordemServico,
                    TipoEventoChamado.ORDEM_SERVICO_ALTERADA,
                    "Número da ordem de serviço alterado de "
                            + numeroAnterior
                            + " para "
                            + numeroAtual
                            + "."
            );
        }

        registrarEventoTecnico(
                chamado,
                ordemServico,
                tecnicoAnterior,
                tecnicoAtual
        );

        if (
                unidadeAnterior != null
                        && unidadeAtual != null
                        && !Objects.equals(
                        unidadeAnterior.getId(),
                        unidadeAtual.getId()
                )
        ) {
            historicoChamadoService.registrar(
                    chamado,
                    ordemServico,
                    TipoEventoChamado.UNIDADE_ORDEM_ALTERADA,
                    "Unidade da OS "
                            + numeroAtual
                            + " alterada de "
                            + unidadeAnterior.getNome()
                            + " para "
                            + unidadeAtual.getNome()
                            + "."
            );
        }
    }

    private void registrarEventoTecnico(
            Chamado chamado,
            OrdemServico ordemServico,
            Tecnico tecnicoAnterior,
            Tecnico tecnicoAtual
    ) {
        Long tecnicoAnteriorId =
                tecnicoAnterior == null
                        ? null
                        : tecnicoAnterior.getId();

        Long tecnicoAtualId =
                tecnicoAtual == null
                        ? null
                        : tecnicoAtual.getId();

        if (
                Objects.equals(
                        tecnicoAnteriorId,
                        tecnicoAtualId
                )
        ) {
            return;
        }

        String numeroOrdemServico =
                ordemServico
                        .getNumeroOrdemServico();

        if (
                tecnicoAnterior == null
                        && tecnicoAtual != null
        ) {
            historicoChamadoService.registrar(
                    chamado,
                    ordemServico,
                    TipoEventoChamado.TECNICO_ATRIBUIDO,
                    "Técnico "
                            + obterNomeTecnico(tecnicoAtual)
                            + " atribuído à OS "
                            + numeroOrdemServico
                            + "."
            );

            return;
        }

        if (
                tecnicoAnterior != null
                        && tecnicoAtual == null
        ) {
            historicoChamadoService.registrar(
                    chamado,
                    ordemServico,
                    TipoEventoChamado.TECNICO_REMOVIDO,
                    "Técnico "
                            + obterNomeTecnico(tecnicoAnterior)
                            + " removido da OS "
                            + numeroOrdemServico
                            + "."
            );

            return;
        }

        historicoChamadoService.registrar(
                chamado,
                ordemServico,
                TipoEventoChamado.TECNICO_ALTERADO,
                "Técnico da OS "
                        + numeroOrdemServico
                        + " alterado de "
                        + obterNomeTecnico(tecnicoAnterior)
                        + " para "
                        + obterNomeTecnico(tecnicoAtual)
                        + "."
        );
    }

    private void concluirAtendimento(
            OrdemServico ordemServico,
            LocalDateTime momentoCheckOut,
            boolean automatico
    ) {
        ordemServico.setDataCheckOut(
                momentoCheckOut
        );

        ordemServicoRepository.saveAndFlush(
                ordemServico
        );

        recalcularStatusOperacionalDoChamado(
                ordemServico.getChamado()
        );

        TipoEventoChamado tipoEvento =
                automatico
                        ? TipoEventoChamado
                          .ATENDIMENTO_FINALIZADO_AUTOMATICAMENTE
                        : TipoEventoChamado
                          .ATENDIMENTO_FINALIZADO;

        String descricao =
                automatico
                        ? "Atendimento da OS "
                          + ordemServico.getNumeroOrdemServico()
                          + " finalizado automaticamente para permitir o início de outra ordem de serviço."
                        : "Atendimento da OS "
                          + ordemServico.getNumeroOrdemServico()
                          + " finalizado.";

        historicoChamadoService.registrar(
                ordemServico.getChamado(),
                ordemServico,
                tipoEvento,
                descricao
        );
    }

    private String obterNomeTecnico(
            Tecnico tecnico
    ) {
        if (
                tecnico == null
                        || tecnico.getUsuario() == null
                        || tecnico
                        .getUsuario()
                        .getNome() == null
        ) {
            return "não informado";
        }

        return tecnico
                .getUsuario()
                .getNome();
    }

    private void recalcularStatusOperacionalDoChamado(
            Chamado chamado
    ) {
        boolean existeAtendimentoAtivo =
                ordemServicoRepository
                        .existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                                chamado.getId()
                        );

        if (existeAtendimentoAtivo) {
            chamado.setStatus(
                    StatusChamado.EM_ATENDIMENTO
            );
            return;
        }

        boolean existeOrdemAtribuidaNaoIniciada =
                ordemServicoRepository
                        .existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                                chamado.getId()
                        );

        if (existeOrdemAtribuidaNaoIniciada) {
            chamado.setStatus(
                    StatusChamado.ATRIBUIDO
            );
            return;
        }

        boolean existeOrdemSemTecnico =
                ordemServicoRepository
                        .existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                                chamado.getId()
                        );

        if (existeOrdemSemTecnico) {
            chamado.setStatus(
                    StatusChamado.ABERTO
            );
            return;
        }

        chamado.setStatus(
                StatusChamado.AGUARDANDO_ANALISE
        );
    }

    private OrdemServicoResponse converterParaResponse(
            OrdemServico ordemServico
    ) {
        Chamado chamado =
                ordemServico.getChamado();

        Tecnico tecnico =
                ordemServico.getTecnico();

        Unidade unidadeAtendimento =
                ordemServico
                        .getUnidadeAtendimento();

        Long tecnicoId = null;
        String tecnicoNome = null;

        if (tecnico != null) {
            tecnicoId =
                    tecnico.getId();

            tecnicoNome =
                    obterNomeTecnico(
                            tecnico
                    );
        }

        return new OrdemServicoResponse(
                ordemServico.getId(),
                ordemServico.getNumeroOrdemServico(),
                chamado.getId(),
                chamado.getNumeroChamado(),
                tecnicoId,
                tecnicoNome,
                ordemServico.getDataAtribuicaoTecnico(),
                unidadeAtendimento.getId(),
                unidadeAtendimento.getNome(),
                ordemServico.getDataCheckIn(),
                ordemServico.getDataCheckOut()
        );
    }
}