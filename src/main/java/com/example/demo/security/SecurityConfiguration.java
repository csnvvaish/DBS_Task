package com.example.demo.security;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.function.Supplier;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookiePath("/");

        return repository;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

        @Bean
        SessionRegistry sessionRegistry() {
                return new SessionRegistryImpl();
        }

        @Bean
        HttpSessionEventPublisher httpSessionEventPublisher() {
                return new HttpSessionEventPublisher();
        }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(
                        CsrfTokenRepository csrfTokenRepository,
                        SessionRegistry sessionRegistry) {

        return new CompositeSessionAuthenticationStrategy(
                List.of(
                        new ChangeSessionIdAuthenticationStrategy(),
                                                new CsrfAuthenticationStrategy(csrfTokenRepository),
                                                new RegisterSessionAuthenticationStrategy(sessionRegistry)
                )
        );
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                java.util.Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList()
        );

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                List.of("Content-Type", "X-XSRF-TOKEN")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository,
            SessionRegistry sessionRegistry,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper objectMapper) throws Exception {

        http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource))

                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(
                                new SpaCsrfTokenRequestHandler()))

                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(false))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation ->
                                fixation.changeSessionId()))

                .httpBasic(basic -> basic.disable())

                .formLogin(form -> form.disable())

                .exceptionHandling(exceptions -> exceptions

                        .authenticationEntryPoint(
                                (request, response, exception) -> {

                                    response.setStatus(
                                            HttpStatus.UNAUTHORIZED.value());

                                    response.setContentType(
                                            "application/json");

                                    objectMapper.writeValue(
                                            response.getOutputStream(),
                                            java.util.Map.of(
                                                    "error",
                                                    "Authentication required"
                                            )
                                    );
                                })

                        .accessDeniedHandler(
                                (request, response, exception) -> {

                                    response.setStatus(
                                            HttpStatus.FORBIDDEN.value());

                                    response.setContentType(
                                            "application/json");

                                    objectMapper.writeValue(
                                            response.getOutputStream(),
                                            java.util.Map.of(
                                                    "error",
                                                    "Access denied"
                                            )
                                    );
                                })
                )

                .authorizeHttpRequests(authorize -> authorize

                        // Allow Spring's error dispatch to expose
                        // the original application error instead
                        // of converting it into a 401.
                        .requestMatchers("/error")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login")
                        .permitAll()

                        .requestMatchers("/api/auth/me")
                        .authenticated()

                        .requestMatchers("/api/user/**")
                        .hasRole("USER")

                        .requestMatchers("/api/manager/**")
                        .hasRole("MANAGER")

                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        .anyRequest()
                        .denyAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies(
                                "JSESSIONID",
                                "XSRF-TOKEN")
                        .logoutSuccessHandler(
                                new HttpStatusReturningLogoutSuccessHandler(
                                        HttpStatus.NO_CONTENT)));

        http.addFilterBefore(
                new ConcurrentSessionFilter(
                        sessionRegistry,
                        event -> {
                            event.getResponse().setStatus(
                                    HttpStatus.UNAUTHORIZED.value());
                            event.getResponse().setContentType(
                                    "application/json");
                            objectMapper.writeValue(
                                    event.getResponse().getOutputStream(),
                                    java.util.Map.of(
                                            "error",
                                            "Session expired; authenticate again"
                                    ));
                        }),
                AuthorizationFilter.class);

        return http.build();
    }

    private static final class SpaCsrfTokenRequestHandler
            implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestAttributeHandler plain =
                new CsrfTokenRequestAttributeHandler();

        private final XorCsrfTokenRequestAttributeHandler xor =
                new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<org.springframework.security.web.csrf.CsrfToken> csrfToken) {

            xor.handle(request, response, csrfToken);

            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(
                HttpServletRequest request,
                org.springframework.security.web.csrf.CsrfToken csrfToken) {

            String headerValue =
                    request.getHeader(csrfToken.getHeaderName());

            return StringUtils.hasText(headerValue)
                    ? plain.resolveCsrfTokenValue(
                            request,
                            csrfToken)
                    : xor.resolveCsrfTokenValue(
                            request,
                            csrfToken);
        }
    }
}