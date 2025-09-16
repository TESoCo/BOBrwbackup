package com.example;

import com.example.servicio.UsuarioDetailsServices;
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
        public AuthenticationManager authenticationManager(
                AuthenticationConfiguration authConfig,
                UsuarioDetailsServices usuarioDetailsServices,
                PasswordEncoder passwordEncoder)
                throws Exception {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(usuarioDetailsServices);
            authProvider.setPasswordEncoder(passwordEncoder);

            return authConfig.getAuthenticationManager();
        }


        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
            http
                    .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                            authorizationManagerRequestMatcherRegistry
                                    .requestMatchers("/css/**","/js/**","/login","/presupuestos/**").permitAll()
                                    .requestMatchers("/BOBWS*", "/BOBWS/*").permitAll()
                                    .requestMatchers("/api/**").permitAll() // Allow API access with no auth
                                    .anyRequest()
                                    .authenticated()

                    )
                    .formLogin( form -> form
                            .loginPage("/login")
                            .defaultSuccessUrl("/redirigir", true)
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
