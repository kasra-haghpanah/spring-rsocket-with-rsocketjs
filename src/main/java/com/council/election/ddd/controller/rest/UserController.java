package com.council.election.ddd.controller.rest;

import com.council.election.configuration.property.Properties;
import com.council.election.configuration.webflux.security.config.JwtConfig;
import com.council.election.configuration.webflux.security.dto.JwtResponse;
import com.council.election.ddd.client.rest.SMSClient;
import com.council.election.ddd.dto.UserDTO;
import com.council.election.ddd.model.User;
import com.council.election.ddd.service.UserService;
import com.council.election.ddd.utility.Cookie;
import org.springframework.http.codec.multipart.Part;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Controller
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final String serverDomainBaseUrl = Properties.getServerDomainBaseUrl();

    public UserController(
            UserService userService,
            PasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public Mono<String> index() {
        return Mono.just("index");
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public Mono<String> logout(
            ServerWebExchange exchange,
            @RequestHeader(value = "Cookie", required = false) String cookie
    ) {
        exchange.getResponse().getCookies().clear();
        Cookie.setCookie(exchange, "");
        return Mono.just("redirect:/");
    }

    @RequestMapping(value = "/cookie", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<JwtResponse> cookie(
            @RequestHeader(value = "Cookie", required = false) String cookie,
            @AuthenticationPrincipal final Mono<Jwt> jwtToken,
            ServerWebExchange exchange
    ) {
        final JwtResponse tokenDto = new JwtResponse("", "", -1, serverDomainBaseUrl, null);
        if (jwtToken == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return Mono.just(tokenDto);
        }

        return jwtToken.flatMap(jwt -> {
                    List<String> roles = (List<String>) jwt.getClaims().get("scope");
                    String username = (String) jwt.getClaims().get("sub");
                    int level = 1;
                    for (int i = 0; i < roles.size(); i++) {
                        if (roles.get(i).equals("ADMIN")) {
                            level = 0;
                            break;
                        }
                    }
                    final int levelValue = level;

                    return userService.findByUsername(username)
                            .flatMap(user -> {
                                tokenDto.setToken(jwt.getTokenValue()).setUsername(username).setLevel(levelValue).setMetadata(user.getMetadata());
                                return Mono.just(tokenDto);
                            });


                })
                .then(Mono.just(tokenDto));


    }


    @RequestMapping(value = "/change/{user}/{code}/{pass}", method = RequestMethod.PUT)
    @ResponseBody
    public Mono<JwtResponse> changePassword(
            @PathVariable("user") String user,
            @PathVariable("pass") String pass,
            @PathVariable("code") String code,
            ServerWebExchange exchange

    ) {
        return userService.changePassword(user, code, pass)
                .flatMap(
                        update -> {
                            final JwtResponse jwtResponse = new JwtResponse("ErrorSignInMessage", "", -1, serverDomainBaseUrl, null);

                            if (update.booleanValue()) {
                                return userService.findByUsername(user)
                                        .flatMap(userDetails -> {
                                            int level = userDetails.getRoles().contains("ADMIN") ? 0 : 1;
                                            JwtResponse response = new JwtResponse(JwtConfig.encoder(userDetails), userDetails.getUsername(), level, serverDomainBaseUrl, userDetails.getMetadata());
                                            MultiValueMap<String, ResponseCookie> cookies = exchange.getResponse().getCookies();
                                            exchange.getResponse().getCookies().toSingleValueMap().forEach((key, value) -> {
                                                cookies.remove(key);
                                            });

                                            exchange.getResponse().getCookies().remove("Cookie");

                                            ResponseCookie cookie = ResponseCookie.from("Cookie", response.getToken()).httpOnly(true).maxAge(Properties.getJwtExpirationTime()).build();
                                            exchange.getResponse().getCookies().set("token", cookie);

                                            jwtResponse.setUsername(userDetails.getUsername());
                                            jwtResponse.setToken(response.getToken());
                                            jwtResponse.setLevel(level);

                                            return Mono.just(jwtResponse);
                                        })
                                        .then(Mono.just(jwtResponse));

                            }
                            return Mono.just(jwtResponse);


                        }
                );
    }

    @RequestMapping(value = "/forgot/{user}", method = RequestMethod.PUT)
    @ResponseBody
    public Mono<String> forgotPassword(
            @PathVariable("user") String user,
            ServerWebExchange exchange

    ) {

        String result = "{\"code\":\"%s\"}";

        return userService.forgotPassword(user)
                .flatMap(code -> {
                    if (code != null) {
                        //SMSClient.sendSMS("09113394969", user, code).subscribe();
                        //return Mono.just(String.format(result, "SendSMS"));
                        return Mono.just(String.format(result, code));
                    }
                    //return Mono.just(String.format(result, "NotExistsUser"));
                    return Mono.just(String.format(result, ""));
                });
    }

    @RequestMapping(value = "/active/{user}/{code}", method = RequestMethod.GET)
    public Mono<String> activeUser(
            @PathVariable("user") String user,
            @PathVariable("code") String code
    ) {

        User userDetails = new User();

        return userService.findByUsernameAndActivationCode(user, code)
                .flatMap(userModel -> {
                    userDetails.setUsername(userModel.getUsername());
                    userDetails.setPassword(userModel.getPassword());
                    userDetails.setActivationCode(userModel.getActivationCode());
                    return Mono.just(userDetails);
                })
                .then(Mono.just(userDetails))
                .flatMap(userModel -> {

                    if (userModel.getUsername() != null && userModel.getActivationCode() != null) {
                        return Mono.just(String.format("redirect:/#!/change/password/%s/%s", userModel.getUsername(), userModel.getActivationCode()));
                    }
                    return Mono.just("redirect:/");
                });
    }


    @RequestMapping(
            value = "/signin",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public Mono<JwtResponse> signin(
            @RequestPart(value = "username", required = false) String username,
            @RequestPart(value = "password", required = false) String password,
            //@RequestPart("file-data") FilePart file,
            //@RequestBody Mono<MultiValueMap<String, Part>> parts,
            ServerWebExchange exchange
    ) {

        return exchange.getFormData().flatMap(form -> {
            String user = form.get("username").get(0);
            while (user.length() > 1 && user.indexOf("0") == 0) {
                user = user.substring(1, user.length());
            }
            String pass = form.get("password").get(0);
            return userService.findByUsername(user)
                    .flatMap(userDetails -> {
                        if (userDetails != null && passwordEncoder.matches(pass, userDetails.getPassword())) {
                            int level = userDetails.getRoles().contains("ADMIN") ? 0 : 1;
                            JwtResponse response = new JwtResponse(JwtConfig.encoder(userDetails), userDetails.getUsername(), level, serverDomainBaseUrl, userDetails.getMetadata());
                            Cookie.setCookie(exchange, response.getToken());
                            //return Mono.just(response);
                            return Mono.just(new JwtResponse(response.getToken(), userDetails.getUsername(), level, serverDomainBaseUrl, userDetails.getMetadata()));
                        }
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return Mono.just(new JwtResponse("ErrorSignInMessage", "", -1, serverDomainBaseUrl, null));
                    });


        });
    }


    //@Transactional("electionTM")
    @RequestMapping(value = "/signup", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<JwtResponse> siginup(
//            @RequestParam(value = "username", required = false) String userName,
//            @RequestParam(value = "password", required = false) String password,
//            @RequestParam(value = "code", required = false) String code,
            ServerWebExchange exchange
    ) {

        return exchange.getFormData()
                .flatMap(form -> {
                    User user = new User();
                    user.setUsername(form.get("username").get(0));
                    user.setPassword(form.get("password").get(0));
                    user.setFirstName(form.get("firstname").get(0));
                    user.setLastName(form.get("lastname").get(0));
                    user.setRoles("USER"/*, "ADMIN"*/);
                    if (user.getUsername() != null && Properties.getAdminUsers().contains(user.getUsername())) {
                        user.setRoles("USER", "ADMIN");
                    }

                    return userService.save(user)
//                            .onErrorResume(throwable -> {
//                                user.setUsername("isExistsUser");
//                                return Mono.just(user);
//                            })
                            .flatMap(userAfterSave -> {

                                if (userAfterSave.getUsername().equals("isExistsUser")) {
                                    exchange.getResponse().setStatusCode(HttpStatus.CONFLICT);
                                    return Mono.just(new JwtResponse("", userAfterSave.getUsername(), -1, serverDomainBaseUrl, null));
                                }

                                String path = "/" + userAfterSave.getId();
                                if (userAfterSave.getParentId() != null) {
                                    return userService.findById(userAfterSave.getParentId())
                                            .flatMap(parentUser -> {
                                                String newPath = parentUser.getPath() + path;
                                                userAfterSave.setPath(newPath);

                                                return userService.save(userAfterSave)
                                                        .flatMap(newUser -> {
                                                            UserDetails userDetails = (UserDetails) newUser;
                                                            int level = userDetails.getAuthorities().contains("ADMIN") ? 0 : 1;
                                                            userAfterSave.setParentId(newUser.getParentId());
                                                            if (userDetails != null) {
                                                                JwtResponse response = new JwtResponse(JwtConfig.encoder(userDetails), userDetails.getUsername(), level, serverDomainBaseUrl, newUser.getMetadata());
                                                                Cookie.setCookie(exchange, response.getToken());
                                                                return Mono.just(response);
                                                            } else {
                                                                return Mono.just(new JwtResponse(JwtConfig.encoder(userDetails), userDetails.getUsername(), level, serverDomainBaseUrl, newUser.getMetadata()));
                                                            }
                                                        });
                                            });

                                } else {
                                    userAfterSave.setPath(path);
                                }
                                return userService.save(userAfterSave)
                                        .flatMap(newUser -> {
                                            int level = newUser.getRoles().contains("ADMIN") ? 0 : 1;
//                                            String message = String.format("%s/active/%s/%s", serverBaseUrl, user1.getUsername(), user1.getActivationCode());
//                                            //SMSClient.sendSMS(user1.getUsername(), message).subscribe();
//                                            SMSClient.sendSMS("9113394969", message).subscribe();
                                            return Mono.just(new JwtResponse(JwtConfig.encoder((UserDetails) newUser), newUser.getUsername(), level, serverDomainBaseUrl, newUser.getMetadata()));
                                        });


                            });


                });
    }


    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Flux<UserDTO> users(
            @RequestHeader("mobile") final String mobile,
            @AuthenticationPrincipal final Mono<Jwt> jwtToken
    ) {
//        if (cookie == null || cookie.equals("") || cookie.indexOf("Cookie=") != 0) {
//            return Flux.empty();
//        }
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
                    if (mobile == null || mobile.equals("")) {
                        if ((boolean) map.get("isAdmin")) {
                            return userService.getRoots();
                        }

                        return userService.getUsersByPhoneParentIdWithParent((String) map.get("phone"));

                    }


                    return userService.getUsersByPhoneParentId((String) map.get("phone"));

                });
    }


    @RequestMapping(value = "/get/childs/parentId/{parentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Flux<UserDTO> getUsersByParentId(
            @PathVariable("parentId") String parentId,
            @AuthenticationPrincipal final Mono<Jwt> jwtToken
    ) {

        return jwtToken.flatMapMany(jwt -> {
            return userService.getUsersByParentId(parentId);
        });

    }


    //@PreAuthorize(value = "hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPERADMIN')")
    //@Transactional("electionTM")
    @RequestMapping(value = "/add/user", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<UserDTO> addUser(
            @RequestBody(required = true) final UserDTO userDTO,
            @AuthenticationPrincipal final Mono<Jwt> jwtToken
    ) {

        return jwtToken.flatMap(jwt -> {
            User user = userDTO.toEntity();
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                user.setRoles("USER");
            }

            return userService.save(user)
                    .flatMap(newUser -> {
                        if (newUser.getParentId() != null) {
                            return userService.findById(newUser.getParentId())
                                    .flatMap(parentUser -> {
                                        String path = "/" + newUser.getId();
                                        path = parentUser.getPath() + path;
                                        newUser.setPath(path);
                                        return userService.save(newUser)
                                                .flatMap(user1 -> {
                                                    return Mono.just(new UserDTO(user1));
                                                });

                                    });

                        }
                        return Mono.empty();
                    });


        });

    }

    //@Transactional("electionTM")
    @RequestMapping(value = "/edit/user", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<UserDTO> editUser(
            @RequestBody(required = true) final UserDTO userDTO,
            @AuthenticationPrincipal final Mono<Jwt> jwtToken,
            ServerWebExchange exchange
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

            // exchange.getResponse().setStatusCode(HttpStatus.METHOD_FAILURE);

        });

    }

    //@Transactional("electionTM")
    @RequestMapping(value = "/delete/users/id/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<String> deleteUsers(
            @PathVariable("id") final String id,
            @AuthenticationPrincipal final Mono<Jwt> jwtToken
    ) {
//        if (cookie == null || cookie.equals("") || cookie.indexOf("Cookie=") != 0) {
//            return Mono.empty();
//        }
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


}
