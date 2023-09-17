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

        String nativeQuery = "[\n" +
                "    {\n" +
                "        \"$lookup\": {\n" +
                "            \"from\": \"user\",\n" +
                "            \"let\": {\"authorPhone\": \"$authorPhone\", \"fileId\": \"$fileId\"},\n" +
                "            \"pipeline\": [\n" +
                "                {\"$match\": {\"$expr\": {\"$eq\": [\"$username\", \"$$authorPhone\"]}}},\n" +
                "                {\n" +
                "                    \"$lookup\": {\n" +
                "                        \"from\": \"fs.files\",\n" +
                "                        \"let\": {\"id\": \"$_id\"},\n" +
                "                        \"pipeline\": [\n" +
                "                            {\"$match\": {\"$expr\": {\"$eq\": [\"$$fileId\", \"$_id\"]}}}\n" +
                "                        ],\n" +
                "                        \"as\": \"file\"\n" +
                "                    }\n" +
                "                },\n" +
                "                {\n" +
                "                    \"$unwind\":\n" +
                "                        {\n" +
                "                            \"path\": \"$file\",\n" +
                "                            \"includeArrayIndex\": \"index\",\n" +
                "                            \"preserveNullAndEmptyArrays\": true  // if is true for left join\n" +
                "                        }\n" +
                "                },\n" +
                "                {\n" +
                "                    \"$project\": {\n" +
                "                        \"_id\": 0,\n" +
                //  "                        \"userId\": {\"$toString\": \"$_id\"},\n" +
                "                        \"firstName\": \"$first_name\",\n" +
                "                        \"lastName\": \"$last_name\",\n" +
                "                        \"fileId\": {\"$toString\":\"$file._id\"},\n" +
                "                        \"filename\": \"$file.filename\",\n" +
                "                        \"size\": \"$file.length\",\n" +
                "                        \"contentType\": \"$file.metadata._contentType\",\n" +
                "                        \"duration\": \"$file.metadata._duration\"\n" +
                "                    }\n" +
                "                }\n" +
                "            ],\n" +
                "            \"as\": \"user\"\n" +
                "        }\n" +
                "    },\n" +
                "\n" +
                "    {\n" +
                "        \"$unwind\":\n" +
                "            {\n" +
                "                \"path\": \"$user\",\n" +
                "                \"includeArrayIndex\": \"index\",\n" +
                "                \"preserveNullAndEmptyArrays\": true  // if is true for left join\n" +
                "            }\n" +
                "    },\n" +
                "    {\n" +
                "        \"$project\": {\n" +
                "            \"_id\": 0,\n" +
                "            \"id\": {\"$toString\": \"$_id\"},\n" +
                "            \"firstName\": \"$user.firstName\",\n" +
                "            \"lastName\": \"$user.lastName\",\n" +
                "            \"title\": \"$title\",\n" +
                "            \"text\": \"$text\",\n" +
                "            \"phone\": \"$authorPhone\",\n" +
                "            \"fileId\": \"$user.fileId\",\n" +
                "            \"filename\": \"$user.filename\",\n" +
                "            \"size\": \"$user.size\",\n" +
                "            \"contentType\": \"$user.contentType\",\n" +
                "            \"duration\": \"$user.duration\"\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "]";
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
