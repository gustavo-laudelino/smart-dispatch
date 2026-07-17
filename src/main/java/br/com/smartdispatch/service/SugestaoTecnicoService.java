package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.SugestaoTecnicoResponse;
import br.com.smartdispatch.enums.NivelIndicacao;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.repository.OrdemServicoRepository;
import br.com.smartdispatch.repository.TecnicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class SugestaoTecnicoService {

    private static final double PESO_DISTANCIA = 4.0;
    private static final double PESO_OS_ATIVA = 2.0;
    private static final double PESO_ATENDIMENTO = 1.0;

    private static final int DIAS_BALANCEAMENTO = 15;

    private static final double LIMITE_INDICACAO_ALTA = 5.0;
    private static final double LIMITE_INDICACAO_MODERADA = 15.0;

    private final TecnicoRepository tecnicoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final DistanciaService distanciaService;

    public SugestaoTecnicoService(
            TecnicoRepository tecnicoRepository,
            OrdemServicoRepository ordemServicoRepository,
            DistanciaService distanciaService
    ) {
        this.tecnicoRepository = tecnicoRepository;
        this.ordemServicoRepository = ordemServicoRepository;
        this.distanciaService = distanciaService;
    }

    @Transactional(readOnly = true)
    public List<SugestaoTecnicoResponse> listarSugestoes(
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId
    ) {
        OrdemServico ordemServicoAlvo = ordemServicoRepository
                .findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId,
                        chamadoId,
                        contratoId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ordem de serviço não encontrada."
                ));

        Unidade unidadeDestino =
                ordemServicoAlvo.getUnidadeAtendimento();

        validarCoordenadasDestino(unidadeDestino);

        List<Tecnico> tecnicos = tecnicoRepository
                .findByBaseOperacionalContratoIdAndAtivoTrue(
                        contratoId
                );

        LocalDateTime inicioPeriodo = LocalDateTime.now()
                .minusDays(DIAS_BALANCEAMENTO);

        List<CandidatoTecnico> candidatos = tecnicos.stream()
                .map(tecnico -> criarCandidato(
                        tecnico,
                        contratoId,
                        ordemServicoId,
                        unidadeDestino,
                        inicioPeriodo
                ))
                .sorted(Comparator.comparingDouble(
                        CandidatoTecnico::pontuacao
                ))
                .toList();

        if (candidatos.isEmpty()) {
            return List.of();
        }

        double melhorPontuacao =
                candidatos.get(0).pontuacao();

        return candidatos.stream()
                .map(candidato -> converterParaResponse(
                        candidato,
                        melhorPontuacao
                ))
                .toList();
    }

    private CandidatoTecnico criarCandidato(
            Tecnico tecnico,
            Long contratoId,
            Long ordemServicoAlvoId,
            Unidade unidadeDestino,
            LocalDateTime inicioPeriodo
    ) {
        List<OrdemServico> ordensAtivas =
                ordemServicoRepository
                        .findByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutIsNull(
                                tecnico.getId(),
                                contratoId
                        )
                        .stream()
                        .filter(ordem ->
                                !ordem.getId().equals(ordemServicoAlvoId)
                        )
                        .toList();

        double distanciaKm = calcularDistanciaMelhorAncora(
                tecnico,
                ordensAtivas,
                unidadeDestino
        );

        int quantidadeOsAtivas = ordensAtivas.size();

        int atendimentosUltimos15Dias = Math.toIntExact(
                ordemServicoRepository
                        .countByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutGreaterThanEqual(
                                tecnico.getId(),
                                contratoId,
                                inicioPeriodo
                        )
        );

        double pontuacao =
                distanciaKm * PESO_DISTANCIA
                        + quantidadeOsAtivas * PESO_OS_ATIVA
                        + atendimentosUltimos15Dias * PESO_ATENDIMENTO;

        return new CandidatoTecnico(
                tecnico,
                pontuacao,
                distanciaKm,
                quantidadeOsAtivas,
                atendimentosUltimos15Dias
        );
    }

    private double calcularDistanciaMelhorAncora(
            Tecnico tecnico,
            List<OrdemServico> ordensAtivas,
            Unidade unidadeDestino
    ) {
        if (!ordensAtivas.isEmpty()) {
            return ordensAtivas.stream()
                    .map(OrdemServico::getUnidadeAtendimento)
                    .mapToDouble(unidadeAncora ->
                            distanciaService.calcularEmKm(
                                    unidadeAncora.getLatitude(),
                                    unidadeAncora.getLongitude(),
                                    unidadeDestino.getLatitude(),
                                    unidadeDestino.getLongitude()
                            )
                    )
                    .min()
                    .orElseThrow();
        }

        BaseOperacional base = tecnico.getBaseOperacional();

        return distanciaService.calcularEmKm(
                base.getLatitude(),
                base.getLongitude(),
                unidadeDestino.getLatitude(),
                unidadeDestino.getLongitude()
        );
    }

    private SugestaoTecnicoResponse converterParaResponse(
            CandidatoTecnico candidato,
            double melhorPontuacao
    ) {
        double diferenca =
                candidato.pontuacao() - melhorPontuacao;

        NivelIndicacao nivelIndicacao =
                definirNivelIndicacao(diferenca);

        return new SugestaoTecnicoResponse(
                candidato.tecnico().getId(),
                candidato.tecnico().getUsuario().getNome(),
                arredondar(candidato.pontuacao()),
                arredondar(candidato.distanciaKm()),
                candidato.quantidadeOsAtivas(),
                candidato.atendimentosUltimos15Dias(),
                nivelIndicacao
        );
    }

    private NivelIndicacao definirNivelIndicacao(
            double diferencaPontuacao
    ) {
        if (diferencaPontuacao <= LIMITE_INDICACAO_ALTA) {
            return NivelIndicacao.ALTA;
        }

        if (diferencaPontuacao <= LIMITE_INDICACAO_MODERADA) {
            return NivelIndicacao.MODERADA;
        }

        return NivelIndicacao.LEVE;
    }

    private void validarCoordenadasDestino(
            Unidade unidadeDestino
    ) {
        if (
                unidadeDestino.getLatitude() == null
                        || unidadeDestino.getLongitude() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A unidade de atendimento não possui coordenadas."
            );
        }
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private record CandidatoTecnico(
            Tecnico tecnico,
            double pontuacao,
            double distanciaKm,
            int quantidadeOsAtivas,
            int atendimentosUltimos15Dias
    ) {
    }
}