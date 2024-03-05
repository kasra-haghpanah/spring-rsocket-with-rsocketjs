package com.spring.boot.spring5webapp.ddd.repositories.mongo.gridfs.service;


import com.mongodb.*;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.gridfs.GridFS;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.spring.boot.spring5webapp.ddd.repositories.mongo.gridfs.repository.FsChunkReactiveRepository;
import com.spring.boot.spring5webapp.ddd.repositories.mongo.gridfs.repository.FsFileReactiveRepository;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.bind.DatatypeConverter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class ReactiveMongoGridFsRepository {

    ReactiveMongoTemplate reactiveMongoTemplate;
    MongoCollection<Document> chunks;
    MongoCollection<Document> files;

    FsFileReactiveRepository fsFileReactiveRepository;
    FsChunkReactiveRepository fsChunkReactiveRepository;

    public ReactiveMongoGridFsRepository(
            ReactiveMongoTemplate reactiveMongoTemplate,
            FsFileReactiveRepository fsFileReactiveRepository,
            FsChunkReactiveRepository fsChunkReactiveRepository
    ) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
       // fsFileReactiveRepository.s


        reactiveMongoTemplate.collectionExists("fs.files")
                .subscribe(isExist -> {
                    if (!isExist.booleanValue()) {
                        reactiveMongoTemplate.createCollection("fs.files")
                                .subscribe(
                                        collection -> {
                                            files = collection;
                                            collection.createIndex(new Document("_id", 1));
                                        }
                                );
                    } else {
                        files = reactiveMongoTemplate.getCollection("fs.files");
                    }
                });

        reactiveMongoTemplate.collectionExists("fs.chunks")
                .subscribe(isExist -> {
                    if (!isExist.booleanValue()) {
                        reactiveMongoTemplate.createCollection("fs.chunks")
                                .subscribe(
                                        collection -> {
                                            chunks = collection;
                                            collection.createIndex(new Document("files_id", 1).append("n", 1));
                                        }
                                );
                    } else {
                        chunks = reactiveMongoTemplate.getCollection("fs.chunks");
                    }
                });

    }

    Mono<String> store(
            Flux<DataBuffer> content,
            @Nullable String filename,
            @Nullable int chunkSize,
            @Nullable int length,
            @Nullable String contentType,
            @Nullable Document metadata
    ) {

        return //reactiveMongoTemplate.save(fsFile)
                saveFile(null, filename, length, chunkSize, null, new Document("_contentType", contentType))
                        .flatMap(
                                document -> {
                                    String id = document.get("_id") + "";
                                    return content.reduce(
                                            new HashMap<String, Object>(), (map, dataBuffer) -> {

                                                DataBuffer dataBufferMap = (DataBuffer) map.get("dataBuffer");
                                                Integer counter = (Integer) map.get("counter");

                                                if (counter == null) {
                                                    counter = 0;
                                                }

                                                if (dataBufferMap == null) {
                                                    dataBufferMap = new DefaultDataBufferFactory(false, chunkSize).allocateBuffer();
                                                    map.put("dataBuffer", dataBufferMap);
                                                }

                                                dataBufferMap.write(dataBuffer);
                                                DataBufferUtils.release(dataBuffer);

                                                int readableByteCount = dataBufferMap.readableByteCount();

                                                if (readableByteCount == chunkSize) {

                                                    //FsChunck fsChunck = new FsChunck();
                                                    byte[] bytes = new byte[chunkSize];
                                                    dataBufferMap.read(bytes);
                                                    DataBufferUtils.release(dataBufferMap);

                                                    //fsChunck.setData(bytes);
                                                    //fsChunck.setFileId(file.getId());
                                                    //fsChunck.setN(++counter);

                                                    // reactiveMongoTemplate.save(fsChunck).subscribe();
                                                    saveChunck(null, id, ++counter, bytes);

                                                    dataBufferMap = new DefaultDataBufferFactory(false, chunkSize).allocateBuffer();
                                                    map.put("dataBuffer", dataBufferMap);
                                                    // dataBufferMap write to database
                                                }
                                                map.put("counter", counter);
                                                return map;
                                            }
                                    )
                                            .flatMap(
                                                    map -> {

                                                        DataBuffer dataBufferMap = (DataBuffer) map.get("dataBuffer");
                                                        Integer counter = (Integer) map.get("counter") + 1;
                                                        if (dataBufferMap != null && dataBufferMap.readableByteCount() > 0) {

                                                            Document fsChunck = new Document();
                                                            byte[] bytes = new byte[dataBufferMap.readableByteCount()];
                                                            dataBufferMap.read(bytes);
                                                            DataBufferUtils.release(dataBufferMap);

                                                            fsChunck.append("data", bytes);
                                                            fsChunck.append("files_id", id);
                                                            fsChunck.append("n", counter);

                                                            reactiveMongoTemplate.save(fsChunck).subscribe();
                                                        }
                                                        return Mono.just(id);
                                                    }


                                            );


                                }
                        );
    }


    public Mono<String> save(Flux<DataBuffer> content, String filename, int chunkSize, int length, String contentType) {
        return this.store(content, filename, chunkSize, length, contentType, null);
    }

    public Flux<DataBuffer> findChunckByFileId(String fileId) {
        // query(where("files_id").is(fileId);

        //  db.fs.chunks.find({ files_id: ObjectId('5ec03b6daadf9a501f8f335d')},  { data : 1 })
        // db.fs.chunks.find(
        String nativeQuery = String.format(" 'fs.chunks' :{ { files_id: ObjectId('%s')},  { data : 1 , _id : 0 } } ", fileId);
        String fId = String.format("ObjectId('%s')", fileId);
        // new BasicQuery(nativeQuery)


        Document document0 = new Document("fs.chunks", new Document("files_id", fId));
        return reactiveMongoTemplate.executeCommand(document0)
                .flatMapMany(
                        document -> {
                            //byte[] bytes = DatatypeConverter.parseBase64Binary(base64);
                            byte[] bytes = (byte[]) document.get("data");
                            DataBuffer dataBuffer = new DefaultDataBufferFactory(false, bytes.length).allocateBuffer();
                            dataBuffer.write(bytes);
                            return Mono.just(dataBuffer);
                        }
                );
    }

    public Flux<DataBuffer> findChunckByN(String fileId, int start, int end) {

/*        Criteria[] criterias = {
                Criteria.where("files_id").is(fileId),
                Criteria.where("n").gte(start),
                Criteria.where("n").lte(end)
        };

        Criteria criteria = new Criteria();
        criteria.andOperator(criterias);


        Query query = new Query();
        query(criteria)*/
        String nativeQuery = String.format("{ files_id: ObjectId('%s') , n:{$gte: %d } , n:{$lte: %d } },  { data : 1 }", fileId, start, end);
        return reactiveMongoTemplate.find(new BasicQuery(nativeQuery), String.class)
                .flatMap(
                        base64 -> {
                            byte[] bytes = DatatypeConverter.parseBase64Binary(base64);
                            DataBuffer dataBuffer = new DefaultDataBufferFactory(false, bytes.length).allocateBuffer();
                            dataBuffer.write(bytes);
                            return Mono.just(dataBuffer);
                        }
                );
    }


    public Mono<Document> saveFile(String id, String filename, int length, int chunkSize, LocalDateTime uploadDate, Document metadata) {

        //BasicDBObject
        Document document = new Document("filename", filename)
                .append("length", length)
                .append("chunkSize", chunkSize)
                .append("uploadDate", "ISODate()")//
                .append("filename", filename)
                .append("metadata", metadata);

        if (uploadDate == null) {
            document.append("uploadDate", LocalDateTime.now());
        } else {
            document.append("uploadDate", uploadDate);
        }


        if (id == null) {
            document.append("_id", "ObjectId( ObjectId.getTimestamp().toString() )");
        } else {
            document.append("_id", "ObjectId(\"" + id + "\")");
        }

        // "fs.files"
        // "fs.chunks"
        return reactiveMongoTemplate.save(document, "fs.files");

    }


    public Mono<Document> saveChunck(String id, String fileId, int n, byte[] data) {

        Document document = new Document("files_id", "ObjectId(" + fileId + ")")
                .append("n", n)
                .append("data", data);
        if (id == null) {
            document.append("_id", "ObjectId( ObjectId.getTimestamp().toString() )");
        } else {
            document.append("_id", "ObjectId(\"" + id + "\")");
        }

        return reactiveMongoTemplate.save(document, "fs.chunks");
    }


}
