package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(
            String email,
            Long id
    );

    Optional<Usuario> findByEmailIgnoreCase(
            String email
    );
}