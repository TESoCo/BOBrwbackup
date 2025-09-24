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

    @Bean
    public CommandLineRunner crearAdminPorDefecto(
            UsuarioServicio usuarioServicio,
            PersonaServicio personaServicio,
            RolServicio rolServicio,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // Verificar si ya existe un administrador
                if (usuarioServicio.encontrarPorNombreUsuario("admin") == null) {
                    try {
                        // 1. Crear persona
                        Persona persona = new Persona();
                        persona.setNombre("Administrador");
                        persona.setApellido("Sistema");
                        persona.setTelefono("000-000-0000");
                        persona.setCorreo("admin@bob.com");

                        System.out.println("Guardando persona...");
                        Persona personaGuardada = personaServicio.salvar(persona);
                        System.out.println("Persona guardada con ID: " + personaGuardada.getIdPersona());




                                // 2. Crear rol ADMIN si no existe
                        Rol rolAdmin = rolServicio.listarRoles().stream()
                                .filter(rol -> "ADMIN".equalsIgnoreCase(rol.getNombreRol()))
                                .findFirst()
                                .orElse(null);

                        if (rolAdmin == null) {
                            rolAdmin = new Rol();
                            rolAdmin.setNombreRol("ADMIN");
                            rolAdmin.setDescripRol("Administrador del sistema con todos los permisos");
                            rolServicio.guardar(rolAdmin);
                        }



                        // 3. Crear usuario admin
                        Usuario usuario = new Usuario();
                        usuario.setNombreUsuario("admin");
                        usuario.setPass_usuario(passwordEncoder.encode("admin123")); // Contraseña por defecto
                        usuario.setCargo("Administrador del Sistema");
                        usuario.setPersona(persona);
                        usuario.setRol(rolAdmin);

                        usuarioServicio.guardar(usuario);

                        System.out.println("Usuario administrador creado:");
                        System.out.println("Usuario: admin");
                        System.out.println("Contraseña: admin123");

                    } catch (Exception e) {
                        System.err.println("Error creando usuario administrador: " + e.getMessage());
                    }
                } else {
                    System.out.println("Usuario administrador ya existe");
            }
        };
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
                            .requestMatchers("/usuario/crear").hasAuthority("CREAR_USUARIO")
                            .requestMatchers("/usuario/editar").hasAuthority("EDITAR_USUARIO")

                            // Combined role and permission access
                            .requestMatchers("/inventario/**").hasAnyRole("ADMIN", "SUPERVISOR")
                            .requestMatchers("/reportes/**").hasAnyRole("ADMIN", "SUPERVISOR")

                            // Public endpoints
                            .requestMatchers("/css/**", "/js/**", "/login", "/presupuestos/**").permitAll()
                            .requestMatchers("/BOBWS*", "/BOBWS/*").permitAll()
                            .requestMatchers("/api/**").permitAll() // Allow API access with no auth
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
