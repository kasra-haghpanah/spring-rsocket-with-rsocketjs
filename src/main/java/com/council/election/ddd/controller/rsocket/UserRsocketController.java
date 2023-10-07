package com.council.election.ddd.controller.rsocket;

import com.council.election.configuration.exception.HttpException;
import com.council.election.configuration.mongo.ReactiveNativeMongo;
import com.council.election.configuration.property.Properties;
import com.council.election.configuration.rsocket.RSocketConfig;
import com.council.election.ddd.dto.MessageDTO;
import com.council.election.ddd.dto.UserDTO;
import com.council.election.ddd.model.Message;
import com.council.election.ddd.model.User;
import com.council.election.ddd.service.MessageService;
import com.council.election.ddd.service.UserService;
import com.council.election.ddd.utility.Cookie;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

@Controller
public class UserRsocketController {

    final Logger logger = Logger.getLogger(UserRsocketController.class.getName());

    final UserService userService;
    final ReactiveNativeMongo reactiveNativeMongo;
    final MessageService messageService;
    final boolean showConnectedClient = Properties.getShowConnectedClient();

    public UserRsocketController(
            UserService userService,
            MessageService messageService,
            @Qualifier("electionNativeTemplate") ReactiveNativeMongo reactiveNativeMongo
    ) {
        this.userService = userService;
        this.reactiveNativeMongo = reactiveNativeMongo;
        this.messageService = messageService;
    }

    @ConnectMapping
    public Mono<Void> handle(
            final RSocketRequester requester,
            @Headers Map<String, Object> map,
            @Payload String token,
            @AuthenticationPrincipal Mono<Jwt> jwtToken
    ) {
        return jwtToken
                .flatMap(
                        jwt -> {
                            Map<String, Object> claims = jwt.getClaims();
                            final String userName = claims.get("sub").toString();
                            List<String> scopes = (List<String>) jwt.getClaims().get("scope");
                            String[] rules = new String[scopes.size()];
                            for (int i = 0; i < scopes.size(); i++) {
                                rules[i] = scopes.get(i).toString();
                            }

                            requester
                                    .rsocket()
                                    .onClose()
                                    .doFirst(
                                            () -> {

                                                Queue<RSocketRequester> sessionsUser = RSocketConfig.getConnectedClients().get(userName);
                                                if (sessionsUser == null) {
                                                    sessionsUser = new ConcurrentLinkedQueue<RSocketRequester>();
                                                }
                                                sessionsUser.offer(requester);
                                                RSocketConfig.getConnectedClients().put(userName, sessionsUser);
                                                if (showConnectedClient) {
                                                    //System.out.println("Client Connect");
                                                    logger.info("Client Connect");
                                                }
                                            }
                                    )
                                    .doOnError(error -> {
                                        if (showConnectedClient) {
                                            //System.out.println("Client Closed!" + error.getMessage());
                                            logger.info("Client Closed!" + error.getMessage());
                                        }
                                    })
                                    .doFinally(rsRequester -> {

                                        Queue<RSocketRequester> sessionsUser = RSocketConfig.getConnectedClients().get(userName);
                                        if (sessionsUser != null) {
                                            sessionsUser.remove(requester);
                                            if (sessionsUser.size() == 0) {
                                                sessionsUser.clear();
                                                RSocketConfig.getConnectedClients().remove(userName);
                                            }

                                        }
                                        if (showConnectedClient) {
                                            //System.out.println("Client Disconnect!");
                                            logger.info("Client Disconnect!");
                                        }
                                    })
                                    .subscribe();

                            return Mono.empty();
                        }
                );

    }

