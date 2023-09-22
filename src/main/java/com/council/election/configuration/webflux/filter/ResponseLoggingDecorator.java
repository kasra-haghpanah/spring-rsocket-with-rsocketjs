package com.council.election.configuration.webflux.filter;

import com.council.election.configuration.log.Log;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

public class ResponseLoggingDecorator extends ServerHttpResponseDecorator {

    //private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingDecorator.class);

    private long startTime;
    private boolean logHeaders;

    private final Log log;

    public ResponseLoggingDecorator(ServerHttpResponse delegate, Log log, long startTime, boolean logHeaders) {
        super(delegate);
        this.log = log;
        this.startTime = startTime;
        this.logHeaders = logHeaders;
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        Flux<DataBuffer> buffer = Flux.from(body);
        return super.writeWith(buffer.doOnNext(dataBuffer -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                Channels.newChannel(baos).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    String response = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    log.setResponseBody(response);
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
    }
}
