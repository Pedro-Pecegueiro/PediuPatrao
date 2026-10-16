package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarios, PasswordEncoder encoder,
                                   @Value("${app.bootstrap.admin-password}") String adminPassword,
                                   @Value("${app.bootstrap.gerente-password}") String gerentePassword,
                                   @Value("${app.bootstrap.atendente-password}") String atendentePassword) {
        return args -> {
            criarUsuarioSeAusente(usuarios, encoder, "admin", adminPassword, PerfilUsuario.ADMIN);
            criarUsuarioSeAusente(usuarios, encoder, "gerente", gerentePassword, PerfilUsuario.GERENTE);
            criarUsuarioSeAusente(usuarios, encoder, "atendente", atendentePassword, PerfilUsuario.ATENDENTE);
        };
    }

    private void criarUsuarioSeAusente(UsuarioRepository repository, PasswordEncoder encoder,
                                        String username, String password, PerfilUsuario role) {
        repository.findByUsername(username).ifPresentOrElse(usuario -> {
            boolean mudou = false;
            if (usuario.getRole() == null) {
                usuario.setRole(role);
                mudou = true;
            }
            if (mudou) {
                repository.save(usuario);
            }
        }, () -> {
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPassword(encoder.encode(password));
            usuario.setRole(role);
            usuario.setAtivo(true);
            repository.save(usuario);
        });
    }
}
