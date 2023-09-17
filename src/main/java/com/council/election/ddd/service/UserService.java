package com.council.election.ddd.service;

import com.council.election.configuration.property.Properties;
import com.council.election.ddd.dto.UserDTO;
import com.council.election.ddd.model.User;
import com.council.election.configuration.mongo.ReactiveNativeMongo;
import com.council.election.ddd.repository.UserMongoRepository;
import com.council.election.ddd.utility.Cookie;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Set;

@Service
public class UserService {

    final ReactiveNativeMongo reactiveNativeMongo;
    final UserMongoRepository userMongoRepository;
    static final String collectionUserName = "user";

    public UserService(
            @Qualifier("electionNativeTemplate") ReactiveNativeMongo reactiveNativeMongo,
            UserMongoRepository userMongoRepository
    ) {
        this.reactiveNativeMongo = reactiveNativeMongo;
        this.userMongoRepository = userMongoRepository;
        this.reactiveNativeMongo.createIndex(collectionUserName, Indexes.ascending("username"), true).subscribe();
    }

    public Mono<User> findByUsername(String username) {
        return userMongoRepository.findByUsername(username);
    }

    public Mono<Boolean> changePassword(String username, String code, String password) {

        String filter = String.format("{username:\"%s\",activationCode:\"%s\"}", username, code);

        Document unsetDocument = new Document();
        unsetDocument.append("activationCode", "");

        return reactiveNativeMongo.updateOneUnset(collectionUserName, filter, unsetDocument)
                .flatMap(updateResult -> {
                    if (updateResult.wasAcknowledged()) {
                        Document setDocument = new Document();
                        setDocument.append("password", new BCryptPasswordEncoder().encode(password));

                        return reactiveNativeMongo.updateOne(collectionUserName, String.format("{username:\"%s\"}", username), setDocument)
                                .flatMap(updateResult1 -> {
                                    return Mono.just(updateResult1.wasAcknowledged());
                                });
                    }
                    return Mono.just(updateResult.wasAcknowledged());
                });
    }

    public Mono<String> forgotPassword(String username) {
        final String code = Cookie.generateCode();
        Document document = new Document();
        document.append("activationCode", code);
        return reactiveNativeMongo.updateOne(collectionUserName, String.format("{username:\"%s\"}", username), document)
                .flatMap(updateResult -> {
                    if (updateResult.wasAcknowledged()) {
                        return Mono.just(code);
                    }
                    return Mono.just(null);
                });
    }


    public Mono<Boolean> changeRole(String username, String... roles) {
        String filter = String.format("{username: \"%s\"}", username);
        return reactiveNativeMongo.updateOne(collectionUserName, filter, new Document("roles", Arrays.asList(roles)))
                .flatMap(updateResult -> {
                    return Mono.just(updateResult.wasAcknowledged());
                });
    }

    public Mono<UserDTO> editUserMetadata(UserDTO userDTO) {
        if (
                userDTO == null ||
                        userDTO.getUsername() == null ||
                        userDTO.getUsername().equals("") ||
                        userDTO.getMetadata() == null ||
                        userDTO.getMetadata().keySet().size() == 0
        ) {
            return Mono.empty();
        }
        String filter = String.format("{username: \"%s\"}", userDTO.getUsername());
        Document metadata = new Document();
        for (String key : userDTO.getMetadata().keySet()) {
            metadata.append(key, userDTO.getMetadata().get(key));
        }

        return reactiveNativeMongo.updateOne(collectionUserName, filter, new Document("metadata", metadata))
                .flatMap(updateResult -> {
                    if (updateResult.wasAcknowledged()) {
                        return reactiveNativeMongo.findOne(collectionUserName, filter)
                                .flatMap(document -> {
                                    return Mono.just(UserDTO.getUserDTOFromDocument(document));
                                });
                    }
                    return Mono.empty();
                });

    }

    public Mono<User> findByUsernameAndActivationCode(String username, String activationCode) {
        return userMongoRepository.findByUsernameAndActivationCode(username, activationCode);
    }

    Mono<User> findByUsernameAndPassword(String username, String password) {
        return userMongoRepository.findByUsernameAndPassword(username, password);
    }

    public Mono<User> save(User user) {
//        Set set = Properties.get("adminUsers", Set.class);
//        if (!set.contains(user.getUsername())) {
//            user.setActivationCode("123");
//        }
        return userMongoRepository.save(user);
    }

    public Mono<User> findById(ObjectId id) {
        return userMongoRepository.findById(id);
    }

    public Flux<User> findAll() {
        return userMongoRepository.findAll();
    }

