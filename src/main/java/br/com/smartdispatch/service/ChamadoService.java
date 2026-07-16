package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.ChamadoRequest;
import br.com.smartdispatch.dto.ChamadoResponse;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.repository.ChamadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.com.smartdispatch.dto.StatusChamadoRequest;
import br.com.smartdispatch.enums.StatusChamado;

import java.time.LocalDateTime;

import java.util.Comparator;
import java.util.List;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UnidadeService unidadeService;
    private final ContratoService contratoService;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            UnidadeService unidadeService,
            ContratoService contratoService
    ) {
        this.chamadoRepository = chamadoRepository;
        this.unidadeService = unidadeService;
        this.contratoService = contratoService;
    }

    @Transactional
    public ChamadoResponse criar(
            Long contratoId,
            ChamadoRequest request
    ) {
        Unidade unidade = unidadeService.buscarPorId(
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

        Chamado chamado = new Chamado();

        aplicarDados(
                chamado,
                request,
                unidade
        );

        Chamado chamadoSalvo =
                chamadoRepository.save(chamado);

        return converterParaResponse(chamadoSalvo);
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> listarPorContrato(
            Long contratoId
    ) {
        contratoService.buscarPorId(contratoId);

        return chamadoRepository
                .findByUnidadeContratoId(contratoId)
                .stream()
                .sorted(
                        Comparator
                                .comparing(Chamado::getDataAbertura)
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
            return listarPorContrato(contratoId);
        }

        return chamadoRepository
                .findAll()
                .stream()
                .sorted(
                        Comparator
                                .comparing(Chamado::getDataAbertura)
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
        Chamado chamado = buscarEntidadePorId(
                contratoId,
                chamadoId
        );

        return converterParaResponse(chamado);
    }

    @Transactional
    public ChamadoResponse atualizar(
            Long contratoId,
            Long chamadoId,
            ChamadoRequest request
    ) {
        Chamado chamado = buscarEntidadePorId(
                contratoId,
                chamadoId
        );

        Unidade unidade = unidadeService.buscarPorId(
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

        aplicarDados(
                chamado,
                request,
                unidade
        );

        Chamado chamadoAtualizado =
                chamadoRepository.save(chamado);

        return converterParaResponse(chamadoAtualizado);
    }

    @Transactional
    public ChamadoResponse atualizarStatus(
            Long contratoId,
            Long chamadoId,
            StatusChamadoRequest request
    ) {
        if (
                request == null ||
                        request.getStatus() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O status do chamado deve ser informado"
            );
        }

        StatusChamado novoStatus =
                request.getStatus();

        if (
                novoStatus == StatusChamado.ABERTO ||
                        novoStatus == StatusChamado.ATRIBUIDO ||
                        novoStatus == StatusChamado.EM_ATENDIMENTO
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Este status é controlado automaticamente pelo sistema"
            );
        }

        Chamado chamado = buscarEntidadePorId(
                contratoId,
                chamadoId
        );

        chamado.setStatus(novoStatus);

        if (novoStatus == StatusChamado.FINALIZADO) {
            chamado.setDataFinalizacao(
                    LocalDateTime.now()
            );
        } else {
            chamado.setDataFinalizacao(null);
        }

        Chamado chamadoAtualizado =
                chamadoRepository.save(chamado);

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Chamado não encontrado neste contrato"
                ));
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

        chamado.setUnidade(unidade);

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

    private ChamadoResponse converterParaResponse(
            Chamado chamado
    ) {
        Unidade unidade = chamado.getUnidade();
        Contrato contrato = unidade.getContrato();

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