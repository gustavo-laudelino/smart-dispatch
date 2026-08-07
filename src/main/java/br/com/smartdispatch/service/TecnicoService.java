package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.TecnicoResponse;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.TecnicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class TecnicoService {

    private final TecnicoRepository tecnicoRepository;
    private final BaseOperacionalService baseOperacionalService;


    public TecnicoService(
            TecnicoRepository tecnicoRepository,
            BaseOperacionalService baseOperacionalService
    ) {
        this.tecnicoRepository = tecnicoRepository;
        this.baseOperacionalService = baseOperacionalService;
    }


    @Transactional(readOnly = true)
    public List<TecnicoResponse> listar(
            Long contratoId,
            Long baseId
    ) {
        baseOperacionalService.buscarPorId(
                contratoId,
                baseId
        );

        return tecnicoRepository
                .findByBaseOperacionalId(baseId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TecnicoResponse buscarPorId(
            Long contratoId,
            Long baseId,
            Long tecnicoId
    ) {
        baseOperacionalService.buscarPorId(
                contratoId,
                baseId
        );

        Tecnico tecnico = tecnicoRepository
                .findByIdAndBaseOperacionalId(
                        tecnicoId,
                        baseId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Técnico não encontrado nesta base operacional"
                ));

        return converterParaResponse(tecnico);
    }

    @Transactional(readOnly = true)
    public Tecnico buscarEntidadePorId(
            Long contratoId,
            Long tecnicoId
    ) {
        Tecnico tecnico = tecnicoRepository
                .findByIdAndBaseOperacionalContratoId(
                        tecnicoId,
                        contratoId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Técnico não encontrado neste contrato"
                ));

        if (!tecnico.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível atribuir uma ordem de serviço a um técnico inativo"
            );
        }

        return tecnico;
    }

    private TecnicoResponse converterParaResponse(
            Tecnico tecnico
    ) {
        Usuario usuario = tecnico.getUsuario();

        BaseOperacional baseOperacional =
                tecnico.getBaseOperacional();

        Contrato contrato =
                baseOperacional.getContrato();

        return new TecnicoResponse(
                tecnico.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getPerfil().name(),
                tecnico.isAtivo(),
                baseOperacional.getId(),
                baseOperacional.getNome(),
                contrato.getId(),
                contrato.getCidade()
        );
    }
}