    public Flux<UserDTO> getRoots() {

        String nativeQuery = """
                [
                    {
                        "$lookup":
                            {
                                "from": "user",
                                "let": {"id": "$_id", "parentId": "$parent_id"},
                                "pipeline": [
                                    {
                                        "$match": {
                                            "$expr": {
                                                "$and":[
                                                        {"$eq": [{"$ifNull": ["$$parentId", "null"]}, "null"]},
                                                        {"$eq": ["$_id", "$$id"]}
                                                    ]
                                            }
                                        }
                                    },
                                    {
                                        "$lookup": {
                                            "from": "user",
                                            "let": {"path": "$path"},
                                            "pipeline": [
                                                {
                                                    "$match": {
                                                        "$expr": {
                                                            "$and": [
                                                                {"$ne": ["$path", "$$path"]},
                                                                {"$eq": [{"$indexOfBytes": ["$path", "$$path"]}, 0]}
                                                            ]
                                                        }
                                                    }
                                                }
                                            ],
                                            "as": "childs"
                                        }
                                    }
                                
                                
                                ],
                                "as": "userChild"
                            }
                    },
                    {
                        "$unwind":
                            {
                                "path": "$userChild",
                                // includeArrayIndex: <string>,
                                "preserveNullAndEmptyArrays": false
                            }
                    },
                    {
                        "$project": {
                            "_id": 0,
                            "id": {
                                "$toString": "$userChild._id"
                            },
                            "parent_id": {
                                "$toString": "$userChild.parent_id"
                            },
                            "first_name": "$userChild.first_name",
                            "last_name": "$userChild.last_name",
                            "username": "$userChild.username",
                            "active": {
                                "$cond": {
                                    "if": {"$eq": [{"$ifNull": ["$userChild.activationCode", "null"]}, "null"]},
                                    "then": true,
                                    "else": false
                                }
                            },
                            "code":{"$ifNull": ["$userChild.activationCode", ""]},
                            "path": "$userChild.path",
                            "roles": "$userChild.roles",
                            "childsCount": {"$size": "$userChild.childs"},
                            "activeChildsCount": {
                                "$size": {
                                    "$filter": {
                                        "input": "$userChild.childs",
                                        "as": "item",
                                        "cond": {
                                            "$eq": [{"$ifNull": ["$$item.activationCode", "null"]}, "null"]
                                
                                        }
                                    }
                                }
                            },
                            "leafNode": {
                                "$cond": {
                                    "if": {"$eq": [{"$size": "$userChild.childs"}, 0]},
                                    "then": true,
                                    "else": false
                                }
                            }
                        }
                    }
                ]
                """;
        nativeQuery = String.format(nativeQuery);
        return reactiveNativeMongo.aggregate(collectionUserName, nativeQuery)
                .flatMap(user -> {
                    return Mono.just(UserDTO.getUserDTOFromDocument(user));
                });
    }


    // @Query(value = "SELECT u.id, u.parent_id, u.first_name, u.last_name, u.username FROM user u WHERE u.parent_id =:parentId", nativeQuery = true)
    public Flux<UserDTO> getUsersByParentId(String parentId) {
        if (parentId == null || parentId.equals("null")) {
            return Flux.empty();
        }
        String nativeQuery = """
                [
                    {
                        "$lookup":
                            {
                                "from": "user",
                                "let": {"id": "$_id", "parentId": "$parent_id"},
                                "pipeline": [
                                    {
                                        "$match": {
                                            "$expr": {
                                                "$and":
                                                    [
                                                        {"$eq": ["$parent_id", "$$id"]},
                                                        {"$eq": ["$$id", {"$toObjectId": "%s"}]}
                                                    ]
                                            }
                                        }
                                    },
                                    {
                                        "$lookup": {
                                            "from": "user",
                                            "let": {"path": "$path"},
                                            "pipeline": [
                                                {
                                                    "$match": {
                                                        "$expr": {
                                                            "$and": [
                                                                {"$ne": ["$path", "$$path"]},
                                                                {"$eq": [{"$indexOfBytes": ["$path", "$$path"]}, 0]}
                                                            ]
                                                        }
                                                    }
                                                }
                                            ],
                                            "as": "childs"
                                        }
                                    }
                                
                                
                                ],
                                "as": "userChild"
                            }
                    },
                    {
                        "$unwind":
                            {
                                "path": "$userChild",
                                // includeArrayIndex: <string>,
                                "preserveNullAndEmptyArrays": false
                            }
                    },
                    {
                        "$project": {
                            "_id": 0,
                            "id": {
                                "$toString": "$userChild._id"
                            },
                            "parent_id": {
                                "$toString": "$userChild.parent_id"
                            },
                            "first_name": "$userChild.first_name",
                            "last_name": "$userChild.last_name",
                            "username": "$userChild.username",
                            "active": {
                                "$cond": {
                                    "if": {"$eq": [{"$ifNull": ["$userChild.activationCode", "null"]}, "null"]},
                                    "then": true,
                                    "else": false
                                }
                            },
                            "code":{"$ifNull": ["$userChild.activationCode", ""]},
                            "path": "$userChild.path",
                            "roles": "$userChild.roles",
                            "childsCount": {"$size": "$userChild.childs"},
                            "activeChildsCount": {
                                "$size": {
                                    "$filter": {
                                        "input": "$userChild.childs",
                                        "as": "item",
                                        "cond": {
                                            "$eq": [{"$ifNull": ["$$item.activationCode", "null"]}, "null"]
                                
                                        }
                                    }
                                }
                            },
                            "leafNode": {
                                "$cond": {
                                    "if": {"$eq": [{"$size": "$userChild.childs"}, 0]},
                                    "then": true,
                                    "else": false
                                }
                            }
                        }
                    }
                ]
                """;
        nativeQuery = String.format(nativeQuery, parentId);
        return reactiveNativeMongo.aggregate(collectionUserName, nativeQuery)
                .flatMap(user -> {
                    return Mono.just(UserDTO.getUserDTOFromDocument(user));
                });
    }

