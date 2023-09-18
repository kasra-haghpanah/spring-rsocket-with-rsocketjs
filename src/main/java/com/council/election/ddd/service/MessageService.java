package com.council.election.ddd.service;

import com.council.election.ddd.dto.MessageDTO;
import com.council.election.ddd.model.Message;
import com.council.election.configuration.mongo.ReactiveNativeMongo;
import com.council.election.ddd.repository.MessageMongoRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MessageService {

    final ReactiveNativeMongo reactiveNativeMongo;
    final MessageMongoRepository messageMongoRepository;
    static final String collectionName = "message";

    public MessageService(
            @Qualifier("electionNativeTemplate") ReactiveNativeMongo reactiveNativeMongo,
            MessageMongoRepository messageMongoRepository
    ) {
        this.reactiveNativeMongo = reactiveNativeMongo;
        this.messageMongoRepository = messageMongoRepository;
    }

    public Mono<Message> findById(ObjectId id) {
        return messageMongoRepository.findById(id);
    }

    public Mono<Message> findById(String id) {
        return messageMongoRepository.findById(new ObjectId(id));
    }

    public Mono<Void> deleteById(ObjectId id) {
        return messageMongoRepository.deleteById(id);
    }

    public Mono<Void> deleteByIdAndFileId(String id, String fileId) {
        if (fileId != null && !fileId.equals("")) {
            this.reactiveNativeMongo.getReactiveMongoGridFs().delete(fileId).subscribe();
        }
        return messageMongoRepository.deleteById(new ObjectId(id));
    }

    public Mono<Void> deleteById(String id) {
        return messageMongoRepository.deleteById(new ObjectId(id));
    }

    public Flux<Message> findByUsername(String username) {
        return messageMongoRepository.findByAuthorPhone(username);
    }

    public Flux<Message> findAll() {
        return messageMongoRepository.findAll();
    }

    public Mono<Message> save(Message message) {
        return messageMongoRepository.save(message);
    }


    public Flux<MessageDTO> messageJoinUserAndFile() {

        String nativeQuery = """
                [
                    {
                        "$lookup": {
                            "from": "user",
                            "let": {"authorPhone": "$authorPhone", "fileId": "$fileId"},
                            "pipeline": [
                                {"$match": {"$expr": {"$eq": ["$username", "$$authorPhone"]}}},
                                {
                                    "$lookup": {
                                        "from": "fs.files",
                                        "let": {"id": "$_id"},
                                        "pipeline": [
                                            {"$match": {"$expr": {"$eq": ["$$fileId", "$_id"]}}}
                                        ],
                                        "as": "file"
                                    }
                                },
                                {
                                    "$unwind":
                                        {
                                            "path": "$file",
                                            "includeArrayIndex": "index",
                                            "preserveNullAndEmptyArrays": true  // if is true for left join
                                        }
                                },
                                {
                                    "$project": {
                                        "_id": 0,
                                        "firstName": "$first_name",
                                        "lastName": "$last_name",
                                        "fileId": {"$toString":"$file._id"},
                                        "filename": "$file.filename",
                                        "size": "$file.length",
                                        "contentType": "$file.metadata._contentType",
                                        "duration": "$file.metadata._duration"
                                    }
                                }
                            ],
                            "as": "user"
                        }
                    },
                                
                    {
                        "$unwind":
                            {
                                "path": "$user",
                                "includeArrayIndex": "index",
                                "preserveNullAndEmptyArrays": true  // if is true for left join
                            }
                    },
                    {
                        "$project": {
                            "_id": 0,
                            "id": {"$toString": "$_id"},
                            "firstName": "$user.firstName",
                            "lastName": "$user.lastName",
                            "title": "$title",
                            "text": "$text",
                            "phone": "$authorPhone",
                            "fileId": "$user.fileId",
                            "filename": "$user.filename",
                            "size": "$user.size",
                            "contentType": "$user.contentType",
                            "duration": "$user.duration"
                        }
                    }
                                
                ]
                """;
        return reactiveNativeMongo.aggregate("message", nativeQuery)
                .flatMap(document -> {
                    MessageDTO dto = new MessageDTO();
                    dto.setId(document.getString("id"));
                    dto.setFirstName(document.getString("firstName"));
                    dto.setLastName(document.getString("lastName"));
                    dto.setTitle(document.getString("title"));
                    dto.setText(document.getString("text"));
                    dto.setPhone(document.getString("phone"));
                    dto.setFileId(document.getString("fileId"));
                    dto.setFilename(document.getString("filename"));
                    dto.setContentType(document.getString("contentType"));
                    if (document.get("size") != null) {
                        dto.setSize(document.getInteger("size"));
                    }
                    if (document.get("duration") != null) {
                        dto.setDuration(document.getDouble("duration"));
                    }
                    return Mono.just(dto);
                });
    }


}
