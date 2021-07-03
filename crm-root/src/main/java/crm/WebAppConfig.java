package crm;

import crm.plugin.CustomPlugin;
import crm.viewResolver.CsvViewResolver;
import crm.viewResolver.ExcelViewResolver;
import crm.viewResolver.PdfViewResolver;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.thymeleaf.dialect.springdata.SpringDataDialect;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class WebAppConfig implements WebMvcConfigurer {

    @Autowired
    private PluginManager pluginManager;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/user/menu").setViewName("user/user-menu");
        registry.addViewController("/customer/menu").setViewName("customer/customer-menu");
        registry.addViewController("/contract/menu").setViewName("contract/contract-menu");
        registry.addViewController("/contract/search").setViewName("contract/search");
        registry.addViewController("/admin").setViewName("admin/panel");
        registry.addViewController("/search").setViewName("search");
        registry.addViewController("/403").setViewName("403");
        registry.addViewController("/logout").setViewName("logout");

        List<PluginWrapper> plugins = pluginManager.getPlugins();

        for(PluginWrapper pluginWrapper: plugins) {

            if (pluginWrapper.getPlugin() instanceof CustomPlugin) {
                CustomPlugin plugin = (CustomPlugin) pluginWrapper.getPlugin();

                if (plugin.hasTemplates()) {
                    registry.addViewController("/" + plugin.getMenuUrl()).setViewName(plugin.getMenuUrl());
                }
            }
        }
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(true)
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                .useJaf(false);

        final Map<String,MediaType> mediaTypes = new HashMap<>();
        mediaTypes.put("html", MediaType.TEXT_HTML);
        mediaTypes.put("json", MediaType.APPLICATION_JSON);
        mediaTypes.put("xls", MediaType.valueOf("application/vnd.ms-excel"));
        mediaTypes.put("pdf", MediaType.APPLICATION_PDF);
        mediaTypes.put("csv", new MediaType("text","csv", Charset.forName("utf-8")));
        configurer.mediaTypes(mediaTypes);
    }

    /**
     * Configure ContentNegotiatingViewResolver
     */
    @Bean
    public ViewResolver contentNegotiatingViewResolver(ContentNegotiationManager manager) {
        ContentNegotiatingViewResolver resolver = new ContentNegotiatingViewResolver();
        resolver.setContentNegotiationManager(manager);

        // Define all possible view resolvers
        List<ViewResolver> resolvers = new ArrayList<>();

        resolvers.add(csvViewResolver());
        resolvers.add(excelViewResolver());
        resolvers.add(pdfViewResolver());
        resolvers.add(viewResolver());

        resolver.setViewResolvers(resolvers);
        return resolver;
    }

    @Bean
    @Description("Thymeleaf template resolver serving HTML 5")
    public ClassLoaderTemplateResolver templateResolver() {

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();

        templateResolver.setPrefix("templates/");
        templateResolver.setCacheable(false);
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCheckExistence(true);
        templateResolver.setOrder(1);

        return templateResolver;
    }

    public List<ClassLoaderTemplateResolver> pluginTemplateResolvers() {

        List<ClassLoaderTemplateResolver> pluginTemplateResolvers = new ArrayList<>();

        List<PluginWrapper> plugins = pluginManager.getPlugins();

        int orderStart = 2;
        for(PluginWrapper pluginWrapper: plugins){

            if(pluginWrapper.getPlugin() instanceof CustomPlugin){
                ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver(pluginWrapper.getPluginClassLoader());
                CustomPlugin plugin =(CustomPlugin) pluginWrapper.getPlugin();

                if(plugin.hasTemplates()){
                    templateResolver.setPrefix(pluginWrapper.getPluginId() + "/");
                    templateResolver.setCacheable(false);
                    templateResolver.setSuffix(".html");
                    templateResolver.setTemplateMode("HTML");
                    templateResolver.setCharacterEncoding("UTF-8");
                    templateResolver.setCheckExistence(true);
                    templateResolver.setOrder(orderStart++);
                    pluginTemplateResolvers.add(templateResolver);
                }
            }
        }

        return pluginTemplateResolvers;
    }

    @Bean
    @Description("Thymeleaf template engine with Spring integration")
    public SpringTemplateEngine templateEngine() {

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(templateResolver());

        for(ClassLoaderTemplateResolver pluginTemplateResolver: pluginTemplateResolvers()){
            templateEngine.addTemplateResolver(pluginTemplateResolver);
        }

        // add dialect spring security
        templateEngine.addDialect(new SpringSecurityDialect());
        return templateEngine;
    }
/*
    @Bean
    public TemplateEngine templateEngine(ITemplateResolver templateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addDialect(new Java8TimeDialect());
        engine.setTemplateResolver(templateResolver);
        return engine;
    }
*/
    @Bean
    @Description("Thymeleaf view resolver")
    public ViewResolver viewResolver() {

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();

        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setCharacterEncoding("UTF-8");

        return viewResolver;
    }

    /**
     * Configure View resolver to provide XLS output using Apache POI library to
     * generate XLS output for an object content
     */
    @Bean
    public ViewResolver excelViewResolver() {
        return new ExcelViewResolver();
    }

    /**
     * Configure View resolver to provide Csv output using Super Csv library to
     * generate Csv output for an object content
     */
    @Bean
    public ViewResolver csvViewResolver() {
        return new CsvViewResolver();
    }

    /**
     * Configure View resolver to provide Pdf output using iText library to
     * generate pdf output for an object content
     */
    @Bean
    public ViewResolver pdfViewResolver() {
        return new PdfViewResolver();
    }

    @Bean
    public SpringDataDialect springDataDialect() {
        return new SpringDataDialect();
    }
}