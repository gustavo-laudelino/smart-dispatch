package br.com.smartdispatch.service;

import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.repository.BaseOperacionalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BaseOperacionalService {

    private final BaseOperacionalRepository baseRepository;
    private final ContratoService contratoService;

    public BaseOperacionalService(
            BaseOperacionalRepository baseRepository,
            ContratoService contratoService
    ) {
        this.baseRepository = baseRepository;
        this.contratoService = contratoService;
    }

    public BaseOperacional criar(
            Long contratoId,
            BaseOperacional base
    ) {
        Contrato contrato = contratoService.buscarPorId(contratoId);

        base.setContrato(contrato);

        return baseRepository.save(base);
    }

    public List<BaseOperacional> listarPorContrato(Long contratoId) {
        contratoService.buscarPorId(contratoId);

        return baseRepository.findByContratoId(contratoId);
    }

    public BaseOperacional buscarPorId(
            Long contratoId,
            Long baseId
    ) {
        return baseRepository
                .findByIdAndContratoId(baseId, contratoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Base operacional não encontrada neste contrato"
                ));
    }

    public BaseOperacional atualizar(
            Long contratoId,
            Long baseId,
            BaseOperacional novosDados
    ) {
        BaseOperacional baseExistente =
                buscarPorId(contratoId, baseId);

        baseExistente.setNome(novosDados.getNome());
        baseExistente.setEndereco(novosDados.getEndereco());
        baseExistente.setCep(novosDados.getCep());
        baseExistente.setBairro(novosDados.getBairro());
        baseExistente.setCidade(novosDados.getCidade());
        baseExistente.setLatitude(novosDados.getLatitude());
        baseExistente.setLongitude(novosDados.getLongitude());

        return baseRepository.save(baseExistente);
    }

    public void excluir(Long contratoId, Long baseId) {
        BaseOperacional base = buscarPorId(contratoId, baseId);

        baseRepository.delete(base);
    }
}