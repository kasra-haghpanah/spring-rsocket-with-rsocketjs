package com.council.election.configuration.rsocket;

import com.council.election.configuration.property.Properties;
import com.council.election.configuration.webflux.security.config.JwtConfig;
import com.council.election.ddd.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.*;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import org.springframework.security.rsocket.metadata.*;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kasra.haghpanah on 12/9/2019.
 */
@DependsOn("properties")
@Configuration(/*proxyBeanMethods = false*/)
@EnableRSocketSecurity
public class RSocketConfig {

    public static final String MIME_FILE_EXTENSION   = "message/x.upload.file.extension";
    public static final String MIME_FILE_NAME        = "message/x.upload.file.name";
    public static final String FILE_NAME = "file-name";
    public static final String FILE_EXTN = "file-extn";

    // for broadcasting in rsocket cloud gateway you can use redis for store user's session as a live object
    public static final Map<String, Queue<RSocketRequester>> connectedClients = new ConcurrentHashMap<String, Queue<RSocketRequester>>();
    final Object host;
    final Object port;
    final String path;
    final JwtReactiveAuthenticationManager jwtReactiveAuthenticationManager;
    private final UserService userService;

    public static Map<String, Queue<RSocketRequester>> getConnectedClients() {
        return connectedClients;
    }

    public RSocketConfig(
            JwtReactiveAuthenticationManager jwtReactiveAuthenticationManager,
            UserService userService
    ) {
        this.host = Properties.getServerHost();
        Integer portInteger = Properties.getServerPort();
        this.port = portInteger < 1 ? "8080" : portInteger + "";
        this.path = Properties.getRsocketServerMappingPath();
        this.jwtReactiveAuthenticationManager = jwtReactiveAuthenticationManager;
        this.userService = userService;
    }

    @Bean("rSocketRequester")//this rSocketRequester has never been used this project, it usually uses for rsocket cloud getway after receive request from gateway
    public Mono<RSocketRequester> rSocketRequester() {
        return userService.findByUsername("9113394969")
                .map(
                        userDetails -> {
                            return RSocketRequester.builder()
                                    .rsocketStrategies(rSocketStrategies())
                                    .rsocketConnector(connector -> {
                                        connector.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2)));
                                    })
                                    // .setupMetadata(new UsernamePasswordMetadata("kasra_khpk1985@yahoo.com", "123"), UsernamePasswordMetadata.BASIC_AUTHENTICATION_MIME_TYPE)
                                    .setupMetadata(JwtConfig.encoder(userDetails), BearerTokenMetadata.BEARER_AUTHENTICATION_MIME_TYPE)
                                    .setupData(JwtConfig.encoder(userDetails))
                                    .websocket(URI.create(String.format("ws://%s:%s%s", host, port, path)));
                        }
                );
    }

    @Bean
