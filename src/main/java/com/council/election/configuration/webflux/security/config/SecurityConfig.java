package com.council.election.configuration.webflux.security.config;

import com.council.election.configuration.exception.HttpException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@DependsOn("jacksonConfig")
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig implements ServerSecurityContextRepository {

    private final JwtReactiveAuthenticationManager jwtReactiveAuthenticationManager;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            ObjectMapper objectMapper,
            JwtReactiveAuthenticationManager jwtReactiveAuthenticationManager
    ) {
        this.objectMapper = objectMapper;
        this.jwtReactiveAuthenticationManager = jwtReactiveAuthenticationManager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        //return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new BCryptPasswordEncoder();
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext sc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String token = null;
        if (request.getCookies().get("Cookie") != null) {
            token = request.getCookies().get("Cookie").get(0).getValue();
        }

        if (token != null && !token.equals("")) {
            Authentication auth = new BearerTokenAuthenticationToken(token);  //new UsernamePasswordAuthenticationToken(authToken, authToken);

            return this.jwtReactiveAuthenticationManager.authenticate(auth).map((authentication) -> {
                return new SecurityContextImpl(authentication);
            });

        } else {
            return Mono.empty();
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://172.16.110.235:8003", "http://172.16.106.30:8093"));
        configuration.setAllowedMethods(Arrays.asList("GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("Content-Type, api_key, Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityWebFilterChain securitygWebFilterChain(ServerHttpSecurity http) {
        return http
                .exceptionHandling()
                .authenticationEntryPoint((exchange, authenticationException) -> {
                    return Mono.fromRunnable(() -> {
                        throw new HttpException(HttpStatus.UNAUTHORIZED.toString(), HttpStatus.UNAUTHORIZED);
                    });
                }).accessDeniedHandler((exchange, authenticationException) -> {
                    return Mono.fromRunnable(() -> {
                        throw new HttpException(HttpStatus.UNAUTHORIZED.toString(), HttpStatus.FORBIDDEN);
                    });
                }).and()
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
/*
                .logout(logoutSpec -> {
                    logoutSpec.logoutUrl("/logout");
                    logoutSpec.requiresLogout(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/logout"));
                    logoutSpec.logoutSuccessHandler(new RedirectServerLogoutSuccessHandler())
                   // logoutSpec.logoutSuccessUrl
                    //logoutSpec.and().logout();
                    //logoutSpec.logoutSuccessHandler().logoutSuccessUrl("/afterlogout.html")
                    new HttpStatusReturningServerLogoutSuccessHandler().onLogoutSuccess()
                    logoutSpec.logoutSuccessHandler(new HttpStatusReturningServerLogoutSuccessHandler());
                    logoutSpec.logoutHandler(new SecurityContextServerLogoutHandler());

                })
               // .logoutSuccessUrl("/")
               // .invalidateHttpSession(true)
               // .deleteCookies("Cookie")
*/

                .cors()
                .configurationSource(corsConfigurationSource())
                .and()
                .authenticationManager(this.jwtReactiveAuthenticationManager)
                .securityContextRepository(this)
                .authorizeExchange()
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers(
                        "/v3/api-docs/**",
                        "/api-docs/**",
                        "/webjars/**",
                        "/cookie",
                        "/change/**",
                        "/signin",
                        "/signup",
                        "/forgot/**",
                        "/inherit/**"
                )
                .permitAll()

                .pathMatchers(HttpMethod.GET,
                        "/js/**", "/lib/**", "/css/**", "/fonts/**", "/view/**", "/",
                        "/favicon.ico", "/css/**", "/custom/**",
                        "/fonts/**", "/images/**", // "/activate/**",
                        "/logout", "/active/**"
                ).permitAll()
                .pathMatchers(HttpMethod.PUT, "/signup", "/signin").permitAll()
                //.pathMatchers(HttpMethod.GET, "/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_USER")
                .pathMatchers("/**")
                .access((mono, context) -> {
                    //context.getExchange().getRequest().getCookies().toSingleValueMap();
                    return mono
                            .map(auth -> auth.getAuthorities().stream()
                                    .filter(e -> {
                                        return e.getAuthority().equals("ROLE_ADMIN");
                                    })
                                    .count() > 0)
                            .map(AuthorizationDecision::new);
                })
                .anyExchange().authenticated()

                .and().build();
    }


}
