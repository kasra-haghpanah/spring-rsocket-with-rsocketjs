package com.council.election.ddd.repository;


import com.council.election.ddd.model.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository("userMongoRepository")
public interface UserMongoRepository extends ReactiveMongoRepository<User, ObjectId> {

    Mono<User> findById(String id);

    Mono<User> findByUsername(String username);

    Mono<User> findByUsernameAndActivationCode(String username, String activationCode);

    Mono<User> findByUsernameAndPassword(String username, String password);

}