// for active Authentication principal
    RSocketMessageHandler messageHandler() {
        RSocketMessageHandler rSocketMessageHandler = new RSocketMessageHandler();
        rSocketMessageHandler.getArgumentResolverConfigurer().addCustomResolver(new AuthenticationPrincipalArgumentResolver());
        rSocketMessageHandler.setRSocketStrategies(rSocketStrategies());
        return rSocketMessageHandler;
    }

    @Bean// for encoder and decoder from dataBuffer
    public RSocketStrategies rSocketStrategies() {

        return RSocketStrategies.builder()
                .encoders(encoders -> {
                    encoders.add(new JsonEncoder());
                    encoders.add(new DataBufferEncoder());
                    // encoders.add(new Jackson2CborEncoder());
                    // encoders.add(new Jackson2JsonEncoder());
                    //encoders.add(new BearerTokenAuthenticationEncoder());
                    //encoders.add(new BasicAuthenticationEncoder());
                })
                .decoders(decoders -> {
                    decoders.add(new JsonDecoder());
                    decoders.add(new DataBufferDecoder());
                    //decoders.add(new Jackson2CborDecoder());
                    //decoders.add(new Jackson2JsonDecoder());
                    //decoders.add(new BearerTokenDecoder());
                    //decoders.add(new BasicAuthenticationDecoder());
                })
                .metadataExtractorRegistry(metadataExtractorRegistry -> {
                    metadataExtractorRegistry.metadataToExtract(MimeType.valueOf(MIME_FILE_EXTENSION), String.class, FILE_EXTN);
                    metadataExtractorRegistry.metadataToExtract(MimeType.valueOf(MIME_FILE_NAME), String.class, FILE_NAME);

                })
                .build();
    }

    public static class JsonDecoder extends AbstractJackson2Decoder {
        public JsonDecoder() {
            this(Jackson2ObjectMapperBuilder.json().build(), MediaType.APPLICATION_OCTET_STREAM);
        }

        public JsonDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
            super(mapper, mimeTypes);
            //Assert.isAssignable(Message.class, mapper.getFactory().getClass());
        }

        public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
            throw new UnsupportedOperationException("Does not support stream decoding yet");
        }
    }

    public static class JsonEncoder extends AbstractJackson2Encoder {
        public JsonEncoder() {
            this(Jackson2ObjectMapperBuilder.json().build(), MediaType.ALL);
        }

        public JsonEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
            super(mapper, mimeTypes);
            //Assert.isAssignable(CBORFactory.class, mapper.getFactory().getClass());
        }

        public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
            throw new UnsupportedOperationException("Does not support stream encoding yet");
        }
    }


    @Bean//for rsocket jwt security
    public PayloadSocketAcceptorInterceptor rsocketInterceptor(RSocketSecurity rsocketSecurity) {

        return rsocketSecurity.authorizePayload(authorize -> {
            authorize
                    .setup()
                    .access((mono, context) -> mono
                            .map(auth -> auth.getAuthorities().stream()
                                    .filter(e -> {
                                        return e.getAuthority().equals("ROLE_USER") || e.getAuthority().equals("ROLE_ADMIN");
                                    })
                                    .count() > 0)
                            .map(AuthorizationDecision::new)
                    )
                    .route("forgot")
                    .hasAnyRole("ADMIN", "USER")
                    .route("getUsers.{mobile}")
                    .hasAnyRole("ADMIN", "USER")
                    .route("getChildsByParentId.{parentId}")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER")
                    .route("add.user")
                    .hasAnyRole("ADMIN", "USER")
                    .route("edit.user")
                    .hasAnyRole("ADMIN", "USER")
                    .route("upload.{title}.{text}.{filename}.{contentType}.{size}.{duration}")
                    .hasAnyRole("ADMIN")
                    .route("delete")
                    .hasAnyRole("ADMIN")
                    .route("allMessages")
                    .hasAnyRole("ADMIN", "USER")
                    .route("role.{phone}")
                    .hasAnyRole("ADMIN")
                    .route("edit.user.metadata")
                    .hasAnyRole("ADMIN", "USER")
                    .route("delete.getUsersBy.{id}")
                    .access((authentication, metaData) -> {
                        metaData.getExchange().getDataMimeType();
                        return authentication
                                .map(auth -> auth.getAuthorities().stream()
                                        .filter(e -> e.getAuthority().equals("ROLE_USER") || e.getAuthority().equals("ROLE_ADMIN"))
                                        .count() > 0)
                                .map(AuthorizationDecision::new);
                    })
                    //.hasAnyRole("ADMIN", "USER")
                    //.permitAll()
                    .anyRequest().permitAll()
                    .anyExchange().authenticated();
        })
                .jwt(jwtSpec -> {
                    try {
                        jwtSpec.authenticationManager(jwtReactiveAuthenticationManager);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                // .basicAuthentication(Customizer.withDefaults())
                .build();
    }



/*    @Bean
    ApplicationListener<ApplicationReadyEvent> ready(RSocketStrategies rsocketStrategies) {

        UsernamePasswordMetadata credentials = new UsernamePasswordMetadata("kasra_khpk1985@yahoo.com", "123");

        return event -> {

            rSocketRequester(rsocketStrategies)
                    .flatMapMany(
                            requester -> {
                                return requester
                                        .route("requestStream.{fname}.{lname}", "kasra", "haghpanah")
                                        .metadata(credentials, MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString()))
                                        .data(new Message("kk", "mm"))
                                        .retrieveFlux(Message.class)
                                        .flatMap(
                                                message -> {
                                                    System.out.println(message);
                                                    return Mono.empty();
                                                }
                                        );
                            }
                    );

        };
    }*/


}