    @MessageExceptionHandler
    Mono<ResponseEntity<String>> handleException(Exception ex /*, RSocketRequester requester*/) {
        /*return Mono.delay(Duration.ofMillis(10)).map(aLong -> {
            return ex.getMessage() + " handled";
        });*/
        //return Mono.just(ex.getMessage() + " handled");
        //System.out.println(GlobalErrorHandler.getStackTrace(ex));
        logger.info(HttpException.convertStackTraceAsString(ex));
        if (ex instanceof DuplicateKeyException) {
            return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body("duplicateKey"));
        }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage()));
    }

    @MessageMapping("role.{phone}")
    public Mono<Boolean> changeRole(
            @DestinationVariable("phone") String phone,
            @Payload String increase
    ) {

        if (increase.equals("true") || Properties.getAdminUsers().contains(phone)) {
            return userService.changeRole(phone, "ADMIN", "USER");
        }

        return userService.changeRole(phone, "USER");
    }

    //@RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @MessageMapping("getUsers")
    public Flux<UserDTO> users(
            final RSocketRequester requester,
            @AuthenticationPrincipal Mono<Jwt> jwtToken
    ) {

        return jwtToken.flatMap(jwt -> {

            boolean isAdmin = false;
            Map<String, Object> claims = jwt.getClaims();
            String phone = (String) claims.get("sub");
            List<String> roles = (List<String>) claims.get("scope");
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).equals("ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("isAdmin", isAdmin);
            map.put("phone", phone);
            return Mono.just(map);
        })
                .flatMapMany(map -> {
                    String mobile = (String) map.get("phone");
                    if ((Boolean) map.get("isAdmin")) {
                        mobile = null;
                    }

                    if (mobile == null || mobile.equals("")) {
                        if ((boolean) map.get("isAdmin")) {
                            return userService.getRoots();
                        }
                        return userService.getUsersByPhoneParentIdWithParent((String) map.get("phone")).onBackpressureDrop();
                    }
                    return userService.getUsersByPhoneParentIdWithParent((String) map.get("phone")).onBackpressureDrop();
                });
    }

    //    @RequestMapping(value = "/get/childs/parentId/{parentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @ResponseBody
    @MessageMapping("getChildsByParentId.{parentId}")
    public Flux<UserDTO> getUsersByParentId(
            @AuthenticationPrincipal final Mono<Jwt> jwtToken,
            @DestinationVariable("parentId") String parentId
    ) {

        return jwtToken.flatMapMany(jwt -> {
            return userService.getUsersByParentId(parentId).onBackpressureDrop();
        });

    }

    @MessageMapping("edit.user.metadata")
    public Mono<UserDTO> editUserMetadata(
            @AuthenticationPrincipal final Mono<Jwt> jwtToken,
            @Payload final UserDTO userDTO
    ) {
        return jwtToken.flatMap(jwt -> {
            Map<String, Object> claims = jwt.getClaims();
            String phone = (String) claims.get("sub");
            userDTO.setUsername(phone);
            return userService.editUserMetadata(userDTO);
        });

    }


    //@PreAuthorize(value = "hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPERADMIN')")
    //@Transactional("electionTM")
    //@RequestMapping(value = "/add/user", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    //@ResponseBody
    @MessageMapping("add.user")
    public Mono<UserDTO> addUser(
            @AuthenticationPrincipal final Mono<Jwt> jwtToken,
            @Payload final UserDTO userDTO
    ) {

        return jwtToken.flatMap(jwt -> {
            User user = userDTO.toEntity();
            user.setActivationCode(Cookie.generateCode());
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                user.setRoles("USER");
            }
            return userService.save(user)
                    //.onErrorReturn(user)
                    .flatMap(newUser -> {
                        if (newUser.getParentId() != null) {
                            return userService.findById(newUser.getParentId())
                                    .flatMap(parentUser -> {
                                        String path = "/" + newUser.getId();
                                        path = parentUser.getPath() + path;
                                        newUser.setPath(path);
                                        return userService.save(newUser)
                                                .flatMap(user1 -> {
                                                    //SMSClient.sendSMS(user1.getUsername(), message).subscribe();
                                                    //SMSClient.sendSMS("9113394969", user1.getUsername(), user1.getActivationCode()).subscribe();
                                                    return Mono.just(new UserDTO(user1));
                                                });
                                    });
                        } else {
                            String path = "/" + newUser.getId();
                            newUser.setPath(path);
                            return userService.save(newUser)
                                    .flatMap(user1 -> {
                                        //SMSClient.sendSMS(user1.getUsername(), message).subscribe();
                                        //SMSClient.sendSMS("9113394969", user1.getUsername(), user1.getActivationCode()).subscribe();
                                        return Mono.just(new UserDTO(user1));
                                    });

                        }

                    });
        });
    }

    //@Transactional("electionTM")
