package br.com.smartdispatch.service;

import br.com.smartdispatch.repository.TecnicoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import br.com.smartdispatch.dto.StatusChamadoRequest;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.repository.OrdemServicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service("autorizacaoService")
public class AutorizacaoService {

    private final OrdemServicoRepository ordemServicoRepository;

    public boolean podeAlterarStatusChamado(
            Authentication authentication,
            Long contratoId,
            StatusChamadoRequest request
    ) {
        boolean gestor = authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_CTO")
                );

        if (gestor) {
            return true;
        }

        if (!tecnicoPertenceAoContrato(authentication, contratoId)) {
            return false;
        }

        if (request == null || request.getStatus() == null) {
            return true;
        }

        return request.getStatus() != StatusChamado.FINALIZADO;
    }

    private final TecnicoRepository tecnicoRepository;

    public AutorizacaoService(
            OrdemServicoRepository ordemServicoRepository, TecnicoRepository tecnicoRepository
    ) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.tecnicoRepository = tecnicoRepository;
    }

    public boolean tecnicoPertenceAoContrato(
            Authentication authentication,
            Long contratoId
    ) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }

        Number usuarioId = jwtAuthentication
                .getToken()
                .getClaim("usuarioId");

        if (usuarioId == null) {
            return false;
        }

        return tecnicoRepository
                .existsByUsuarioIdAndBaseOperacionalContratoId(
                        usuarioId.longValue(),
                        contratoId
                );
    }

    public boolean tecnicoAtribuidoAOrdemServico(
            Authentication authentication,
            Long contratoId,
            Long chamadoId,
            Long ordemServicoId
    ) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }

        Number usuarioId = jwtAuthentication
                .getToken()
                .getClaim("usuarioId");

        if (usuarioId == null) {
            return false;
        }

        return ordemServicoRepository
                .existsByIdAndTecnicoUsuarioIdAndChamadoIdAndChamadoUnidadeContratoIdAndChamadoStatusNot(
                        ordemServicoId,
                        usuarioId.longValue(),
                        chamadoId,
                        contratoId,
                        StatusChamado.FINALIZADO
                );
    }

    public Long obterContratoIdTecnico(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return null;
        }

        Number usuarioId = jwtAuthentication
                .getToken()
                .getClaim("usuarioId");

        if (usuarioId == null) {
            return null;
        }

        return tecnicoRepository
                .findByUsuarioId(usuarioId.longValue())
                .map(tecnico ->
                        tecnico
                                .getBaseOperacional()
                                .getContrato()
                                .getId()
                )
                .orElse(null);
    }

    public Long resolverContratoPermitido(
            Authentication authentication,
            Long contratoIdSolicitado
    ) {
        boolean gestor = authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_CTO")
                );

        if (gestor) {
            return contratoIdSolicitado;
        }

        Long contratoIdTecnico =
                obterContratoIdTecnico(authentication);

        if (contratoIdTecnico == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuário não possui vínculo com um contrato"
            );
        }

        if (contratoIdSolicitado != null
                && !contratoIdTecnico.equals(contratoIdSolicitado)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuário não possui acesso a este contrato"
            );
        }

        return contratoIdTecnico;
    }
}