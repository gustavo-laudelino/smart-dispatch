package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.ComentarioChamadoRequest;
import br.com.smartdispatch.dto.ComentarioChamadoResponse;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.ComentarioChamado;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.ComentarioChamadoRepository;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import br.com.smartdispatch.enums.StatusChamado;

@Service
public class ComentarioChamadoService {

    private final ComentarioChamadoRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChamadoService chamadoService;
    private final OrdemServicoService ordemServicoService;

    public ComentarioChamadoService(
            ComentarioChamadoRepository comentarioRepository,
            UsuarioRepository usuarioRepository,
            ChamadoService chamadoService,
            OrdemServicoService ordemServicoService
    ) {
        this.comentarioRepository = comentarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.chamadoService = chamadoService;
        this.ordemServicoService = ordemServicoService;
    }

    @Transactional
    public ComentarioChamadoResponse criar(
            Long contratoId,
            Long chamadoId,
            ComentarioChamadoRequest request,
            Authentication authentication
    ) {
        validarRequest(request);

        Chamado chamado = chamadoService.buscarEntidadePorId(
                contratoId,
                chamadoId
        );

        boolean gestor = authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_CTO")
                );

        if (chamado.getStatus() == StatusChamado.FINALIZADO && !gestor) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Chamado finalizado não permite novos comentários"
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

        Usuario autor = usuarioRepository
                .findById(usuarioId.longValue())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Autor não encontrado"
                ));

        OrdemServico ordemServico = null;

        if (request.getOrdemServicoId() != null) {
            ordemServico = ordemServicoService.buscarEntidadePorId(
                    contratoId,
                    chamadoId,
                    request.getOrdemServicoId()
            );
        }

        ComentarioChamado comentario = new ComentarioChamado();

        comentario.setChamado(chamado);
        comentario.setAutor(autor);
        comentario.setOrdemServico(ordemServico);
        comentario.setTexto(request.getTexto().trim());
        comentario.setDataCriacao(LocalDateTime.now());

        ComentarioChamado comentarioSalvo =
                comentarioRepository.save(comentario);

        return converterParaResponse(comentarioSalvo);
    }

    @Transactional(readOnly = true)
    public List<ComentarioChamadoResponse> listarPorChamado(
            Long contratoId,
            Long chamadoId
    ) {
        chamadoService.buscarEntidadePorId(
                contratoId,
                chamadoId
        );

        return comentarioRepository
                .findByChamadoIdOrderByDataCriacaoAsc(chamadoId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    private void validarRequest(
            ComentarioChamadoRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os dados do comentário devem ser informados"
            );
        }

        if (request.getTexto() == null
                || request.getTexto().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O texto do comentário deve ser informado"
            );
        }

        if (request.getTexto().trim().length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O comentário deve possuir no máximo 2000 caracteres"
            );
        }
    }

    private ComentarioChamadoResponse converterParaResponse(
            ComentarioChamado comentario
    ) {
        OrdemServico ordemServico =
                comentario.getOrdemServico();

        Long ordemServicoId = null;
        String numeroOrdemServico = null;

        if (ordemServico != null) {
            ordemServicoId = ordemServico.getId();
            numeroOrdemServico =
                    ordemServico.getNumeroOrdemServico();
        }

        return new ComentarioChamadoResponse(
                comentario.getId(),
                comentario.getChamado().getId(),
                comentario.getAutor().getId(),
                comentario.getAutor().getNome(),
                ordemServicoId,
                numeroOrdemServico,
                comentario.getTexto(),
                comentario.getDataCriacao()
        );
    }
}