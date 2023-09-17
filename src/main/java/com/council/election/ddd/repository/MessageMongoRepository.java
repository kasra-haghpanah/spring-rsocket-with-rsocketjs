package com.council.election.ddd.repository;


import com.council.election.ddd.model.Message;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository("messageMongoRepository")
public interface MessageMongoRepository extends ReactiveMongoRepository<Message, ObjectId> {

    Mono<Message> findById(ObjectId id);
    Flux<Message> findByAuthorPhone(String phone);


}
