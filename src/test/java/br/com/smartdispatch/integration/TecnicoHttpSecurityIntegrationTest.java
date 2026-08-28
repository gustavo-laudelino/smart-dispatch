package br.com.smartdispatch.integration;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.service.TokenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TecnicoHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void devePermitirListarTecnicosComoAdmin() throws Exception {
        Contrato contrato = persistirContrato();
        BaseOperacional base = persistirBaseOperacional(contrato);
        Tecnico tecnico = persistirTecnico(
                base,
                "Tecnico Base",
                "tecnico.base.013@teste.local"
        );
        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.013@teste.local",
                PerfilUsuario.ADMIN
        );
        String token = tokenService.gerarToken(admin);

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/bases/{baseId}/tecnicos",
                                contrato.getId(),
                                base.getId()
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarTecnicosComoCto() throws Exception {
        Contrato contrato = persistirContrato();
        BaseOperacional base = persistirBaseOperacional(contrato);
        Tecnico tecnico = persistirTecnico(
                base,
                "Tecnico Base",
                "tecnico.base.014@teste.local"
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.014@teste.local",
                PerfilUsuario.CTO
        );
        String token = tokenService.gerarToken(cto);

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/bases/{baseId}/tecnicos",
                                contrato.getId(),
                                base.getId()
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearListarTecnicosComoTecnico() throws Exception {
        Contrato contrato = persistirContrato();
        BaseOperacional base = persistirBaseOperacional(contrato);
        Usuario tecnicoAtor = persistirUsuario(
                "Tecnico Ator",
                "tecnico.ator.015@teste.local",
                PerfilUsuario.TECNICO
        );
        String token = tokenService.gerarToken(tecnicoAtor);

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/bases/{baseId}/tecnicos",
                                contrato.getId(),
                                base.getId()
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirBuscarTecnicoPorIdComoCto() throws Exception {
        Contrato contrato = persistirContrato();
        BaseOperacional base = persistirBaseOperacional(contrato);
        Tecnico tecnico = persistirTecnico(
                base,
                "Tecnico Base",
                "tecnico.base.016@teste.local"
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.016@teste.local",
                PerfilUsuario.CTO
        );
        String token = tokenService.gerarToken(cto);

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/bases/{baseId}/tecnicos/{tecnicoId}",
                                contrato.getId(),
                                base.getId(),
                                tecnico.getId()
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearBuscarTecnicoPorIdComoTecnicoInterno() throws Exception {
        Contrato contrato = persistirContrato();
        BaseOperacional base = persistirBaseOperacional(contrato);
        Tecnico tecnico = persistirTecnico(
                base,
                "Tecnico Base",
                "tecnico.base.017@teste.local"
        );
        Usuario tecnicoInternoAtor = persistirUsuario(
                "Tecnico Interno Ator",
                "tecnico.interno.ator.017@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        String token = tokenService.gerarToken(tecnicoInternoAtor);

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/bases/{baseId}/tecnicos/{tecnicoId}",
                                contrato.getId(),
                                base.getId(),
                                tecnico.getId()
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());
    }

    private Contrato persistirContrato() {
        Contrato contrato = new Contrato();
        entityManager.persist(contrato);
        return contrato;
    }

    private BaseOperacional persistirBaseOperacional(Contrato contrato) {
        BaseOperacional baseOperacional = new BaseOperacional();
        baseOperacional.setContrato(contrato);
        entityManager.persist(baseOperacional);
        return baseOperacional;
    }

    private Usuario persistirUsuario(
            String nome,
            String email,
            PerfilUsuario perfil
    ) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        usuario.setSenha("senha-teste");
        usuario.setAtivo(true);
        entityManager.persist(usuario);
        return usuario;
    }

    private Tecnico persistirTecnico(
            BaseOperacional base,
            String nome,
            String email
    ) {
        Usuario usuario = persistirUsuario(
                nome,
                email,
                PerfilUsuario.TECNICO
        );

        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuario);
        tecnico.setBaseOperacional(base);
        tecnico.setAtivo(true);
        entityManager.persist(tecnico);
        return tecnico;
    }
}