//    @RequestMapping(value = "/edit/user", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//    @ResponseBody
    @MessageMapping("edit.user")
    public Mono<UserDTO> editUser(
            @AuthenticationPrincipal final Mono<Jwt> jwtToken,
            @Payload final UserDTO userDTO
    ) {

        return jwtToken.flatMap(jwt -> {
            User user = userDTO.toEntity();
            return userService.findById(user.getId())
                    .flatMap(user1 -> {
                        User editUser = user1;
                        editUser.setUsername(user.getUsername());
                        editUser.setFirstName(user.getFirstName());
                        editUser.setLastName(user.getLastName());
                        return userService.save(editUser).flatMap(
                                user2 -> {
                                    return Mono.just(new UserDTO(user2));
                                }
                        );
                    });
        });
    }


    @MessageMapping("forgot")
    public Mono<String> forgotPassword(@Payload String user) {

        String result = "{\"code\":\"%s\"}";

        return userService.forgotPassword(user)
                .flatMap(code -> {
                    if (code != null) {
                        return Mono.just(String.format(result, code));
                    }
                    return Mono.just(String.format(result, ""));
                });
    }


    @MessageMapping("update.message")
    public Mono<MessageDTO> updateMessage(
            @AuthenticationPrincipal final Mono<Jwt> jwtToken,
            @Payload final MessageDTO messageDTO,
            RSocketRequester requester
    ) {

        return jwtToken.flatMap(jwt -> {

            boolean isAdmin = false;
            Map<String, Object> claims = jwt.getClaims();
            String phone = (String) claims.get("sub");
            List<String> roles = (List<String>) claims.get("scope");
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).equals("ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }

            final MessageDTO dto = new MessageDTO();
            dto.setId(messageDTO.getId());

            if (!isAdmin) {
                return Mono.just(dto);
            }

            String nativeQuery = String.format("{_id:ObjectId('%s')}", messageDTO.getId());

            Document document = new Document();
            document.put("title", messageDTO.getTitle());
            document.put("text", messageDTO.getText());
            return reactiveNativeMongo.updateOne("message", nativeQuery, document)
                    .flatMap(updateResult -> {
                        if (updateResult.wasAcknowledged()) {
                            return reactiveNativeMongo.findOne("message", nativeQuery)
                                    .flatMap(documentAfterUpdate -> {
                                        dto.setId(documentAfterUpdate.getObjectId("_id").toHexString());
                                        dto.setTitle(documentAfterUpdate.getString("title"));
                                        dto.setText(documentAfterUpdate.getString("text"));

                                        // send to other client ( broadcast )
                                        Flux.fromIterable(RSocketConfig.getConnectedClients().entrySet())
                                                .flatMap(
                                                        session -> {
                                                            return Flux.fromIterable(session.getValue());
                                                        }
                                                )
                                                .filter(
                                                        client -> {
                                                            return !client.equals(requester);
                                                        }
                                                )
                                                .subscribe(
                                                        client -> {

                                                            client.route("updateMessage")
                                                                    .data(dto)
                                                                    .send()
                                                                    .subscribe();

                                                        }
                                                );
                                        return Mono.just(dto);
                                    });
                        }


                        return Mono.just(dto);
                    });

        });

    }

    //@Transactional("electionTM")
