package com.council.election.configuration.netty;

import com.council.election.configuration.property.Properties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.net.InetAddress;
import java.net.UnknownHostException;

//https://www.baeldung.com/spring-boot-reactor-netty
@DependsOn("properties")
@Configuration
public class NettyWebServerFactorySslCustomizer implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

    @Override
    public void customize(NettyReactiveWebServerFactory serverFactory) {
//        Ssl ssl = new Ssl();
//        ssl.setEnabled(true);
//        ssl.setKeyStore("classpath:sample.jks");
//        ssl.setKeyAlias("alias");
//        ssl.setKeyPassword("password");
//        ssl.setKeyStorePassword("secret");
//        Http2 http2 = new Http2();
//        http2.setEnabled(false);
//        serverFactory.addServerCustomizers(new SslServerCustomizer(ssl, http2, null));
        serverFactory.setPort(Properties.getServerPort());

        InetAddress address = null;
        try {
            address = InetAddress.getByName(Properties.getServerHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        serverFactory.setAddress(address);

        ((ConfigurableServletWebServerFactory) serverFactory).setContextPath(Properties.getContextPath());


    }
}
