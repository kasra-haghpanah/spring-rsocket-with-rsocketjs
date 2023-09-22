package com.council.election.configuration.webflux.filter;

import com.council.election.configuration.log.Log;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

public class RequestLoggingDecorator extends ServerHttpRequestDecorator {

    //private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingDecorator.class);
    private final Log log;

    public RequestLoggingDecorator(ServerHttpRequest delegate, Log log) {
        super(delegate);
        this.log = log;
    }

    @Override
    public Flux<DataBuffer> getBody() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        return super.getBody().doOnNext(dataBuffer -> {
            try {
                Channels.newChannel(baos).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    String body = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    log.setRequestBody(body);
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
