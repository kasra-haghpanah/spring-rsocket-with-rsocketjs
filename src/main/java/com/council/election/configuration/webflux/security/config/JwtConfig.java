package com.council.election.configuration.webflux.security.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.council.election.configuration.property.Properties;
import com.council.election.ddd.model.User;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@DependsOn("properties")
@Configuration
public class JwtConfig {

    private static Long expirationTime;
    private static String secret;

    public JwtConfig() {
        expirationTime = Properties.getJwtExpirationTime();
        secret = Properties.getJwtSecret();
    }


    public static final Mono<Jwt> decoder(String token) {
        Mac mac = null;

//        if (token.indexOf("bearer ") < 0) {
//            token = "bearer " + token;
//        }
        try {
            mac = Mac.getInstance(SignatureAlgorithm.HS512.getJcaName());
        } catch (NoSuchAlgorithmException e) {
            //e.printStackTrace();
            return Mono.empty();
        }
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), mac.getAlgorithm());
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS512).build().decode(token);
    }


    public static final String encoder(UserDetails userDetails) {
        Algorithm algorithm = Algorithm.HMAC512(secret);

        List<String> roles = new ArrayList<String>();
        ((User) userDetails).getRoles().forEach(role -> roles.add(role));

        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                //.withIssuer("hello-service-demo")
                //.withAudience("hello-service")
                .withIssuedAt(Date.from(Instant.now()))
                .withSubject(userDetails.getUsername())
                .withExpiresAt(Date.from(Instant.now().plus(expirationTime, ChronoUnit.MINUTES)))
                .withClaim("scope", roles)
                .sign(algorithm);
    }

    @Bean
    public JwtReactiveAuthenticationManager jwtReactiveAuthenticationManager() {
        JwtReactiveAuthenticationManager jwtReactiveAuthenticationManager = new JwtReactiveAuthenticationManager(JwtConfig::decoder);
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("scope");
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        jwtReactiveAuthenticationManager.setJwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(authenticationConverter));
        return jwtReactiveAuthenticationManager;
    }

}