//    @RequestMapping(value = "/delete/users/id/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
//    @ResponseBody
    @MessageMapping("delete.getUsersBy.{id}")
    public Mono<String> deleteUsers(
            @AuthenticationPrincipal final Mono<Jwt> jwtToken,
            @DestinationVariable("id") final String id
    ) {

        return jwtToken.flatMap(jwt -> {

            boolean isAdmin = false;
            Map<String, Object> claims = jwt.getClaims();
            String phone = (String) claims.get("sub");
            List<String> roles = (List<String>) claims.get("scope");
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).equals("ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }

            if (isAdmin) {
                userService.deleteAllUsersById(id);
                return Mono.just("{\"status\":true}");
            }

            return Mono.just("{\"status\":false}");
        });

    }

    @MessageMapping("upload.{title}.{text}.{filename}.{contentType}.{size}.{duration}")
    public Mono<MessageDTO> upload(
            @DestinationVariable("title") String title,
            @DestinationVariable("text") String text,
            @DestinationVariable("filename") String filename,
            @DestinationVariable("contentType") String contentType,
            @DestinationVariable("size") int size,
            @DestinationVariable("duration") String duration,
            @Payload Flux<DataBuffer> content,
            @AuthenticationPrincipal final Mono<Jwt> jwtToken,
            RSocketRequester requester
    ) {
        final String newFilename = filename.replaceAll("-dot-", ".").replaceAll("-slash-", "/");
        final String newContentType = contentType.replaceAll("-dot-", ".").replaceAll("-slash-", "/");
        final double newDuration = Double.valueOf(duration.replaceAll("-dot-", "."));

        final String newTitle = String.valueOf(title.replaceAll("-dot-", "."));
        final String newText = String.valueOf(text.replaceAll("-dot-", "."));

        return jwtToken.flatMap(
                jwt -> {
                    boolean isAdmin = false;
                    Map<String, Object> claims = jwt.getClaims();
                    String phone = (String) claims.get("sub");
                    List<String> roles = (List<String>) claims.get("scope");
                    for (int i = 0; i < roles.size(); i++) {
                        if (roles.get(i).equals("ADMIN")) {
                            isAdmin = true;
                            break;
                        }
                    }

                    if (isAdmin) {
                        return userService.findByUsername(phone).flatMap(user -> {
                            return reactiveNativeMongo.getReactiveMongoGridFs().save(content, newFilename, size, newContentType, newDuration, null, null)
                                    .flatMap(
                                            fsFile -> {
                                                Message message = new Message();
                                                message.setAuthorPhone(user.getUsername());
                                                message.setFileId(fsFile.getObjectId("_id"));
                                                message.setTitle(newTitle);
                                                message.setText(newText);
                                                return Mono.just(message);
                                            }
                                    )
                                    .flatMap(
                                            message -> {
                                                return messageService.save(message)
                                                        .flatMap(
                                                                messageAfterSave -> {

                                                                    MessageDTO messageDTO = new MessageDTO();
                                                                    messageDTO.setId(messageAfterSave.getId().toHexString());
                                                                    messageDTO.setPhone(phone);
                                                                    messageDTO.setFirstName(user.getFirstName());
                                                                    messageDTO.setLastName(user.getLastName());
                                                                    messageDTO.setTitle(message.getTitle());
                                                                    messageDTO.setText(message.getText());
                                                                    messageDTO.setFileId(message.getFileId().toHexString());
                                                                    messageDTO.setFilename(newFilename);
                                                                    messageDTO.setContentType(newContentType);
                                                                    messageDTO.setDuration(newDuration);
                                                                    messageDTO.setSize(size);

                                                                    // send to other client
                                                                    Flux.fromIterable(RSocketConfig.getConnectedClients().entrySet())
                                                                            .flatMap(
                                                                                    session -> {
                                                                                        return Flux.fromIterable(session.getValue());
                                                                                    }
                                                                            )
                                                                            .filter(
                                                                                    client -> {
                                                                                        return !client.equals(requester);
                                                                                    }
                                                                            )
                                                                            .subscribe(
                                                                                    client -> {

                                                                                        client.route("pushMessage")
                                                                                                .data(messageDTO)
                                                                                                .send()
                                                                                                .subscribe();

                                                                                    }
                                                                            );

                                                                    return Mono.just(messageDTO);
                                                                }
                                                        );
                                            });

                        });

                    }

                    return Mono.empty();
                }
        );


    }

    @MessageMapping("download")
    public Flux<DataBuffer> download(@Payload String id) {
        return reactiveNativeMongo.getReactiveMongoGridFs().getFileByFileId(id).onBackpressureDrop();
    }


    @MessageMapping("delete")
    public Mono<String> delete(
            @Payload MessageDTO messageDTO,
            RSocketRequester requester
    ) {
        messageService.deleteByIdAndFileId(messageDTO.getId(), messageDTO.getFileId()).subscribe();

        Flux.fromIterable(RSocketConfig.getConnectedClients().entrySet())
                .flatMap(
                        session -> {
                            return Flux.fromIterable(session.getValue());
                        }
                )
                .filter(
                        client -> {
                            return !client.equals(requester);
                        }
                )
                .subscribe(
                        client -> {
                            client.route("delete").data(messageDTO.getId()).send().subscribe();
                        }
                );

        return Mono.just(messageDTO.getId());
    }

    @MessageMapping("all.messages")
    public Flux<MessageDTO> allMessages(RSocketRequester requester) {
        return messageService.messageJoinUserAndFile().onBackpressureDrop();
    }
}
