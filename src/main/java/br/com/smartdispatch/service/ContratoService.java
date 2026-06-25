package br.com.smartdispatch.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.repository.ContratoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;

    public ContratoService(ContratoRepository contratoRepository) {
        this.contratoRepository = contratoRepository;
    }

    public Contrato criar(Contrato contrato) {
        return contratoRepository.save(contrato);
    }

    public List<Contrato> listar() {
        return contratoRepository.findAll();
    }

    public Contrato buscarPorId(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Contrato não encontrado"
                ));
    }

    public Contrato atualizar(Long id, Contrato novosDados) {

        Contrato contratoExistente = buscarPorId(id);

        contratoExistente.setCidade(novosDados.getCidade());
        contratoExistente.setSecretarioResponsavel(
                novosDados.getSecretarioResponsavel()
        );
        contratoExistente.setSlaHoras(novosDados.getSlaHoras());
        contratoExistente.setLinkPortalChamados(
                novosDados.getLinkPortalChamados()
        );

        return contratoRepository.save(contratoExistente);
    }

    public void excluir(Long id) {
        Contrato contrato = buscarPorId(id);
        contratoRepository.delete(contrato);
    }
}