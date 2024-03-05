package com.spring.boot.spring5webapp.controllers;

import com.spring.boot.spring5webapp.ddd.repositories.mongo.reactive.UserMongoRepository;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

/**
 * Created by kasra.haghpanah on 16/11/2019.
 */
//@Controller
public class AuthenticationController {

    UserMongoRepository userMongoRepository;

    ReactiveUserDetailsService reactiveUserDetailsService;

    public AuthenticationController(UserMongoRepository userMongoRepository) {
        this.userMongoRepository = userMongoRepository;
    }

    //@ModelAttribute("_csrf")
/*    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String defaultPage(ServerWebExchange exchange, Model model) {

//        String tokenValue = exchange.getRequest().getCookies().get("XSRF-TOKEN").get(0).getValue();
//        CsrfToken csrfToken = new DefaultCsrfToken("XSRF-TOKEN", "_csrf", tokenValue);
//        model.addAttribute("_csrf", csrfToken);

        Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
        model.addAttribute("_csrf", token);

        return "index";
    }

    //@ModelAttribute("_csrf")
   @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index(ServerWebExchange exchange, Model model) {

//        String tokenValue = exchange.getRequest().getCookies().get("XSRF-TOKEN").get(0).getValue();
//        CsrfToken csrfToken = new DefaultCsrfToken("XSRF-TOKEN", "_csrf", tokenValue);
//        model.addAttribute("_csrf", csrfToken);

        Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
        model.addAttribute("_csrf", token);

        return "index";
    }*/

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(ServerWebExchange exchange, Model model) {

        String key1 = "Idea-8af5a049";
        String key2 = "XSRF-TOKEN";
        String key3 = "SESSION";

//        String tokenValue = exchange.getRequest().getCookies().get("XSRF-TOKEN").get(0).getValue();
//        CsrfToken csrfToken = new DefaultCsrfToken("XSRF-TOKEN", "_csrf", tokenValue);
//        model.addAttribute("_csrf", csrfToken);

        //Mono<CsrfToken> token = exchange.getAttributeOrDefault(CsrfToken.class.getName(), Mono.empty());
        //model.addAttribute("_csrf", token);
//        return token.map(t -> {
//            model.addAttribute("_csrf", t);
//            return "login";
//        });

         return "login";
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout() {
        return "logout";
    }


/*    @PostMapping("/login")
    public Mono<UserDetails> login(ServerWebExchange exchange , @RequestParam("username") Integer username , @RequestParam("password") Integer password ,  @RequestParam("_csrf") Integer csrf) {

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class)
                .doOnNext(userDetail -> {

                    exchange.getResponse().addCookie();
                    addTokenHeader(exchange.getResponse(), userMongoRepository.findByUsername(userDetail.getUsername())); // your job to code it the way you want
                });
    }*/

}
