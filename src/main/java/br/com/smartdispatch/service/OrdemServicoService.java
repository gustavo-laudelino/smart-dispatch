package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.CheckInRequest;
import br.com.smartdispatch.dto.OrdemServicoRequest;
import br.com.smartdispatch.dto.OrdemServicoResponse;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoEventoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.repository.OrdemServicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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
    private final NumeracaoService numeracaoService;

    public OrdemServicoService(
            OrdemServicoRepository ordemServicoRepository,
            ChamadoService chamadoService,
            TecnicoService tecnicoService,
            UnidadeService unidadeService,
            HistoricoChamadoService historicoChamadoService,
            NumeracaoService numeracaoService
    ) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.chamadoService = chamadoService;
        this.tecnicoService = tecnicoService;
        this.unidadeService = unidadeService;
        this.historicoChamadoService = historicoChamadoService;
        this.numeracaoService = numeracaoService;
    }

    // ================= Criação =================

    @Transactional
    public OrdemServicoResponse criar(
            Long contratoId,
            OrdemServicoRequest request
    ) {
        validarRequest(request);

        Chamado chamado = null;

        if (request.getChamadoId() != null) {
            chamado = chamadoService.buscarEntidadePorId(
                    contratoId,
                    request.getChamadoId()
            );

            validarChamadoPermiteNovaOrdem(chamado);
        }

        return executarCriacao(contratoId, chamado, request);
    }

    /** Rota antiga, aninhada em Chamado — converge para a mesma regra. */
    @Transactional
    public OrdemServicoResponse criar(
            Long contratoId,
            Long chamadoId,
            OrdemServicoRequest request
    ) {
        validarRequest(request);

        request.setChamadoId(chamadoId);

        return criar(contratoId, request);
    }

    private OrdemServicoResponse executarCriacao(
            Long contratoId,
            Chamado chamado,
            OrdemServicoRequest request
    ) {
        Tecnico tecnico = null;

        if (request.getTecnicoId() != null) {
            tecnico = tecnicoService.buscarEntidadePorId(
                    contratoId,
                    request.getTecnicoId()
            );
        }

        Unidade unidadeAtendimento = definirUnidadeAtendimento(
                contratoId,
                chamado,
                request.getUnidadeAtendimentoId()
        );

        OrdemServico ordemServico = new OrdemServico();

        ordemServico.setNumeroOrdemServico(
                numeracaoService.proximoNumeroOrdemServico()
        );

        ordemServico.setChamado(chamado);
        ordemServico.setTecnico(tecnico);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);

        ordemServico.setDescricao(
                request.getDescricao() != null
                        ? request.getDescricao()
                        : (chamado != null ? chamado.getDescricao() : null)
        );

        ordemServico.setNumeroPatrimonio(
                request.getNumeroPatrimonio() != null
                        ? request.getNumeroPatrimonio()
                        : (chamado != null ? chamado.getNumeroPatrimonio() : null)
        );

        ordemServico.setData(
                request.getData() != null
                        ? request.getData()
                        : LocalDate.now()
        );

        ordemServico.setHora(request.getHora());

        if (tecnico != null) {
            ordemServico.setDataAtribuicaoTecnico(LocalDateTime.now());
        }

        OrdemServico ordemServicoSalva =
                ordemServicoRepository.saveAndFlush(ordemServico);

        if (chamado != null) {
            historicoChamadoService.registrar(
                    chamado,
                    ordemServicoSalva,
                    TipoEventoChamado.ORDEM_SERVICO_CRIADA,
                    "Ordem de serviço "
                            + ordemServicoSalva.getNumeroOrdemServico()
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
                                + ordemServicoSalva.getNumeroOrdemServico()
                                + "."
                );
            }

            recalcularStatusOperacionalDoChamado(chamado);
        }

        return converterParaResponse(ordemServicoSalva);
    }

    // ================= Listagem =================

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listarPorChamado(
            Long contratoId,
            Long chamadoId
    ) {
        chamadoService.buscarEntidadePorId(contratoId, chamadoId);

        return ordemServicoRepository
                .findByChamadoId(chamadoId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listarPorContrato(
            Long contratoId,
            Long chamadoId,
            Long tecnicoId,
            LocalDate data,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        return ordemServicoRepository
                .buscarPorContratoComFiltros(
                        contratoId,
                        chamadoId,
                        tecnicoId,
                        data,
                        dataInicio,
                        dataFim
                )
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    // ================= Busca =================

    /** Rota antiga, aninhada em Chamado. */
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

    /** Rota canônica, por contrato — OS com ou sem Chamado. */
    @Transactional(readOnly = true)
    public OrdemServico buscarEntidadePorId(
            Long contratoId,
            Long ordemServicoId
    ) {
        return ordemServicoRepository
                .findByIdAndUnidadeAtendimentoContratoId(
                        ordemServicoId,
                        contratoId
                )
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Ordem de serviço não encontrada neste contrato"
                        )
                );
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponse buscarPorId(
            Long contratoId,
            Long ordemServicoId
    ) {
        return converterParaResponse(
                buscarEntidadePorId(contratoId, ordemServicoId)
        );
    }

    // ================= Atualização =================

    /** Rota antiga, aninhada em Chamado — converge para a mesma regra. */
    @Transactional
    public OrdemServicoResponse atualizar(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId,
            OrdemServicoRequest request
    ) {
        buscarEntidadePorId(contratoId, chamadoId, ordemServicoId);

        return atualizar(contratoId, ordemServicoId, request);
    }

    @Transactional
    public OrdemServicoResponse atualizar(
            Long contratoId,
            Long ordemServicoId,
            OrdemServicoRequest request
    ) {
        validarRequest(request);

        OrdemServico ordemServico =
                buscarEntidadePorId(contratoId, ordemServicoId);

        Chamado chamadoAnterior = ordemServico.getChamado();

        Tecnico tecnicoAnterior = ordemServico.getTecnico();

        Long tecnicoAnteriorId =
                tecnicoAnterior == null ? null : tecnicoAnterior.getId();

        Unidade unidadeAnterior = ordemServico.getUnidadeAtendimento();

        Tecnico tecnico;
        Unidade unidadeAtendimento;
        Chamado novoChamado;

        if (ordemServico.getDataCheckIn() == null) {

            if (request.getTecnicoId() == null) {
                tecnico = null;
            } else {
                tecnico = tecnicoService.buscarEntidadePorId(
                        contratoId,
                        request.getTecnicoId()
                );
            }

            unidadeAtendimento = request.getUnidadeAtendimentoId() == null
                    ? ordemServico.getUnidadeAtendimento()
                    : unidadeService.buscarPorId(
                    contratoId,
                    request.getUnidadeAtendimentoId()
            );

            novoChamado = request.getChamadoId() == null
                    ? null
                    : chamadoService.buscarEntidadePorId(
                    contratoId,
                    request.getChamadoId()
            );

        } else {

            if (request.getTecnicoId() == null
                    || ordemServico.getTecnico() == null
                    || !ordemServico.getTecnico().getId()
                    .equals(request.getTecnicoId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível alterar o técnico após o check-in"
                );
            }

            if (request.getUnidadeAtendimentoId() != null
                    && !ordemServico.getUnidadeAtendimento().getId()
                    .equals(request.getUnidadeAtendimentoId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível alterar a unidade após o check-in"
                );
            }

            Long chamadoAnteriorId =
                    chamadoAnterior == null ? null : chamadoAnterior.getId();

            if (!Objects.equals(chamadoAnteriorId, request.getChamadoId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível alterar o chamado vinculado após o check-in"
                );
            }

            tecnico = ordemServico.getTecnico();
            unidadeAtendimento = ordemServico.getUnidadeAtendimento();
            novoChamado = chamadoAnterior;
        }

        ordemServico.setTecnico(tecnico);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);
        ordemServico.setChamado(novoChamado);
        ordemServico.setDescricao(request.getDescricao());
        ordemServico.setNumeroPatrimonio(request.getNumeroPatrimonio());

        ordemServico.setData(
                request.getData() != null
                        ? request.getData()
                        : ordemServico.getData()
        );

        ordemServico.setHora(request.getHora());

        atualizarDataAtribuicaoTecnico(ordemServico, tecnicoAnteriorId, tecnico);

        OrdemServico ordemServicoAtualizada =
                ordemServicoRepository.saveAndFlush(ordemServico);

        if (novoChamado != null) {
            registrarEventosAtualizacao(
                    ordemServicoAtualizada,
                    tecnicoAnterior,
                    unidadeAnterior
            );
        }

        recalcularStatusOperacionalDoChamado(chamadoAnterior);
        recalcularStatusOperacionalDoChamado(novoChamado);

        return converterParaResponse(ordemServicoAtualizada);
    }

    // ================= Check-in / Check-out =================

    /** Rota antiga, aninhada em Chamado — converge para a mesma regra. */
    @Transactional
    public OrdemServicoResponse realizarCheckIn(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId,
            CheckInRequest request
    ) {
        buscarEntidadePorId(contratoId, chamadoId, ordemServicoId);

        return realizarCheckIn(contratoId, ordemServicoId, request);
    }

    @Transactional
    public OrdemServicoResponse realizarCheckIn(
            Long contratoId,
            Long ordemServicoId,
            CheckInRequest request
    ) {
        OrdemServico ordemServico =
                buscarEntidadePorId(contratoId, ordemServicoId);

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

        Tecnico tecnico = ordemServico.getTecnico();

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

        OrdemServico ordemAtivaAnterior = ordemServicoRepository
                .findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        tecnico.getId()
                )
                .orElse(null);

        LocalDateTime momentoCheckIn = LocalDateTime.now();

        if (ordemAtivaAnterior != null) {

            boolean encerrarAnterior = request != null
                    && Boolean.TRUE.equals(request.getEncerrarCheckInAnterior());

            if (!encerrarAnterior) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível efetuar o check-in, pois você possui "
                                + "um check-in ativo na OS "
                                + ordemAtivaAnterior.getNumeroOrdemServico()
                                + ", na unidade "
                                + ordemAtivaAnterior.getUnidadeAtendimento().getNome()
                                + ". Deseja encerrar esse atendimento e iniciar a OS "
                                + ordemServico.getNumeroOrdemServico()
                                + "?"
                );
            }

            concluirAtendimento(ordemAtivaAnterior, momentoCheckIn, true);
        }

        ordemServico.setDataCheckIn(momentoCheckIn);

        if (ordemServico.getChamado() != null) {
            ordemServico.getChamado().setStatus(StatusChamado.EM_ATENDIMENTO);
        }

        OrdemServico ordemServicoAtualizada =
                ordemServicoRepository.saveAndFlush(ordemServico);

        if (ordemServicoAtualizada.getChamado() != null) {
            historicoChamadoService.registrar(
                    ordemServicoAtualizada.getChamado(),
                    ordemServicoAtualizada,
                    TipoEventoChamado.ATENDIMENTO_INICIADO,
                    "Atendimento iniciado na OS "
                            + ordemServicoAtualizada.getNumeroOrdemServico()
                            + " pelo técnico "
                            + obterNomeTecnico(tecnico)
                            + "."
            );
        }

        return converterParaResponse(ordemServicoAtualizada);
    }

    /** Rota antiga, aninhada em Chamado — converge para a mesma regra. */
    @Transactional
    public OrdemServicoResponse realizarCheckOut(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId
    ) {
        buscarEntidadePorId(contratoId, chamadoId, ordemServicoId);

        return realizarCheckOut(contratoId, ordemServicoId);
    }

    @Transactional
    public OrdemServicoResponse realizarCheckOut(
            Long contratoId,
            Long ordemServicoId
    ) {
        OrdemServico ordemServico =
                buscarEntidadePorId(contratoId, ordemServicoId);

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

        concluirAtendimento(ordemServico, LocalDateTime.now(), false);

        return converterParaResponse(ordemServico);
    }

    // ================= Auxiliares =================

    private Unidade definirUnidadeAtendimento(
            Long contratoId,
            Chamado chamado,
            Long unidadeAtendimentoId
    ) {
        if (unidadeAtendimentoId != null) {
            return unidadeService.buscarPorId(contratoId, unidadeAtendimentoId);
        }

        if (chamado != null) {
            return chamado.getUnidade();
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A unidade de atendimento deve ser informada quando não há chamado vinculado"
        );
    }

    private void validarRequest(OrdemServicoRequest request) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os dados da ordem de serviço devem ser informados"
            );
        }
    }

    private void validarChamadoPermiteNovaOrdem(Chamado chamado) {
        if (chamado.getStatus() == StatusChamado.FINALIZADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível criar uma ordem de serviço para um chamado finalizado"
            );
        }

        if (chamado.getStatus() == StatusChamado.CANCELADO) {
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
            ordemServico.setDataAtribuicaoTecnico(null);
            return;
        }

        boolean tecnicoFoiAlterado = tecnicoAnteriorId == null
                || !tecnicoAnteriorId.equals(novoTecnico.getId());

        if (tecnicoFoiAlterado) {
            ordemServico.setDataAtribuicaoTecnico(LocalDateTime.now());
        }
    }

    private void registrarEventosAtualizacao(
            OrdemServico ordemServico,
            Tecnico tecnicoAnterior,
            Unidade unidadeAnterior
    ) {
        Chamado chamado = ordemServico.getChamado();

        Tecnico tecnicoAtual = ordemServico.getTecnico();
        Unidade unidadeAtual = ordemServico.getUnidadeAtendimento();

        registrarEventoTecnico(chamado, ordemServico, tecnicoAnterior, tecnicoAtual);

        if (unidadeAnterior != null
                && unidadeAtual != null
                && !Objects.equals(unidadeAnterior.getId(), unidadeAtual.getId())) {
            historicoChamadoService.registrar(
                    chamado,
                    ordemServico,
                    TipoEventoChamado.UNIDADE_ORDEM_ALTERADA,
                    "Unidade da OS "
                            + ordemServico.getNumeroOrdemServico()
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
                tecnicoAnterior == null ? null : tecnicoAnterior.getId();

        Long tecnicoAtualId =
                tecnicoAtual == null ? null : tecnicoAtual.getId();

        if (Objects.equals(tecnicoAnteriorId, tecnicoAtualId)) {
            return;
        }

        String numeroOrdemServico =
                String.valueOf(ordemServico.getNumeroOrdemServico());

        if (tecnicoAnterior == null && tecnicoAtual != null) {
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

        if (tecnicoAnterior != null && tecnicoAtual == null) {
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
        ordemServico.setDataCheckOut(momentoCheckOut);

        ordemServicoRepository.saveAndFlush(ordemServico);

        Chamado chamado = ordemServico.getChamado();

        recalcularStatusOperacionalDoChamado(chamado);

        if (chamado == null) {
            return;
        }

        TipoEventoChamado tipoEvento = automatico
                ? TipoEventoChamado.ATENDIMENTO_FINALIZADO_AUTOMATICAMENTE
                : TipoEventoChamado.ATENDIMENTO_FINALIZADO;

        String descricao = automatico
                ? "Atendimento da OS "
                + ordemServico.getNumeroOrdemServico()
                + " finalizado automaticamente para permitir o início de outra ordem de serviço."
                : "Atendimento da OS "
                + ordemServico.getNumeroOrdemServico()
                + " finalizado.";

        historicoChamadoService.registrar(chamado, ordemServico, tipoEvento, descricao);
    }

    private String obterNomeTecnico(Tecnico tecnico) {
        if (tecnico == null
                || tecnico.getUsuario() == null
                || tecnico.getUsuario().getNome() == null) {
            return "não informado";
        }

        return tecnico.getUsuario().getNome();
    }

    private void recalcularStatusOperacionalDoChamado(Chamado chamado) {
        if (chamado == null) {
            return;
        }

        boolean existeAtendimentoAtivo = ordemServicoRepository
                .existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamado.getId()
                );

        if (existeAtendimentoAtivo) {
            chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
            return;
        }

        boolean existeOrdemAtribuidaNaoIniciada = ordemServicoRepository
                .existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamado.getId()
                );

        if (existeOrdemAtribuidaNaoIniciada) {
            chamado.setStatus(StatusChamado.ATRIBUIDO);
            return;
        }

        boolean existeOrdemSemTecnico = ordemServicoRepository
                .existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamado.getId()
                );

        if (existeOrdemSemTecnico) {
            chamado.setStatus(StatusChamado.ABERTO);
            return;
        }

        chamado.setStatus(StatusChamado.AGUARDANDO_ANALISE);
    }

    private OrdemServicoResponse converterParaResponse(
            OrdemServico ordemServico
    ) {
        Chamado chamado = ordemServico.getChamado();
        Tecnico tecnico = ordemServico.getTecnico();
        Unidade unidadeAtendimento = ordemServico.getUnidadeAtendimento();
        Contrato contrato = unidadeAtendimento.getContrato();

        Long tecnicoId = null;
        String tecnicoNome = null;

        if (tecnico != null) {
            tecnicoId = tecnico.getId();
            tecnicoNome = obterNomeTecnico(tecnico);
        }

        Long chamadoId = chamado == null ? null : chamado.getId();
        String numeroChamado = chamado == null ? null : chamado.getNumeroChamado();

        return new OrdemServicoResponse(
                ordemServico.getId(),
                ordemServico.getNumeroOrdemServico(),
                contrato.getId(),
                contrato.getCidade(),
                chamadoId,
                numeroChamado,
                ordemServico.getDescricao(),
                ordemServico.getNumeroPatrimonio(),
                tecnicoId,
                tecnicoNome,
                ordemServico.getData(),
                ordemServico.getHora(),
                ordemServico.getDataAtribuicaoTecnico(),
                unidadeAtendimento.getId(),
                unidadeAtendimento.getNome(),
                ordemServico.getDataCheckIn(),
                ordemServico.getDataCheckOut()
        );
    }
}
