package br.com.smartdispatch.repository;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class TecnicoRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    @Test
    void deveConfirmarUsuarioVinculadoATecnicoDoContrato() {
        Contrato contratoA = persistirContrato();
        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.001@teste.local"
        );
        persistirTecnico(usuarioA, baseA, true);

        entityManager.flush();

        Long usuarioAId = usuarioA.getId();
        Long contratoAId = contratoA.getId();

        entityManager.clear();

        boolean resultado = tecnicoRepository
                .existsByUsuarioIdAndBaseOperacionalContratoId(
                        usuarioAId,
                        contratoAId
                );

        assertThat(resultado).isTrue();
    }

    @Test
    void deveNegarUsuarioQuandoTecnicoPertenceAOutroContrato() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.002@teste.local"
        );
        persistirTecnico(usuarioA, baseA, true);

        entityManager.flush();

        Long usuarioAId = usuarioA.getId();
        Long contratoBId = contratoB.getId();

        entityManager.clear();

        boolean resultado = tecnicoRepository
                .existsByUsuarioIdAndBaseOperacionalContratoId(
                        usuarioAId,
                        contratoBId
                );

        assertThat(resultado).isFalse();
    }

    @Test
    void deveBuscarTecnicoPorUsuarioId() {
        Contrato contratoA = persistirContrato();
        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.003@teste.local"
        );
        Tecnico tecnicoA = persistirTecnico(usuarioA, baseA, true);

        entityManager.flush();

        Long usuarioAId = usuarioA.getId();
        Long tecnicoAId = tecnicoA.getId();

        entityManager.clear();

        Optional<Tecnico> resultado =
                tecnicoRepository.findByUsuarioId(usuarioAId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(tecnicoAId);
        assertThat(resultado.get().getUsuario().getId())
                .isEqualTo(usuarioAId);
    }

    @Test
    void deveRetornarVazioQuandoUsuarioNaoPossuiTecnico() {
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.004@teste.local"
        );

        entityManager.flush();

        Long usuarioAId = usuarioA.getId();

        entityManager.clear();

        Optional<Tecnico> resultado =
                tecnicoRepository.findByUsuarioId(usuarioAId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveListarSomenteTecnicosDaBaseInformada() {
        Contrato contratoA = persistirContrato();
        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        BaseOperacional baseB = persistirBaseOperacional(contratoA);

        Usuario usuarioA1 = persistirUsuario(
                "Usuario Teste A1",
                "usuario.a1.005@teste.local"
        );
        Usuario usuarioA2 = persistirUsuario(
                "Usuario Teste A2",
                "usuario.a2.005@teste.local"
        );
        Usuario usuarioB1 = persistirUsuario(
                "Usuario Teste B1",
                "usuario.b1.005@teste.local"
        );

        Tecnico tecnicoA1 = persistirTecnico(usuarioA1, baseA, true);
        Tecnico tecnicoA2 = persistirTecnico(usuarioA2, baseA, false);
        persistirTecnico(usuarioB1, baseB, true);

        entityManager.flush();

        Long baseAId = baseA.getId();
        Long tecnicoIdA1 = tecnicoA1.getId();
        Long tecnicoIdA2 = tecnicoA2.getId();

        entityManager.clear();

        List<Tecnico> resultado =
                tecnicoRepository.findByBaseOperacionalId(baseAId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado)
                .extracting(Tecnico::getId)
                .containsExactlyInAnyOrder(tecnicoIdA1, tecnicoIdA2);
    }

    @Test
    void deveBuscarTecnicoQuandoIdEBaseCorrespondem() {
        Contrato contratoA = persistirContrato();
        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.006@teste.local"
        );
        Tecnico tecnicoA = persistirTecnico(usuarioA, baseA, true);

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long baseAId = baseA.getId();

        entityManager.clear();

        Optional<Tecnico> resultado = tecnicoRepository
                .findByIdAndBaseOperacionalId(tecnicoAId, baseAId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(tecnicoAId);
    }

    @Test
    void deveRetornarVazioQuandoTecnicoPertenceAOutraBase() {
        Contrato contratoA = persistirContrato();
        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        BaseOperacional baseB = persistirBaseOperacional(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.007@teste.local"
        );
        Tecnico tecnicoA = persistirTecnico(usuarioA, baseA, true);

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long baseBId = baseB.getId();

        entityManager.clear();

        Optional<Tecnico> resultado = tecnicoRepository
                .findByIdAndBaseOperacionalId(tecnicoAId, baseBId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveBuscarTecnicoQuandoIdEContratoCorrespondem() {
        Contrato contratoA = persistirContrato();
        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.008@teste.local"
        );
        Tecnico tecnicoA = persistirTecnico(usuarioA, baseA, true);

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long contratoAId = contratoA.getId();

        entityManager.clear();

        Optional<Tecnico> resultado = tecnicoRepository
                .findByIdAndBaseOperacionalContratoId(
                        tecnicoAId,
                        contratoAId
                );

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(tecnicoAId);
        assertThat(
                resultado.get().getBaseOperacional().getContrato().getId()
        ).isEqualTo(contratoAId);
    }

    @Test
    void deveRetornarVazioQuandoTecnicoPertenceAOutroContrato() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.009@teste.local"
        );
        Tecnico tecnicoA = persistirTecnico(usuarioA, baseA, true);

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long contratoBId = contratoB.getId();

        entityManager.clear();

        Optional<Tecnico> resultado = tecnicoRepository
                .findByIdAndBaseOperacionalContratoId(
                        tecnicoAId,
                        contratoBId
                );

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveListarSomenteTecnicosAtivosDoContrato() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        BaseOperacional baseA1 = persistirBaseOperacional(contratoA);
        BaseOperacional baseA2 = persistirBaseOperacional(contratoA);
        BaseOperacional baseB1 = persistirBaseOperacional(contratoB);

        Usuario usuarioA1 = persistirUsuario(
                "Usuario Teste A1",
                "usuario.a1.010@teste.local"
        );
        Usuario usuarioA2 = persistirUsuario(
                "Usuario Teste A2",
                "usuario.a2.010@teste.local"
        );
        Usuario usuarioB1 = persistirUsuario(
                "Usuario Teste B1",
                "usuario.b1.010@teste.local"
        );

        Tecnico tecnicoA1 = persistirTecnico(usuarioA1, baseA1, true);
        persistirTecnico(usuarioA2, baseA2, false);
        persistirTecnico(usuarioB1, baseB1, true);

        entityManager.flush();

        Long contratoAId = contratoA.getId();
        Long tecnicoIdA1 = tecnicoA1.getId();

        entityManager.clear();

        List<Tecnico> resultado = tecnicoRepository
                .findByBaseOperacionalContratoIdAndAtivoTrue(contratoAId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(tecnicoIdA1);
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

    private Usuario persistirUsuario(String nome, String email) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(PerfilUsuario.TECNICO);
        usuario.setSenha("senha-teste");
        usuario.setAtivo(true);
        entityManager.persist(usuario);
        return usuario;
    }

    private Tecnico persistirTecnico(
            Usuario usuario,
            BaseOperacional baseOperacional,
            boolean ativo
    ) {
        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuario);
        tecnico.setBaseOperacional(baseOperacional);
        tecnico.setAtivo(ativo);
        entityManager.persist(tecnico);
        return tecnico;
    }
}
