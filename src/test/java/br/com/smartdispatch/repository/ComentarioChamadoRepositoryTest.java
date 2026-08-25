package br.com.smartdispatch.repository;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.enums.PrioridadeChamado;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.ComentarioChamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ComentarioChamadoRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ComentarioChamadoRepository comentarioChamadoRepository;

    @Test
    void deveListarComentariosDoChamadoEmOrdemCronologica() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-001", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.001@teste.local"
        );

        ComentarioChamado comentario1 = persistirComentario(
                chamadoA,
                usuarioA,
                "Comentario das 11h",
                LocalDateTime.of(2026, 8, 23, 11, 0)
        );
        ComentarioChamado comentario2 = persistirComentario(
                chamadoA,
                usuarioA,
                "Comentario das 09h",
                LocalDateTime.of(2026, 8, 23, 9, 0)
        );
        ComentarioChamado comentario3 = persistirComentario(
                chamadoA,
                usuarioA,
                "Comentario das 10h",
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );

        entityManager.flush();

        Long chamadoAId = chamadoA.getId();
        Long idComentario09h = comentario2.getId();
        Long idComentario10h = comentario3.getId();
        Long idComentario11h = comentario1.getId();

        entityManager.clear();

        List<ComentarioChamado> resultado = comentarioChamadoRepository
                .findByChamadoIdOrderByDataCriacaoAsc(chamadoAId);

        assertThat(resultado).hasSize(3);
        assertThat(resultado)
                .extracting(ComentarioChamado::getId)
                .containsExactly(
                        idComentario09h,
                        idComentario10h,
                        idComentario11h
                );
    }

    @Test
    void deveListarSomenteComentariosDoChamadoInformado() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-002-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-002-B", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.002@teste.local"
        );

        ComentarioChamado comentarioA1 = persistirComentario(
                chamadoA,
                usuarioA,
                "Comentario A1",
                LocalDateTime.of(2026, 8, 23, 9, 0)
        );
        ComentarioChamado comentarioA2 = persistirComentario(
                chamadoA,
                usuarioA,
                "Comentario A2",
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );
        persistirComentario(
                chamadoB,
                usuarioA,
                "Comentario B1",
                LocalDateTime.of(2026, 8, 23, 9, 0)
        );

        entityManager.flush();

        Long chamadoAId = chamadoA.getId();
        Long idComentarioA1 = comentarioA1.getId();
        Long idComentarioA2 = comentarioA2.getId();

        entityManager.clear();

        List<ComentarioChamado> resultado = comentarioChamadoRepository
                .findByChamadoIdOrderByDataCriacaoAsc(chamadoAId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado)
                .extracting(ComentarioChamado::getId)
                .containsExactlyInAnyOrder(idComentarioA1, idComentarioA2);
    }

    private Contrato persistirContrato() {
        Contrato contrato = new Contrato();
        entityManager.persist(contrato);
        return contrato;
    }

    private Unidade persistirUnidade(Contrato contrato) {
        Unidade unidade = new Unidade();
        unidade.setContrato(contrato);
        entityManager.persist(unidade);
        return unidade;
    }

    private Chamado persistirChamado(String numeroChamado, Unidade unidade) {
        Chamado chamado = new Chamado();
        chamado.setNumeroChamado(numeroChamado);
        chamado.setLinkChamadoOsti(
                "https://teste.local/chamado/" + numeroChamado
        );
        chamado.setUnidade(unidade);
        chamado.setTipo(TipoChamado.INCIDENTE);
        chamado.setCategoria(CategoriaChamado.OUTROS);
        chamado.setPrioridade(PrioridadeChamado.MEDIA);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDescricao("Chamado para teste de integração");
        chamado.setDataAbertura(
                LocalDateTime.of(2026, 8, 23, 8, 0)
        );
        entityManager.persist(chamado);
        return chamado;
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

    private ComentarioChamado persistirComentario(
            Chamado chamado,
            Usuario autor,
            String texto,
            LocalDateTime dataCriacao
    ) {
        ComentarioChamado comentario = new ComentarioChamado();
        comentario.setChamado(chamado);
        comentario.setAutor(autor);
        comentario.setTexto(texto);
        comentario.setDataCriacao(dataCriacao);
        entityManager.persist(comentario);
        return comentario;
    }
}
