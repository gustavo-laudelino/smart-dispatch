package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.HistoricoChamadoResponse;
import br.com.smartdispatch.enums.TipoEventoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.HistoricoChamado;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.repository.ChamadoRepository;
import br.com.smartdispatch.repository.HistoricoChamadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HistoricoChamadoService {

    private final HistoricoChamadoRepository historicoRepository;
    private final ChamadoRepository chamadoRepository;

    public HistoricoChamadoService(
            HistoricoChamadoRepository historicoRepository,
            ChamadoRepository chamadoRepository
    ) {
        this.historicoRepository =
                historicoRepository;

        this.chamadoRepository =
                chamadoRepository;
    }

    @Transactional
    public void registrar(
            Chamado chamado,
            OrdemServico ordemServico,
            TipoEventoChamado tipoEvento,
            String descricao
    ) {
        if (chamado == null) {
            throw new IllegalArgumentException(
                    "O chamado deve ser informado no histórico"
            );
        }

        if (tipoEvento == null) {
            throw new IllegalArgumentException(
                    "O tipo do evento deve ser informado"
            );
        }

        if (
                descricao == null ||
                        descricao.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "A descrição do evento deve ser informada"
            );
        }

        HistoricoChamado historico =
                new HistoricoChamado();

        historico.setChamado(chamado);
        historico.setOrdemServico(ordemServico);
        historico.setTipoEvento(tipoEvento);

        historico.setDescricao(
                descricao.trim()
        );

        historicoRepository.save(
                historico
        );
    }

    @Transactional(readOnly = true)
    public List<HistoricoChamadoResponse> listarPorChamado(
            Long contratoId,
            Long chamadoId
    ) {
        chamadoRepository
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

        return historicoRepository
                .findByChamadoIdAndChamadoUnidadeContratoIdOrderByDataEventoAsc(
                        chamadoId,
                        contratoId
                )
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    private HistoricoChamadoResponse converterParaResponse(
            HistoricoChamado historico
    ) {
        OrdemServico ordemServico =
                historico.getOrdemServico();

        Long ordemServicoId = null;
        String numeroOrdemServico = null;

        if (ordemServico != null) {
            ordemServicoId =
                    ordemServico.getId();

            numeroOrdemServico =
                    ordemServico
                            .getNumeroOrdemServico();
        }

        return new HistoricoChamadoResponse(
                historico.getId(),
                historico.getChamado().getId(),
                ordemServicoId,
                numeroOrdemServico,
                historico.getTipoEvento(),
                historico.getDescricao(),
                historico.getDataEvento()
        );
    }
}