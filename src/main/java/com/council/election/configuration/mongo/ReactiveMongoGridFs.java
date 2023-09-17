package com.council.election.configuration.mongo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
//import io.swagger.models.auth.In;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactiveMongoGridFs {

    static final String collectionFsFilesName = "fs.files";
    static final String collectionFsChunksName = "fs.chunks";
    static final int chunkLength = 261120;//255 * 1024;
    final ReactiveNativeMongo reactiveNativeMongo;
    final ObjectMapper objectMapper;

    public ReactiveMongoGridFs(ReactiveNativeMongo reactiveNativeMongo, ObjectMapper objectMapper) {
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        this.objectMapper = objectMapper;
        this.reactiveNativeMongo = reactiveNativeMongo;
        this.reactiveNativeMongo.createIndex(collectionFsFilesName, Indexes.compoundIndex(Indexes.ascending("filename"), Indexes.ascending("uploadDate")), false).subscribe();
        this.reactiveNativeMongo.createIndex(collectionFsChunksName, Indexes.compoundIndex(Indexes.ascending("files_id"), Indexes.ascending("n")), true).subscribe();
    }

    private Flux<DeleteResult> deleteFsFileById(String fileId) {
        return reactiveNativeMongo.deleteMany(collectionFsFilesName, new Document("_id", new ObjectId(fileId)));
    }

    private Flux<DeleteResult> deleteFsChunksByFileId(String fileId) {
        return reactiveNativeMongo.deleteMany(collectionFsChunksName, new Document("files_id", new ObjectId(fileId)));
    }

    public Mono<Document> getFsFileById(String fileId) {
        String nativeQuery = String.format("{\"_id\":ObjectId(\"%s\")}", fileId);
        return reactiveNativeMongo.findOne(collectionFsFilesName, nativeQuery);
    }

    public Mono<Document> insertFsFile(
            String filename,
            Integer chunkSize,
            LocalDateTime uploadDate,
            Integer length,
            Document metadata
    ) {
        if (chunkSize == null) {
            chunkSize = this.chunkLength;
        }

        Document document = new Document();
        document.append("filename", filename);
        document.append("length", length);
        document.append("chunkSize", chunkSize);
        document.append("uploadDate", uploadDate);
        if (metadata != null) {
            document.append("metadata", metadata);
        }
        return reactiveNativeMongo.insert(collectionFsFilesName, document);

    }

    public Flux<UpdateResult> updateFsFile(Document document) {
        return reactiveNativeMongo.updateOne(collectionFsFilesName, new Document("_id", document.getObjectId("_id")), document);
    }

    public Flux<UpdateResult> updateFsFileJustLength(String fileId, int length) {
        return reactiveNativeMongo.updateOne(collectionFsFilesName, new Document("_id", new ObjectId(fileId)), new Document("length", length));
    }

    public Mono<Document> saveFsChunk(ObjectId fileId, int n, Integer second, byte[] data) {
        Document document = new Document();
        document.append("files_id", fileId);
        document.append("n", n);
        if (second != null) {
            document.append("second", second);
        }
        Binary binary = new Binary(data);
        document.append("data", binary);
        return reactiveNativeMongo.insert(collectionFsChunksName, document);
    }

    //https://www.vinsguru.com/rsocket-file-upload-example/
    Mono<Document> store(
            Flux<DataBuffer> content,
            @Nullable final String filename,
            @Nullable final int chunkSize,
            @Nullable final LocalDateTime uploadDate,
            @Nullable final int length,
            @Nullable final Document metadata,
            @Nullable final Integer second

    ) {

        final DataBuffer dataBufferMap = new DefaultDataBufferFactory(true, 2 * chunkSize).allocateBuffer();

        return insertFsFile(filename, chunkSize, uploadDate, length, metadata)
                .flatMap(
                        file -> {
                            return content.onBackpressureBuffer()/*.onBackpressureDrop()*/.reduce(
                                    new Integer(0), (counter, dataBuffer) -> {

                                        dataBufferMap.write(dataBuffer);
                                        DataBufferUtils.release(dataBuffer);

                                        int readableByteCount = dataBufferMap.readableByteCount();

                                        if (readableByteCount >= chunkSize) {

                                            byte[] bytes = new byte[chunkSize];
                                            dataBufferMap.read(bytes);
                                            saveFsChunk(file.getObjectId("_id"), counter, null, bytes).subscribe();
                                            return counter + 1;
                                        }

                                        return counter;
                                    }
                            )
                                    .flatMap(
                                            counter -> {

                                                int readableByteCount = dataBufferMap.readableByteCount();

                                                if (dataBufferMap.readableByteCount() > 0) {

                                                    byte[] bytes = new byte[readableByteCount];
                                                    dataBufferMap.read(bytes);
                                                    DataBufferUtils.release(dataBufferMap);
                                                    saveFsChunk(file.getObjectId("_id"), counter, null, bytes).subscribe();
                                                } else {
                                                    DataBufferUtils.release(dataBufferMap);
                                                }

                                                int sizeFile = (counter + 1) * chunkSize + readableByteCount;
                                                if (file.getInteger("length") != sizeFile) {
                                                    file.put("length", sizeFile);
                                                    updateFsFile(file).subscribe();
                                                }
                                                return Mono.just(file);
                                            }

                                    );
                        }
                );
    }

    public Mono<Integer> saveChunckBySecond(
            final String fileId,
            @Nullable final Integer second,
            Flux<DataBuffer> content
    ) {
        return saveChunckBySecond(fileId, second, this.chunkLength, content);
    }

    public Mono<Integer> saveChunckBySecond(
            final String fileId,
            @Nullable final Integer second,
            @Nullable final Integer chunkSize,
            Flux<DataBuffer> content
    ) {
        final DataBuffer dataBufferMap = new DefaultDataBufferFactory(true, 2 * chunkSize).allocateBuffer();
        return content.reduce(
                new Integer(0), (counter, dataBuffer) -> {
                    dataBufferMap.write(dataBuffer);
                    DataBufferUtils.release(dataBuffer);
                    int readableByteCount = dataBufferMap.readableByteCount();
                    if (readableByteCount >= chunkSize) {
                        byte[] bytes = new byte[chunkSize];
                        dataBufferMap.read(bytes);
                        saveFsChunk(new ObjectId(fileId), counter, null, bytes).subscribe();
                        return counter + 1;
                    }
                    return counter;
                }
        )
                .flatMap(
                        counter -> {
                            int readableByteCount = dataBufferMap.readableByteCount();

                            if (dataBufferMap.readableByteCount() > 0) {
                                byte[] bytes = new byte[readableByteCount];
                                dataBufferMap.read(bytes);
                                saveFsChunk(new ObjectId(fileId), counter, null, bytes).subscribe();
                            }
                            DataBufferUtils.release(dataBufferMap);
                            return Mono.just(second);
                        }
                );
    }

    public Mono<Document> save(Flux<DataBuffer> content, String filename, String contentType, Integer second) {
        return this.store(content, filename, chunkLength, LocalDateTime.now(), 0, null, second);
    }

    public Mono<Document> save(FilePart filePart, String filename, String contentType, Integer second) {
        return this.store(filePart.content(), filename, chunkLength, LocalDateTime.now(), 0, null, second);
    }

    public Mono<Document> save(Flux<DataBuffer> content, String filename, int length, String contentType, Integer second) {
        return this.store(content, filename, chunkLength, LocalDateTime.now(), length, null, second);
    }

    public Mono<Document> save(Flux<DataBuffer> content, String filename, int length, String contentType, double duration, Document metadata, Integer second) {
        if (duration > 0 || contentType != null) {
            if (metadata == null) {
                metadata = new Document();
            }
        }
        if (duration > 0) {
            metadata.append("_duration", duration);
        }
        if (contentType != null) {
            metadata.append("_contentType", contentType);
        }
        return this.store(content, filename, chunkLength, LocalDateTime.now(), length, metadata, second);
    }

    public Mono<Document> save(FilePart filePart, String filename, int length, String contentType, double duration, Document metadata, Integer second) {
        return this.save(filePart.content(), filename, length, contentType, duration, metadata, second);
    }

    public Mono<Document> save(FilePart filePart, String filename, int length, String contentType, Integer second) {
        return this.store(filePart.content(), filename, chunkLength, LocalDateTime.now(), length, null, second);
    }

    public Mono<Document> save(Flux<DataBuffer> content, String filename, int chunkSize, int length, String contentType, Integer second) {
        return this.store(content, filename, chunkSize, LocalDateTime.now(), length, null, second);
    }

    public Mono<Document> save(FilePart filePart, String filename, int chunkSize, int length, String contentType, Integer second) {
        return this.store(filePart.content(), filename, chunkSize, LocalDateTime.now(), length, null, second);
    }

    public Flux<DataBuffer> getFileByFileId(String fileId) {

        return reactiveNativeMongo.find(collectionFsChunksName, String.format("{files_id: ObjectId('%s')}", fileId), "{_id:0, data:1}")
                .flatMap(fsChunck -> {
                    byte[] bytes = ((Binary) fsChunck.get("data")).getData();
                    DataBuffer dataBuffer = new DefaultDataBufferFactory(true, bytes.length).allocateBuffer();
                    dataBuffer.write(bytes);
                    return Mono.just(dataBuffer);
                });
    }

    public Mono<DataBuffer> getDataBufferJoinByFileId(String fileId) {

        return getFsFileById(fileId)
                .flatMap(fsFile -> {
                    return getFileByFileId(fsFile.getObjectId("_id").toHexString()).reduce(
                            new DefaultDataBufferFactory(true, fsFile.getInteger("length")).allocateBuffer(),
                            (allDataBuffer, dataBuffer) -> {
                                allDataBuffer.write(dataBuffer);
                                return allDataBuffer;
                            }
                    );
                });
    }

    public Flux<DataBuffer> getFileByFileIdByN(String fileId, int start, int end) {
        return getFileByFileIdByRange(fileId, start, end, "n");
    }

    public Flux<DataBuffer> getFileByFileIdBySecond(String fileId, int start, int end) {
        return getFileByFileIdByRange(fileId, start, end, "second");
    }

    private Flux<DataBuffer> getFileByFileIdByRange(String fileId, int start, int end, String type) {

        String findQuery = String.format("{ $and: [{files_id:ObjectId('%s')},{n: { $gte: %d }},{n: { $lte: %d }} ]}", fileId, start, end);
        return reactiveNativeMongo.find(collectionFsChunksName, findQuery, "{data:1,_id:0}")
                .flatMap(fsChunck -> {
                    byte[] bytes = ((Binary) fsChunck.get("data")).getData();
                    DataBuffer dataBuffer = new DefaultDataBufferFactory(true, bytes.length).allocateBuffer();
                    dataBuffer.write(bytes);
                    return Mono.just(dataBuffer);
                });
    }

    public Mono<Void> delete(String fileId) {
        deleteFsFileById(fileId).subscribe();
        deleteFsChunksByFileId(fileId).subscribe();
        return Mono.empty();
    }

    public Mono<DataBuffer> join(Flux<DataBuffer> dataBufferFlux, int sizeFile) {
        final DataBuffer buffer = new DefaultDataBufferFactory(true, sizeFile).allocateBuffer();
        return dataBufferFlux
                .reduce(
                        buffer, (allBuffer, dataBuffer) -> {
                            allBuffer.write(dataBuffer);
                            return allBuffer;
                        }
                );
    }

    public Flux<Document> findAll() {
        return reactiveNativeMongo.find(collectionFsFilesName, "{}");
    }

    public Flux<Map<String, Object>> findAllDetails() {
        return findAll()
                .flatMap(
                        fsFile -> {

                            final DataBuffer buffer = new DefaultDataBufferFactory(true, fsFile.getInteger("length")).allocateBuffer();
                            return this.getFileByFileId(fsFile.getObjectId("_id").toHexString())
                                    .reduce(
                                            buffer, (allBuffer, dataBuffer) -> {
                                                allBuffer.write(dataBuffer);
                                                return allBuffer;
                                            }
                                    )
                                    .flatMap(
                                            allBuffer -> {
                                                byte[] bytes = new byte[allBuffer.readableByteCount()];
                                                allBuffer.read(bytes);
                                                DataBufferUtils.release(allBuffer);

                                                Map<String, Object> file = new HashMap<String, Object>();
                                                file.put("id", fsFile.getObjectId("_id").toHexString());
                                                file.put("content", bytes);
                                                file.put("contentType", fsFile.get("metadata", Document.class).get("_contentType").toString());

                                                Document metadata = fsFile.get("metadata", Document.class);
                                                if (metadata != null && metadata.get("_duration") != null) {
                                                    file.put("duration", fsFile.get("metadata", Document.class).getDouble("_duration"));
                                                }
                                                file.put("name", fsFile.getString("filename"));
                                                file.put("size", fsFile.getInteger("length"));
                                                return Mono.just(file);
                                            }
                                    );
                        }
                );
    }

}