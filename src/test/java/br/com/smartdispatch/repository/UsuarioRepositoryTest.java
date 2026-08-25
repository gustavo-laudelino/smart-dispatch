package br.com.smartdispatch.repository;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class UsuarioRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void deveConfirmarQuandoEmailJaExiste() {
        persistirUsuario("Usuario Teste A", "usuario.a.001@teste.local");

        entityManager.flush();
        entityManager.clear();

        boolean resultado = usuarioRepository
                .existsByEmail("usuario.a.001@teste.local");

        assertThat(resultado).isTrue();
    }

    @Test
    void deveNegarQuandoEmailNaoExiste() {
        persistirUsuario("Usuario Teste A", "usuario.a.002@teste.local");

        entityManager.flush();
        entityManager.clear();

        boolean resultado = usuarioRepository
                .existsByEmail("email.inexistente@teste.local");

        assertThat(resultado).isFalse();
    }

    @Test
    void deveConfirmarQuandoEmailPertenceAOutroUsuario() {
        persistirUsuario("Usuario Teste A", "usuario.a.003@teste.local");
        Usuario usuarioB = persistirUsuario(
                "Usuario Teste B",
                "usuario.b.003@teste.local"
        );

        entityManager.flush();

        Long usuarioBId = usuarioB.getId();

        entityManager.clear();

        boolean resultado = usuarioRepository.existsByEmailAndIdNot(
                "usuario.a.003@teste.local",
                usuarioBId
        );

        assertThat(resultado).isTrue();
    }

    @Test
    void deveNegarConflitoQuandoEmailPertenceAoProprioUsuario() {
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.004@teste.local"
        );

        entityManager.flush();

        Long usuarioAId = usuarioA.getId();

        entityManager.clear();

        boolean resultado = usuarioRepository.existsByEmailAndIdNot(
                "usuario.a.004@teste.local",
                usuarioAId
        );

        assertThat(resultado).isFalse();
    }

    @Test
    void deveBuscarUsuarioPorEmailIgnorandoMaiusculasEMinusculas() {
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste",
                "usuario.teste.005@teste.local"
        );

        entityManager.flush();

        Long usuarioAId = usuarioA.getId();

        entityManager.clear();

        Optional<Usuario> resultado = usuarioRepository
                .findByEmailIgnoreCase("USUARIO.TESTE.005@TESTE.LOCAL");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(usuarioAId);
        assertThat(resultado.get().getEmail())
                .isEqualTo("usuario.teste.005@teste.local");
    }

    @Test
    void deveRetornarVazioQuandoEmailNaoExiste() {
        persistirUsuario("Usuario Teste A", "usuario.a.006@teste.local");

        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> resultado = usuarioRepository
                .findByEmailIgnoreCase("nao.existe@teste.local");

        assertThat(resultado).isEmpty();
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
}