    //@Query(value = "DELETE FROM USER WHERE path LIKE CONCAT( (SELECT u2.path FROM USER u2 WHERE u2.id =:id ) , '/%') OR id=:id ", nativeQuery = true)
    public void deleteAllUsersById(String id) {

        if (id == null) {
            return;
        }
        userMongoRepository.findById(new ObjectId(id))
                .flatMapMany(user -> {
                    //String x = "{path: { $regex: /\\/6058ff7446582775c7f93d3b\\/605909aa46582775c7f93d3e\\/(.)*/ }}";
                    String path = user.getPath();
                    String nativeQuery = String.format("{\"path\": { \"$regex\": '%s(.)*' }}", path);
                    return reactiveNativeMongo.find(collectionUserName, nativeQuery);

                })
                .flatMap(document -> {
                    String nativeQuery = String.format("{_id:ObjectId(\"%s\")}", ((ObjectId) document.get("_id")).toHexString());
                    return reactiveNativeMongo.deleteOne(collectionUserName, nativeQuery);
                })
                .subscribe();

    }

    //@Query(value = "SELECT u.id, u.parent_id, u.first_name, u.last_name, u.username FROM user u WHERE u.username =:phone OR u.parent_id = (SELECT u2.id FROM user u2 WHERE u2.username =:phone )", nativeQuery = true)
    public Flux<UserDTO> getUsersByPhoneParentIdWithParent(String phone) {
        if (phone == null) {
            return Flux.empty();
        }
        String nativeQuery = """
                [
                    {
                        "$lookup":
                            {
                                "from": "user",
                                "let": {"id": "$_id", "username": "$username"},
                                "pipeline": [
                                    {
                                        "$match": {
                                            "$expr": {
                                                "$or": [
                                                    {
                                                        "$and":
                                                            [
                                                                {"$eq": ["$parent_id", "$$id"]},
                                                                {"$eq": ["$$username", "%s"]}
                                                            ]
                                                    },
                                                    {
                                                        "$and":
                                                            [
                                                                {"$eq": ["$_id", "$$id"]},
                                                                {"$eq": ["$$username", "%s"]}
                                                            ]
                                                    }
                                
                                                ]
                                            }
                                        }
                                
                                    },
                                    {
                                        "$lookup": {
                                            "from": "user",
                                            "let": {"id1": "$_id", "path": "$path"},
                                            "pipeline": [
                                                {
                                                    "$match": {
                                                        "$expr": {
                                                            "$and": [
                                                                {"$ne": ["$path", "$$path"]},
                                                                {"$eq": [{"$indexOfBytes": ["$path", "$$path"]}, 0]}
                                                            ]
                                                        }
                                                    }
                                                }
                                            ],
                                            "as": "childs"
                                        }
                                    }
                                
                                
                                ],
                                "as": "userChild"
                            }
                    },
                    {
                        "$unwind":
                            {
                                "path": "$userChild",
                                "preserveNullAndEmptyArrays": false
                            }
                    },
                    {
                        "$project": {
                            "_id": 0,
                            "id": {
                                "$toString": "$userChild._id"
                            },
                            "parent_id": {
                                "$toString": "$userChild.parent_id"
                            },
                            "first_name": "$userChild.first_name",
                            "last_name": "$userChild.last_name",
                            "username": "$userChild.username",
                            "active": {
                                "$cond": {
                                    "if": {"$eq": [{"$ifNull": ["$userChild.activationCode", "null"]}, "null"]},
                                    "then": true,
                                    "else": false
                                }
                            },
                            "code":{"$ifNull": ["$userChild.activationCode", ""]},
                            "path": "$userChild.path",
                            "roles": "$userChild.roles",
                            "childsCount": {"$size": "$userChild.childs"},
                            "activeChildsCount": {
                                "$size": {
                                    "$filter": {
                                        "input": "$userChild.childs",
                                        "as": "item",
                                        "cond": {
                                            "$eq": [{"$ifNull": ["$$item.activationCode", "null"]}, "null"]
                                
                                        }
                                    }
                                }
                            },
                            "leafNode": {
                                "$cond": {
                                    "if": {"$eq": [{"$size": "$userChild.childs"}, 0]},
                                    "then": true,
                                    "else": false
                                }
                            }
                        }
                    }
                ]
                """;
        nativeQuery = String.format(nativeQuery, phone, phone);
        return reactiveNativeMongo.aggregate(collectionUserName, nativeQuery)
                .flatMap(user -> {
                    return Mono.just(UserDTO.getUserDTOFromDocument(user));
                });
    }

