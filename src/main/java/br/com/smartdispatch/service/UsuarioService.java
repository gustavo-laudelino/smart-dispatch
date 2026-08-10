package br.com.smartdispatch.service;

import br.com.smartdispatch.repository.TecnicoRepository;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import br.com.smartdispatch.dto.CriarUsuarioRequest;
import br.com.smartdispatch.dto.UsuarioResponse;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import br.com.smartdispatch.dto.AtualizarUsuarioRequest;
import br.com.smartdispatch.dto.AtualizarStatusUsuarioRequest;
import br.com.smartdispatch.dto.AlterarSenhaRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TecnicoRepository tecnicoRepository;
    private final BaseOperacionalService baseOperacionalService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            TecnicoRepository tecnicoRepository,
            BaseOperacionalService baseOperacionalService,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.baseOperacionalService = baseOperacionalService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest request) {

        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nome deve ser informado"
            );
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O e-mail deve ser informado"
            );
        }

        if (request.getPerfil() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O perfil deve ser informado"
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (usuarioRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um usuário cadastrado com este e-mail"
            );
        }

        boolean perfilTecnico =
                request.getPerfil() == PerfilUsuario.TECNICO
                        || request.getPerfil() == PerfilUsuario.TECNICO_INTERNO;

        BaseOperacional baseOperacional = null;

        if (perfilTecnico) {

            if (request.getContratoId() == null
                    || request.getBaseOperacionalId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Contrato e base operacional devem ser informados para técnicos"
                );
            }

            baseOperacional = baseOperacionalService.buscarPorId(
                    request.getContratoId(),
                    request.getBaseOperacionalId()
            );
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(request.getTelefone());
        usuario.setPerfil(request.getPerfil());
        usuario.setSenha(passwordEncoder.encode("cto"));
        usuario.setAtivo(true);

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        if (perfilTecnico) {
            Tecnico tecnico = new Tecnico();
            tecnico.setUsuario(usuarioSalvo);
            tecnico.setBaseOperacional(baseOperacional);
            tecnico.setAtivo(true);

            tecnicoRepository.save(tecnico);
        }

        return converterParaResponse(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository
                .findAll()
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long usuarioId) {
        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        return converterParaResponse(usuario);
    }

    private UsuarioResponse converterParaResponse(Usuario usuario) {

        Long contratoId = null;
        Long baseOperacionalId = null;

        boolean perfilTecnico =
                usuario.getPerfil() == PerfilUsuario.TECNICO
                        || usuario.getPerfil() == PerfilUsuario.TECNICO_INTERNO;

        if (perfilTecnico) {
            Tecnico tecnico = tecnicoRepository
                    .findByUsuarioId(usuario.getId())
                    .orElse(null);

            if (tecnico != null) {
                BaseOperacional baseOperacional =
                        tecnico.getBaseOperacional();

                baseOperacionalId = baseOperacional.getId();
                contratoId = baseOperacional.getContrato().getId();
            }
        }

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                contratoId,
                baseOperacionalId
        );
    }

    @Transactional
    public UsuarioResponse atualizar(
            Long usuarioId,
            AtualizarUsuarioRequest request
    ) {
        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        PerfilUsuario perfilAtual = usuario.getPerfil();

        if (request.getPerfil() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O perfil deve ser informado"
            );
        }

        PerfilUsuario novoPerfil = request.getPerfil();

        boolean eraTecnico =
                perfilAtual == PerfilUsuario.TECNICO
                        || perfilAtual == PerfilUsuario.TECNICO_INTERNO;

        boolean seraTecnico =
                novoPerfil == PerfilUsuario.TECNICO
                        || novoPerfil == PerfilUsuario.TECNICO_INTERNO;

        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nome deve ser informado"
            );
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O e-mail deve ser informado"
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (usuarioRepository.existsByEmailAndIdNot(
                email,
                usuarioId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe outro usuário cadastrado com este e-mail"
            );
        }


        usuario.setNome(request.getNome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(request.getTelefone());

        boolean informouContrato = request.getContratoId() != null;
        boolean informouBase = request.getBaseOperacionalId() != null;

        if (seraTecnico && informouContrato != informouBase) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Contrato e base operacional devem ser informados juntos"
            );
        }

        Tecnico tecnico = tecnicoRepository
                .findByUsuarioId(usuarioId)
                .orElse(null);

        if (eraTecnico && tecnico == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Usuário técnico não possui vínculo operacional"
            );
        }

        if (!eraTecnico && seraTecnico) {

            if (!informouContrato) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Contrato e base operacional devem ser informados ao transformar um usuário em técnico"
                );
            }

            BaseOperacional novaBase =
                    baseOperacionalService.buscarPorId(
                            request.getContratoId(),
                            request.getBaseOperacionalId()
                    );

            if (tecnico == null) {
                tecnico = new Tecnico();
                tecnico.setUsuario(usuario);
                tecnico.setBaseOperacional(novaBase);
                tecnico.setAtivo(usuario.isAtivo());

                tecnicoRepository.save(tecnico);
            } else {
                tecnico.setBaseOperacional(novaBase);
                tecnico.setAtivo(usuario.isAtivo());
            }
        }

        if (eraTecnico && seraTecnico && informouContrato) {

            BaseOperacional novaBase = baseOperacionalService.buscarPorId(
                    request.getContratoId(),
                    request.getBaseOperacionalId()
            );

            tecnico.setBaseOperacional(novaBase);
        }

        if (eraTecnico && !seraTecnico) {
            tecnico.setAtivo(false);
        }

        usuario.setPerfil(novoPerfil);
        Usuario usuarioAtualizado =
                usuarioRepository.save(usuario);

        return converterParaResponse(usuarioAtualizado);
    }

    @Transactional
    public UsuarioResponse atualizarStatus(
            Long usuarioId,
            AtualizarStatusUsuarioRequest request
    ) {
        if (request.getAtivo() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O campo ativo deve ser informado"
            );
        }

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        usuario.setAtivo(request.getAtivo());

        boolean perfilTecnico =
                usuario.getPerfil() == PerfilUsuario.TECNICO
                        || usuario.getPerfil() == PerfilUsuario.TECNICO_INTERNO;

        if (perfilTecnico) {
            Tecnico tecnico = tecnicoRepository
                    .findByUsuarioId(usuarioId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Usuário técnico não possui vínculo operacional"
                    ));

            tecnico.setAtivo(request.getAtivo());
        }

        Usuario usuarioAtualizado =
                usuarioRepository.save(usuario);

        return converterParaResponse(usuarioAtualizado);
    }

    @Transactional
    public void resetarSenha(Long usuarioId) {
        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        usuario.setSenha(
                passwordEncoder.encode("cto")
        );

        usuarioRepository.save(usuario);
    }

    @Transactional
    public void alterarPropriaSenha(
            AlterarSenhaRequest request,
            Authentication authentication
    ) {
        if (request.getSenhaAtual() == null
                || request.getSenhaAtual().isBlank()
                || request.getNovaSenha() == null
                || request.getNovaSenha().isBlank()
                || request.getConfirmacaoSenha() == null
                || request.getConfirmacaoSenha().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Todas as senhas devem ser informadas"
            );
        }

        if (!request.getNovaSenha().equals(request.getConfirmacaoSenha())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A confirmação da senha não corresponde à nova senha"
            );
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado"
            );
        }

        Number usuarioId = jwtAuthentication
                .getToken()
                .getClaim("usuarioId");

        if (usuarioId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não identificado"
            );
        }

        Usuario usuario = usuarioRepository
                .findById(usuarioId.longValue())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        if (!passwordEncoder.matches(
                request.getSenhaAtual(),
                usuario.getSenha()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Senha atual incorreta"
            );
        }

        usuario.setSenha(
                passwordEncoder.encode(request.getNovaSenha())
        );

        usuarioRepository.save(usuario);
    }
}