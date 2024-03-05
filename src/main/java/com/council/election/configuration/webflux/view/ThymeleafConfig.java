package com.council.election.configuration.webflux.view;

import com.council.election.configuration.property.Properties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.CacheControl;
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
import java.util.concurrent.TimeUnit;

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
                MessageFormat.format("/{0}/favicon.**", Properties.getViewVersion()),
                "/favicon.**"
        };

        resourceLocations = new String[]{
                MessageFormat.format("classpath:static/{0}/css/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/custom/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/fonts/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/js/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/lib/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/view/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/", Properties.getViewVersion()),
                MessageFormat.format("classpath:static/{0}/images/", Properties.getViewVersion()),
                "classpath:static/images/"
        };

    }

    protected int getHttpStatus(Map<String, Object> errorAttributes) {
        return ((Integer) errorAttributes.get("status")).intValue();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // https://www.baeldung.com/cachable-static-assets-with-spring-mvc
        registry.addResourceHandler(resourceHandler)
                .addResourceLocations(resourceLocations)
                .setCacheControl(CacheControl.maxAge(360, TimeUnit.SECONDS));

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

        //resolver.setCacheable(true);
        //resolver.setCacheablePatterns(Set.of(MessageFormat.format("/{0}/**", Properties.getViewVersion())));

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
