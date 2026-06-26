package br.com.smartdispatch.service;

import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.repository.UnidadeRepository;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@Service
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final ContratoService contratoService;

    public UnidadeService(
            UnidadeRepository unidadeRepository,
            ContratoService contratoService
    ) {
        this.unidadeRepository = unidadeRepository;
        this.contratoService = contratoService;
    }

    public Unidade criar(Long contratoId, Unidade unidade) {

        Contrato contrato = contratoService.buscarPorId(contratoId);

        unidade.setContrato(contrato);

        return unidadeRepository.save(unidade);
    }

    public List<Unidade> listarPorContrato(Long contratoId) {

        contratoService.buscarPorId(contratoId);

        return unidadeRepository.findByContratoId(contratoId);
    }

    public Unidade buscarPorId(Long contratoId, Long unidadeId) {
        return unidadeRepository
                .findByIdAndContratoId(unidadeId, contratoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Unidade não encontrada neste contrato"
                ));
    }
    public Unidade atualizar(
            Long contratoId,
            Long unidadeId,
            Unidade novosDados
    ) {
        Unidade unidadeExistente = buscarPorId(contratoId, unidadeId);

        unidadeExistente.setNome(novosDados.getNome());
        unidadeExistente.setEndereco(novosDados.getEndereco());
        unidadeExistente.setCep(novosDados.getCep());
        unidadeExistente.setBairro(novosDados.getBairro());
        unidadeExistente.setCidade(novosDados.getCidade());
        unidadeExistente.setLatitude(novosDados.getLatitude());
        unidadeExistente.setLongitude(novosDados.getLongitude());

        return unidadeRepository.save(unidadeExistente);
    }

    public void excluir(Long contratoId, Long unidadeId) {
        Unidade unidade = buscarPorId(contratoId, unidadeId);
        unidadeRepository.delete(unidade);
    }
}