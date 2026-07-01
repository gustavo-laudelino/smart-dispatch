package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.AtualizarTecnicoRequest;
import br.com.smartdispatch.dto.CriarTecnicoRequest;
import br.com.smartdispatch.dto.TecnicoResponse;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.TecnicoRepository;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TecnicoService {

    private final TecnicoRepository tecnicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BaseOperacionalService baseOperacionalService;

    public TecnicoService(
            TecnicoRepository tecnicoRepository,
            UsuarioRepository usuarioRepository,
            BaseOperacionalService baseOperacionalService
    ) {
        this.tecnicoRepository = tecnicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.baseOperacionalService = baseOperacionalService;
    }

    @Transactional
    public TecnicoResponse criar(
            Long contratoId,
            Long baseId,
            CriarTecnicoRequest request
    ) {
        BaseOperacional baseOperacional =
                baseOperacionalService.buscarPorId(
                        contratoId,
                        baseId
                );

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um usuário cadastrado com este e-mail"
            );
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());
        usuario.setPerfil(PerfilUsuario.TECNICO);

        Usuario usuarioSalvo =
                usuarioRepository.save(usuario);

        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuarioSalvo);
        tecnico.setBaseOperacional(baseOperacional);
        tecnico.setAtivo(true);

        Tecnico tecnicoSalvo =
                tecnicoRepository.save(tecnico);

        return converterParaResponse(tecnicoSalvo);
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

    @Transactional
    public TecnicoResponse atualizar(
            Long contratoId,
            Long baseId,
            Long tecnicoId,
            AtualizarTecnicoRequest request
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

        Usuario usuario = tecnico.getUsuario();

        boolean emailPertenceAOutroUsuario =
                usuarioRepository.existsByEmailAndIdNot(
                        request.getEmail(),
                        usuario.getId()
                );

        if (emailPertenceAOutroUsuario) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe outro usuário cadastrado com este e-mail"
            );
        }

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());

        usuarioRepository.save(usuario);

        return converterParaResponse(tecnico);
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