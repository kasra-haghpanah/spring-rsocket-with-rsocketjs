package com.council.election.configuration.mongo;

import com.council.election.configuration.property.Properties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReactiveNativeMongo {


    final ReactiveMongoTemplate reactiveMongoTemplate;
    final ObjectMapper objectMapper;
    final ReactiveMongoGridFs reactiveMongoGridFs;

    public ReactiveNativeMongo(
            ReactiveMongoTemplate reactiveMongoTemplate,
            ObjectMapper objectMapper
    ) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        this.objectMapper = objectMapper;
        this.reactiveMongoGridFs = new ReactiveMongoGridFs(this, objectMapper);
    }

    public ReactiveMongoTemplate getReactiveMongoTemplate() {
        return reactiveMongoTemplate;
    }

    public ReactiveMongoGridFs getReactiveMongoGridFs() {
        return reactiveMongoGridFs;
    }

    public static Mono<Void> createDatabase(String host, Integer port, String databaseName, String username, String password) {
        String connectionString = String.format("mongodb://%s:%d", host, port);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .retryWrites(true)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);

        String createUserCmd = "{" +
                "\"createUser\": \"%s\",\n" +
                "\"pwd\": \"%s\",\n" +
                "\"roles\": [ { \"role\": \"userAdminAnyDatabase\", \"db\": \"admin\" } ]\n" +
                "}";

        createUserCmd = String.format(createUserCmd, username, password);

        mongoClient.getDatabase(databaseName).runCommand(BasicDBObject.parse(createUserCmd))
                .subscribe(new Subscriber<Document>() {
                    @Override
                    public void onSubscribe(final Subscription s) {
                        s.request(10);  // <--- Data requested and the insertion will now occur
                    }

                    @Override
                    public void onNext(final Document success) {
                        System.out.println("Inserted");
                    }

                    @Override
                    public void onError(final Throwable t) {
                        //System.out.println("Failed");
                    }

                    @Override
                    public void onComplete() {
                        //System.out.println("Completed");
                    }
                });
        return Mono.empty();
    }


    public Flux<Document> listIndexes(String collectionName) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.listIndexes();
                });
    }

    public Flux<Void> dropIndex(String collectionName, Bson bson) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.dropIndex(bson);
                });
    }

    private Flux<String> createIndexCollection(String collectionName, Bson index, boolean isUnique) {
        IndexOptions indexOptions = new IndexOptions().unique(isUnique);
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.createIndex(index, indexOptions);
                });
    }

    public Mono<Boolean> createIndex(String collectionName, Bson index, boolean isUnique) {
        if (collectionName == null || collectionName.equals("") || index == null) {
            return Mono.just(false);
        }
        final List<Boolean> booleanList = Arrays.asList(false);
        final StringBuilder stringBuilder = new StringBuilder();
        BsonDocument indexMetadata = index.toBsonDocument(Document.class, null);
        int size = indexMetadata.keySet().size();
        int i = 1;
        for (String key : indexMetadata.keySet()) {
            stringBuilder.append(key);
            stringBuilder.append("_");
            stringBuilder.append(indexMetadata.get(key).asNumber().intValue() == 1 ? 1 : 0);
            if (i < size) {
                stringBuilder.append("_");
            }
            i++;
        }
        String nameOfIndex = stringBuilder.toString();

        return listIndexes(collectionName)
                .flatMap(indexs -> {
                    if (indexs.getString("name").equals(nameOfIndex)) {
                        booleanList.set(0, true);
                        return Mono.just(booleanList);
                    }
                    return Mono.just(booleanList);
                })
                .thenMany(Mono.just(booleanList))
                .flatMap(boolList -> {
                    if (!boolList.get(0)) {//isExistsIndex
                        return createIndexCollection(collectionName, index, isUnique)
                                .flatMap(newIndex -> {
                                    return Mono.just(true);
                                });
                    }
                    return Mono.just(false);
                })
                .reduce(new Boolean(false), (boolList, boolValue) -> {
                    return boolValue;
                })
                .flatMap(boolValue -> {
                    return Mono.just(boolValue);
                });
    }

    public Flux<DeleteResult> deleteOne(String collectionName, String nativeQuery) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    BasicDBObject object = BasicDBObject.parse(nativeQuery);
                    return collection.deleteOne(object);
                });
    }

    public Flux<DeleteResult> deleteOne(String collectionName, Document filter) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.deleteOne(filter);
                });
    }

    public Flux<DeleteResult> deleteMany(String collectionName, String nativeQuery) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    BasicDBObject object = BasicDBObject.parse(nativeQuery);
                    return collection.deleteMany(object);
                });
    }

    public Flux<DeleteResult> deleteMany(String collectionName, Document filter) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.deleteMany(filter);
                });
    }

    public Mono<UpdateResult> updateOne(String collectionName, String filter, Document updateFileds) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    BasicDBObject object = BasicDBObject.parse(filter);
                    return collection.updateOne(object, new Document("$set", updateFileds));
                }).reduce((a, b) -> {
                    return b;
                });
    }

    public Mono<UpdateResult> updateOne(String collectionName, String filter, String updateFileds) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    BasicDBObject object = BasicDBObject.parse(filter);
                    return collection.updateOne(object, new Document("$set", BasicDBObject.parse(updateFileds)));
                }).reduce((a, b) -> {
                    return b;
                });
    }

    public Mono<UpdateResult> updateOneUnset(String collectionName, String filter, Document unsetFileds) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    BasicDBObject object = BasicDBObject.parse(filter);
                    return collection.updateOne(object, new Document("$unset", unsetFileds));
                }).reduce((a, b) -> {
                    return b;
                });
    }

    public Flux<UpdateResult> updateOne(String collectionName, Document filter, Document updateFileds) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.updateOne(filter, new Document("$set", updateFileds));
                });
    }

    public Flux<UpdateResult> updateMany(String collectionName, String filter, Document updateFileds) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    BasicDBObject object = BasicDBObject.parse(filter);
                    return collection.updateMany(object, new Document("$set", updateFileds));
                });
    }

    public Flux<UpdateResult> updateMany(String collectionName, Document filter, Document updateFileds) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.updateMany(filter, new Document("$set", updateFileds));
                });
    }

    public Flux<InsertOneResult> insertOne(String collectionName, String nativeQuery) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    Document object = Document.parse(nativeQuery);
                    return collection.insertOne(object);
                });
    }

    public Flux<InsertOneResult> insertOne(String collectionName, Document document) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.insertOne(document);
                });
    }

    public Mono<Document> insert(String collectionName, Document document) {
        return reactiveMongoTemplate.insert(document, collectionName);
    }

    public Flux<InsertManyResult> insertMany(String collectionName, String nativeQuery) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    List<Document> object = castNativeQueryToBsonList(nativeQuery, new TypeReference<List<Map<String, Object>>>() {
                    });
                    return collection.insertMany(object);
                });
    }

    public Flux<InsertManyResult> insertMany(String collectionName, List<Document> documents) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.insertMany(documents);
                });
    }

    public Flux<Document> find(String collectionName, String nativeQuery) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    BasicDBObject object = BasicDBObject.parse(nativeQuery);
                    return collection.find(object);
                });
    }

    public Flux<Document> find(String collectionName, String nativeQuery, String fields) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.find(BasicDBObject.parse(nativeQuery)).projection(BasicDBObject.parse(fields));
                });
    }

    public Mono<Document> findOne(String collectionName, String nativeQuery) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.find(BasicDBObject.parse(nativeQuery)).first();
                })
                .reduce((a, b) -> {
                    return b;
                });
    }

    public Mono<Document> findOne(String collectionName, String nativeQuery, String fields) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.find(BasicDBObject.parse(nativeQuery)).projection(BasicDBObject.parse(fields)).first();
                })
                .reduce((a, b) -> {
                    return b;
                });
    }

    public Flux<Document> find(String collectionName, String nativeQuery, int limit) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.find(BasicDBObject.parse(nativeQuery)).limit(limit);
                });
    }

    public Flux<Document> find(String collectionName, String nativeQuery, String fields, int limit) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    return collection.find(BasicDBObject.parse(nativeQuery)).projection(BasicDBObject.parse(fields)).limit(limit);
                });
    }

    public Flux<Document> aggregate(String collectionName, String nativeQuery) {
        return reactiveMongoTemplate.getCollection(collectionName)
                .flatMapMany(collection -> {
                    List<Document> documents = castNativeQueryToBsonList(nativeQuery, new TypeReference<List<Map<String, Object>>>() {
                    });
                    return collection.aggregate(documents);
                });
    }

    public <T extends Map<String, Object>> List<Document> castNativeQueryToBsonList(String json, TypeReference typeReference) {
        final List<Document> documents = new ArrayList<Document>();
        List<T> objectList = null;
        try {
            objectList = (List<T>) objectMapper.readValue(json, typeReference);
            if (objectList == null) {
                return null;
            }
            objectList
                    .forEach(map -> {
                        Document document = new Document(map);
                        documents.add(document);
                    });

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } finally {
            return documents;
        }
    }

}
