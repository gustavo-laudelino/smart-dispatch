package br.com.smartdispatch.service;

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
}