package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.CriarTecnicoRequest;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.TecnicoRepository;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public Tecnico criar(
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

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuarioSalvo);
        tecnico.setBaseOperacional(baseOperacional);
        tecnico.setAtivo(true);

        return tecnicoRepository.save(tecnico);
    }
}