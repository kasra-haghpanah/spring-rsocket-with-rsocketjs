package com.council.election.configuration.webflux.view;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.thymeleaf.spring6.ISpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import reactor.core.publisher.Mono;
//import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * Created by kasra.haghpanah on 11/9/2019.
 */
@Configuration
@EnableWebFlux
/*@PropertySources({
        @PropertySource("classpath:static/css/"),
        @PropertySource("classpath:static/custom/"),
        @PropertySource("classpath:static/fonts/"),
        @PropertySource("classpath:static/images/"),
        @PropertySource("classpath:static/js/"),
        @PropertySource("classpath:static/lib/"),
        @PropertySource("classpath:static/view/")
})
@Import(SwaggerConfig.class)*/
//@EnableSwagger2
public class ThymeleafConfig implements WebFluxConfigurer /*extends DefaultErrorAttributes implements ApplicationContextAware, ErrorAttributes*/ {
    //https://howtodoinjava.com/spring-webflux/spring-webflux-tutorial/

    ApplicationContext applicationContext;

    public static final String[] resourceHandler = {
            "/css/**",
            "/custom/**",
            "/fonts/**",
            "/images/**",
            "/js/**",
            "/lib/**",
            "/view/**",
            "/favicon.**"
            //,"/swagger-ui/**"
    };

    public static final String[] resourceLocations = {
            "classpath:static/css/",
            "classpath:static/custom/",
            "classpath:static/fonts/",
            "classpath:static/images/",
            "classpath:static/js/",
            "classpath:static/lib/",
            "classpath:static/view/",
            "classpath:static/"
//            ,"classpath:/META-INF/resources/"
//            ,"classpath:/META-INF/resources/webjars/"
    };

/*
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    //////////////////////////exception handling

    @Override//for global exception handling
    public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        Map<String, Object> map = null;
        try {
            map = super.getErrorAttributes(request, includeStackTrace);
        } catch (Exception e) {

        }
        //map.put("status", HttpStatus.BAD_REQUEST);
        //map.put("message", "username is required");

        return map;
    }

    @Override
    public Throwable getError(ServerRequest serverRequest) {
        return super.getError(serverRequest);
    }

    @Override
    public void storeErrorInformation(Throwable throwable, ServerWebExchange serverWebExchange) {
        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));
        System.out.println("for logging!" + errors.toString());

        serverWebExchange.getRequest().getBody()
                .map(body -> {
                    return body;
                    //  Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
                    //  ResolvableType elementType = forClass(LoginRequestDto.class);
                    //   return decoder.decodeToMono(body, elementType, MediaType.APPLICATION_JSON, Collections.emptyMap()).cast(LoginRequestDto.class);
                }).next();

    }


    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {

        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, false);

        return ServerResponse.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorPropertiesMap));
    }*/

    //////////////////////////exception handling

    protected int getHttpStatus(Map<String, Object> errorAttributes) {
        return ((Integer) errorAttributes.get("status")).intValue();
    }

    @Override
   // @Ap(SwaggerConfig.class)
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(resourceHandler).addResourceLocations(resourceLocations);

        registry.addResourceHandler("/swagger-ui")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        //.setCachePeriod(31556926);
    }


    @Bean
    public ITemplateResolver thymeleafTemplateResolver() {
        final SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(this.applicationContext);
        resolver.setPrefix("classpath:templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        resolver.setCheckExistence(false);
        return resolver;
    }

    @Bean
    public ISpringWebFluxTemplateEngine thymeleafTemplateEngine() {
        SpringWebFluxTemplateEngine templateEngine = new SpringWebFluxTemplateEngine();
        templateEngine.setTemplateResolver(thymeleafTemplateResolver());
        return templateEngine;
    }

    @Bean
    public ThymeleafReactiveViewResolver thymeleafReactiveViewResolver() {
        ThymeleafReactiveViewResolver viewResolver = new ThymeleafReactiveViewResolver();
        viewResolver.setTemplateEngine(thymeleafTemplateEngine());
        return viewResolver;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.viewResolver(thymeleafReactiveViewResolver());
    }

    public Mono<ServerResponse> handleRequest(ServerRequest request) {
        return Mono.just("Created exception!")
                .flatMap(s -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .bodyValue(s))
                .onErrorResume(e -> Mono.just("Error " + e.getMessage())
                        .flatMap(s -> ServerResponse.ok()
                                .contentType(MediaType.TEXT_PLAIN)
                                .bodyValue(s)));
    }


/*
    @Bean
    public RouterFunction<ServerResponse> route(WebFluxConfig welcomeHandler) {

        HandlerFunction<ServerResponse> indexHandlerFunction = (request) -> {
            // final Map<String, Object> model = Collections.singletonMap(CsrfToken.class.getName(), request.exchange().getAttribute(CsrfToken.class.getName()));
            return ServerResponse.ok().contentType(MediaType.TEXT_HTML).render("index", request.exchange()*/
/*, model*//*
);
        };


        return RouterFunctions
                .route(GET("/index").and(RequestPredicates.accept(MediaType.TEXT_HTML)), indexHandlerFunction)
                .andRoute(GET("/").and(RequestPredicates.accept(MediaType.TEXT_HTML)), indexHandlerFunction)
                */
/*.Filter((req, resHandler) ->
                        req.exchange()
                                .getAttributeOrDefault(
                                        CsrfToken.class.getName(),
                                        Mono.empty().ofType(CsrfToken.class)
                                )
                                .flatMap(csrfToken -> {

                                    req.exchange()
                                            .getAttributes()
                                            .put(csrfToken.getParameterName(), csrfToken);
                                    return resHandler.handle(req);
                                })

                )*//*
;


    }
*/


}
