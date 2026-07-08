package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.OrdemServicoRequest;
import br.com.smartdispatch.dto.OrdemServicoResponse;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.repository.OrdemServicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.com.smartdispatch.dto.CheckInRequest;
import java.time.LocalDateTime;

import java.util.List;

@Service
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ChamadoService chamadoService;
    private final TecnicoService tecnicoService;
    private final UnidadeService unidadeService;

    public OrdemServicoService(
            OrdemServicoRepository ordemServicoRepository,
            ChamadoService chamadoService,
            TecnicoService tecnicoService,
            UnidadeService unidadeService
    ) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.chamadoService = chamadoService;
        this.tecnicoService = tecnicoService;
        this.unidadeService = unidadeService;
    }

    @Transactional
    public OrdemServicoResponse criar(
            Long contratoId,
            Long chamadoId,
            OrdemServicoRequest request
    ) {
        validarRequest(request);

        Chamado chamado = chamadoService.buscarEntidadePorId(
                contratoId,
                chamadoId
        );

        validarChamadoPermiteNovaOrdem(chamado);

        String numeroOrdemServico =
                request.getNumeroOrdemServico().trim();

        boolean numeroJaCadastrado =
                ordemServicoRepository.existsByNumeroOrdemServico(
                        numeroOrdemServico
                );

        if (numeroJaCadastrado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma ordem de serviço com este número"
            );
        }

        Tecnico tecnico = tecnicoService.buscarEntidadePorId(
                contratoId,
                request.getTecnicoId()
        );

        Unidade unidadeAtendimento = definirUnidadeAtendimento(
                contratoId,
                chamado,
                request.getUnidadeAtendimentoId()
        );

        OrdemServico ordemServico = new OrdemServico();

        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        ordemServico.setChamado(chamado);
        ordemServico.setTecnico(tecnico);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);

        OrdemServico ordemServicoSalva =
                ordemServicoRepository.save(ordemServico);

        boolean existeAtendimentoAtivo =
                ordemServicoRepository
                        .existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                                chamadoId
                        );

        if (existeAtendimentoAtivo) {
            chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        } else {
            chamado.setStatus(StatusChamado.ATRIBUIDO);
        }

        return converterParaResponse(ordemServicoSalva);
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
                .findByChamadoId(chamadoId)
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

        OrdemServico ordemServico = buscarEntidadePorId(
                contratoId,
                chamadoId,
                ordemServicoId
        );

        String numeroOrdemServico =
                request.getNumeroOrdemServico().trim();

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

            tecnico = tecnicoService.buscarEntidadePorId(
                    contratoId,
                    request.getTecnicoId()
            );

            if (request.getUnidadeAtendimentoId() == null) {
                unidadeAtendimento =
                        ordemServico.getUnidadeAtendimento();
            } else {
                unidadeAtendimento = unidadeService.buscarPorId(
                        contratoId,
                        request.getUnidadeAtendimentoId()
                );
            }

        } else {

            if (!ordemServico
                    .getTecnico()
                    .getId()
                    .equals(request.getTecnicoId())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível alterar o técnico após o check-in"
                );
            }

            if (request.getUnidadeAtendimentoId() != null
                    && !ordemServico
                    .getUnidadeAtendimento()
                    .getId()
                    .equals(request.getUnidadeAtendimentoId())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível alterar a unidade após o check-in"
                );
            }

            tecnico = ordemServico.getTecnico();
            unidadeAtendimento =
                    ordemServico.getUnidadeAtendimento();
        }

        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        ordemServico.setTecnico(tecnico);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);


        OrdemServico ordemServicoAtualizada =
                ordemServicoRepository.save(ordemServico);

        return converterParaResponse(ordemServicoAtualizada);
    }

    @Transactional
    public OrdemServicoResponse realizarCheckIn(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId,
            CheckInRequest request
    ) {
        OrdemServico ordemServico = buscarEntidadePorId(
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

        Tecnico tecnico = ordemServico.getTecnico();

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

        LocalDateTime momentoCheckIn = LocalDateTime.now();

        if (ordemAtivaAnterior != null) {

            boolean encerrarAnterior =
                    request != null
                            && Boolean.TRUE.equals(
                            request.getEncerrarCheckInAnterior()
                    );

            if (!encerrarAnterior) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Não é possível efetuar o check-in, pois você possui "
                                + "um check-in ativo na OS "
                                + ordemAtivaAnterior.getNumeroOrdemServico()
                                + ", na unidade "
                                + ordemAtivaAnterior
                                .getUnidadeAtendimento()
                                .getNome()
                                + ". Deseja encerrar esse atendimento e iniciar a OS "
                                + ordemServico.getNumeroOrdemServico()
                                + "?"
                );
            }

            realizarCheckOutAutomatico(
                    ordemAtivaAnterior,
                    momentoCheckIn
            );
        }

        ordemServico.setDataCheckIn(momentoCheckIn);

        ordemServico
                .getChamado()
                .setStatus(StatusChamado.EM_ATENDIMENTO);

        OrdemServico ordemServicoAtualizada =
                ordemServicoRepository.save(ordemServico);

        return converterParaResponse(ordemServicoAtualizada);
    }

    @Transactional
    public OrdemServicoResponse realizarCheckOut(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId
    ) {
        OrdemServico ordemServico = buscarEntidadePorId(
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

        realizarCheckOutAutomatico(
                ordemServico,
                LocalDateTime.now()
        );

        return converterParaResponse(ordemServico);
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ordem de serviço não encontrada neste chamado"
                ));
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
        if (request.getNumeroOrdemServico() == null
                || request.getNumeroOrdemServico().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O número da ordem de serviço deve ser informado"
            );
        }

        if (request.getTecnicoId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O técnico deve ser informado"
            );
        }
    }

    private void validarChamadoPermiteNovaOrdem(
            Chamado chamado
    ) {
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

    private void realizarCheckOutAutomatico(
            OrdemServico ordemServicoAtiva,
            LocalDateTime momentoCheckOut
    ) {
        ordemServicoAtiva.setDataCheckOut(momentoCheckOut);

        ordemServicoRepository.saveAndFlush(
                ordemServicoAtiva
        );

        Chamado chamadoAnterior =
                ordemServicoAtiva.getChamado();

        boolean existeOutroAtendimentoAtivo =
                ordemServicoRepository
                        .existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                                chamadoAnterior.getId()
                        );

        if (existeOutroAtendimentoAtivo) {
            chamadoAnterior.setStatus(
                    StatusChamado.EM_ATENDIMENTO
            );
        } else {
            chamadoAnterior.setStatus(
                    StatusChamado.AGUARDANDO_ANALISE
            );
        }
    }

    private OrdemServicoResponse converterParaResponse(
            OrdemServico ordemServico
    ) {
        Chamado chamado = ordemServico.getChamado();
        Tecnico tecnico = ordemServico.getTecnico();
        Unidade unidadeAtendimento =
                ordemServico.getUnidadeAtendimento();

        return new OrdemServicoResponse(
                ordemServico.getId(),
                ordemServico.getNumeroOrdemServico(),
                chamado.getId(),
                chamado.getNumeroChamado(),
                tecnico.getId(),
                tecnico.getUsuario().getNome(),
                unidadeAtendimento.getId(),
                unidadeAtendimento.getNome(),
                ordemServico.getDataCheckIn(),
                ordemServico.getDataCheckOut()
        );
    }
}