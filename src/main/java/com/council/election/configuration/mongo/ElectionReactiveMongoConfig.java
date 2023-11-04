package com.council.election.configuration.mongo;

import com.council.election.configuration.property.Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.net.ConnectException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@DependsOn({"jacksonConfig"})
@Configuration
@EnableReactiveMongoRepositories(value = {
        "com.council.election.ddd.repository"
},
        reactiveMongoTemplateRef = "reactiveMongoTemplate"
)
public class ElectionReactiveMongoConfig {
    // https://docs.mongodb.com/manual/reference/connection-string/
    // https://docs.mongodb.com/manual/reference/connection-string/#urioption.authSource
    // https://docs.mongodb.com/drivers/reactive-streams/

    //***************************************************************
    // https://www.baeldung.com/spring-reinitialize-singleton-bean#:~:text=Reinitializing%20the%20Singleton%20Bean,the%20context%20reinitialize%20it%20automatically.
/*    public void reinitializeBean() {
        DefaultSingletonBeanRegistry registry = (DefaultSingletonBeanRegistry) applicationContext.getAutowireCapableBeanFactory();
        registry.destroySingleton("ConfigManager");
        registry.registerSingleton("ConfigManager", new ConfigManager(filePath));
    }*/
    //***************************************************************

    // mongod --replSet rs0 --port 27017 --bind_ip localhost --dbpath "C:/Program Files/MongoDB/Server/4.2/data/rs0-0"  --oplogSize 128
    @Retryable(include = {ConnectException.class},
            maxAttemptsExpression = "300000",
            backoff = @Backoff(
                    delay = 150000l,
                    delayExpression = "20000",
                    maxDelayExpression = "2",
                    multiplierExpression = "500000"
            ))
    @Bean("electionReactiveMongoDatabaseFactory")
    public ReactiveMongoDatabaseFactory reactiveElectionMongoDatabaseFactory() {
        //"mongodb://admin:pass@localhost:27017/election"

        String host = Properties.getMongoHost();
        Integer port = Properties.getMongoPort();
        String databaseName = Properties.getMongoDatabase();
        String username = Properties.getMongoUser();
        String password = Properties.getMongoPassword();

        String connectionString = String.format("mongodb://%s:%s@%s:%d/%s?w=majority&serverSelectionTimeoutMS=2000&connectTimeoutMS=2000&socketTimeoutMS=2000", username, password, host, port, databaseName);
        ReactiveNativeMongo.createDatabase(host, port, databaseName, username, password).block();


        //  MongoClientOptions options = new MongoClientOptions.Builder().socketKeepAlive(true).build();
        //   SocketSettings.builder().
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToSocketSettings(socket -> {
                    socket.connectTimeout(2, TimeUnit.SECONDS);
                    // socket.applySettings(SocketSettings.builder().);
                })
                .retryReads(true)
                .retryWrites(true)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);

        return new SimpleReactiveMongoDatabaseFactory(mongoClient, databaseName);
    }

    @Qualifier("electionTemplate")
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        ReactiveMongoTemplate reactiveMongoTemplate = new ReactiveMongoTemplate(reactiveElectionMongoDatabaseFactory());
        return reactiveMongoTemplate;
    }

    @Qualifier("electionTM")
    @Bean("electionTM")
    ReactiveMongoTransactionManager electionTM() {
        return new ReactiveMongoTransactionManager(reactiveElectionMongoDatabaseFactory());
    }

    @Bean
    @Qualifier("electionNativeTemplate")
    public ReactiveNativeMongo reactiveNativeMongo(ObjectMapper objectMapper) {
        return new ReactiveNativeMongo(reactiveMongoTemplate(), objectMapper);
    }

}
