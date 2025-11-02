package com.example;

import com.example.domain.Persona;
import com.example.domain.Rol;
import com.example.domain.Usuario;
import com.example.servicio.PersonaServicio;
import com.example.servicio.RolServicio;
import com.example.servicio.UsuarioDetailsServices;
import com.example.servicio.UsuarioServicio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
public class ProtoBOB {

	public static void main(String[] args) {
		SpringApplication.run(ProtoBOB.class, args);
	}


    @Configuration

    public static class ConfSeg {

        @Bean
        public PasswordEncoder passwordEncoder() {
            //return NoOpPasswordEncoder.getInstance();
            return new BCryptPasswordEncoder();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider(
                UsuarioDetailsServices usuarioDetailsServices, PasswordEncoder passwordEncoder
        )
        {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(usuarioDetailsServices);
            authProvider.setPasswordEncoder(passwordEncoder);
            return authProvider;        };



        @Bean
        public AuthenticationManager authenticationManager(
                AuthenticationConfiguration authConfig,
                UsuarioDetailsServices usuarioDetailsServices,
                PasswordEncoder passwordEncoder)
                throws Exception {
            return authConfig.getAuthenticationManager();
        }


        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
            http
                    .authorizeHttpRequests(auth -> auth
                            // Public endpoints
                            .requestMatchers("/css/**", "/js/**", "/login", "/presupuestos/**").permitAll()
                            .requestMatchers("/BOBWS*", "/BOBWS/*").permitAll()
                            .requestMatchers("/api/**").permitAll() // Allow API access with no auth
                            .requestMatchers("/usuarios/foto/**").permitAll() // Permitir acceso a fotos de perfil

                            // Role-based access control
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .requestMatchers("/supervisor/**").hasRole("SUPERVISOR")
                            .requestMatchers("/operativo/**").hasRole("OPERATIVO")

                            // Permission-based access control (more granular)
                            .requestMatchers("/apu/crear").hasAuthority("CREAR_APU")
                            .requestMatchers("/apu/editar").hasAuthority("EDITAR_APU")
                            .requestMatchers("/avance/crear").hasAuthority("CREAR_AVANCE")
                            .requestMatchers("/avance/editar").hasAuthority("EDITAR_AVANCE")
                            .requestMatchers("/contratista/crear").hasAuthority("CREAR_CONTRATISTA")
                            .requestMatchers("/contratista/editar").hasAuthority("EDITAR_CONTRATISTA")
                            .requestMatchers("/fotodato/crear").hasAuthority("CREAR_FOTODATO")
                            .requestMatchers("/fotodato/editar").hasAuthority("EDITAR_FOTODATO")
                            .requestMatchers("/infoComarecial/crear").hasAuthority("CREAR_INFOCOMERCIAL")
                            .requestMatchers("/infoComercial/editar").hasAuthority("EDITAR_INFOCOMERCIAL")
                            .requestMatchers("/inventario/crear").hasAuthority("CREAR_INVENTARIO")
                            .requestMatchers("/inventario/editar").hasAuthority("EDITAR_INVENTARIO")
                            .requestMatchers("/material/crear").hasAuthority("CREAR_MATERIAL")
                            .requestMatchers("/material/editar").hasAuthority("EDITAR_MATERIAL")
                            .requestMatchers("/obra/crear").hasAuthority("CREAR_OBRA")
                            .requestMatchers("/obra/editar").hasAuthority("EDITAR_OBRA")
                            .requestMatchers("/permiso/crear").hasAuthority("CREAR_PERMISO")
                            .requestMatchers("/permiso/editar").hasAuthority("EDITAR_PERMISO")
                            .requestMatchers("/proveedor/crear").hasAuthority("CREAR_PROVEEDOR")
                            .requestMatchers("/proveedor/editar").hasAuthority("EDITAR_PROVEEDOR")
                            .requestMatchers("/rol/crear").hasAuthority("CREAR_ROL")
                            .requestMatchers("/rol/editar").hasAuthority("EDITAR_ROL")
                            .requestMatchers("/usuario/registrar").hasAuthority("CREAR_USUARIO")
                            .requestMatchers("/usuario/editar").hasAuthority("EDITAR_USUARIO")

                            // Usuarios endpoints - permission based
                            .requestMatchers("/usuarios/registrar").hasAnyAuthority("CREAR_USUARIO", "ROLE_ADMIN")
                            .requestMatchers("/usuarios/editar/**").hasAnyAuthority("EDITAR_USUARIO", "ROLE_ADMIN")
                            .requestMatchers("/usuarios/eliminar/**").hasAnyAuthority("EDITAR_USUARIO", "ROLE_ADMIN")
                            .requestMatchers("/usuarios").hasAnyAuthority("CREAR_USUARIO", "EDITAR_USUARIO", "ROLE_ADMIN")

                            // Combined role and permission access
                            .requestMatchers("/inventario/**").hasAnyRole("ADMIN", "SUPERVISOR")
                            .requestMatchers("/reportes/**").hasAnyRole("ADMIN", "SUPERVISOR")


                            .anyRequest().authenticated()
                    )
                    .formLogin(form -> form
                            .loginPage("/login")
                            .defaultSuccessUrl("/redirigir", true)
                            .permitAll()
                    )
                    .logout(logout -> logout
                            .logoutUrl("/logout")
                            .logoutSuccessUrl("/login?logout")
                            .permitAll()
                    )

                    .httpBasic(httpSecurityHttpBasicConfigurer -> {})
                    .csrf(csrf -> csrf
                            .ignoringRequestMatchers("/api/**") // Disable CSRF for API endpoints
                    );

            return http.build();
        }
    }
}
