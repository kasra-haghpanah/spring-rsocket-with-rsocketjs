package com.council.election.configuration.webflux.view;

import com.council.election.configuration.property.Properties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
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

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * Created by kasra.haghpanah on 11/9/2019.
 */
@Configuration
@EnableWebFlux
@DependsOn({"properties"})
public class ThymeleafConfig implements WebFluxConfigurer /*extends DefaultErrorAttributes implements ApplicationContextAware, ErrorAttributes*/ {
    //https://howtodoinjava.com/spring-webflux/spring-webflux-tutorial/

    ApplicationContext applicationContext;
    public final String[] resourceHandler;
    public final String[] resourceLocations;

    public ThymeleafConfig() {

        resourceHandler = new String[]{
                MessageFormat.format("/{0}/css/**", Properties.getViewVersion()),
                MessageFormat.format("/{0}/custom/**", Properties.getViewVersion()),
                MessageFormat.format("/{0}/fonts/**", Properties.getViewVersion()),
                MessageFormat.format("/{0}/images/**", Properties.getViewVersion()),
                MessageFormat.format("/{0}/js/**", Properties.getViewVersion()),
                MessageFormat.format("/{0}/lib/**", Properties.getViewVersion()),
                MessageFormat.format("/{0}/view/**", Properties.getViewVersion()),
                MessageFormat.format("/{0}/favicon.**", Properties.getViewVersion())
        };

        resourceLocations = new String[]{
                MessageFormat.format("classpath:static/{0}/css/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/custom/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/fonts/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/images/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/js/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/lib/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/view/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/", Properties.getViewVersion())
        };

    }

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
    }


    @Bean
    public ITemplateResolver thymeleafTemplateResolver() {
        final SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(this.applicationContext);
        resolver.setPrefix("classpath:templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode(TemplateMode.HTML);

        resolver.setCacheable(true);
        resolver.setCacheablePatterns(Set.of(MessageFormat.format("/{0}/**", Properties.getViewVersion())));

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