    //@Query(value = "SELECT u.id, u.parent_id, u.first_name, u.last_name, u.username FROM user u WHERE u.parent_id = (SELECT u2.id FROM user u2 WHERE u2.username =:phone )", nativeQuery = true)
    public Flux<UserDTO> getUsersByPhoneParentId(String phone) {
        if (phone == null) {
            return Flux.empty();
        }
        String nativeQuery = """
                [
                    {
                        "$lookup":
                            {
                                "from": "user",
                                "let": {"id": "$_id", "username": "$username"},
                                "pipeline": [
                                    {
                                        "$match": {
                                            "$expr": {
                                                "$and":
                                                    [
                                                        {"$eq": ["$parent_id", "$$id"]},
                                                        {"$eq": ["$$username", "%s"]}
                                                    ]
                                            }
                                        }
                                    },
                                    {
                                        "$lookup": {
                                            "from": "user",
                                            "let": {"path": "$path"},
                                            "pipeline": [
                                                {
                                                    "$match": {
                                                        "$expr": {
                                                            "$and": [
                                                                {"$ne": ["$path", "$$path"]},
                                                                {"$eq": [{"$indexOfBytes": ["$path", "$$path"]}, 0]}
                                                            ]
                                                        }
                                                    }
                                                }
                                            ],
                                            "as": "childs"
                                        }
                                    }
                                
                                
                                ],
                                "as": "userChild"
                            }
                    },
                    {
                        "$unwind":
                            {
                                "path": "$userChild",
                                // includeArrayIndex: <string>,
                                "preserveNullAndEmptyArrays": false
                            }
                    },
                    {
                        "$project": {
                            "_id": 0,
                            "id": {
                                "$toString": "$userChild._id"
                            },
                            "parent_id": {
                                "$toString": "$userChild.parent_id"
                            },
                            "first_name": "$userChild.first_name",
                            "last_name": "$userChild.last_name",
                            "username": "$userChild.username",
                            "active": {
                                "$cond": {
                                    "if": {"$eq": [{"$ifNull": ["$userChild.activationCode", "null"]}, "null"]},
                                    "then": true,
                                    "else": false
                                }
                            },
                            "code":{"$ifNull": ["$userChild.activationCode", ""]},
                            "path": "$userChild.path",
                            "roles": "$userChild.roles",
                            "childsCount": {"$size": "$userChild.childs"},
                            "activeChildsCount": {
                                "$size": {
                                    "$filter": {
                                        "input": "$userChild.childs",
                                        "as": "item",
                                        "cond": {
                                            "$eq": [{"$ifNull": ["$$item.activationCode", "null"]}, "null"]
                                
                                        }
                                    }
                                }
                            },
                            "leafNode": {
                                "$cond": {
                                    "if": {"$eq": [{"$size": "$userChild.childs"}, 0]},
                                    "then": true,
                                    "else": false
                                }
                            }
                        }
                    }
                ]
                """;
        nativeQuery = String.format(nativeQuery, phone);
        return reactiveNativeMongo.aggregate(collectionUserName, nativeQuery)
                .flatMap(user -> {
                    return Mono.just(UserDTO.getUserDTOFromDocument(user));
                });
    }


}
