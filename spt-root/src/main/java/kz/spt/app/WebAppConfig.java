package kz.spt.app;

import kz.spt.lib.plugin.CustomPlugin;
import kz.spt.app.viewResolver.CsvViewResolver;
import kz.spt.app.viewResolver.ExcelViewResolver;
import kz.spt.app.viewResolver.PdfViewResolver;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.thymeleaf.dialect.springdata.SpringDataDialect;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executor;

@Configuration
public class WebAppConfig implements WebMvcConfigurer {

    @Autowired
    @Lazy
    private PluginManager pluginManager;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/403").setViewName("403");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(true)
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                .useJaf(false);

        final Map<String, MediaType> mediaTypes = new HashMap<>();
        mediaTypes.put("html", MediaType.TEXT_HTML);
        mediaTypes.put("json", MediaType.APPLICATION_JSON);
        mediaTypes.put("xls", MediaType.valueOf("application/vnd.ms-excel"));
        mediaTypes.put("pdf", MediaType.APPLICATION_PDF);
        mediaTypes.put("csv", new MediaType("text", "csv", Charset.forName("utf-8")));
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
        for (PluginWrapper pluginWrapper : plugins) {

            if (pluginWrapper.getPlugin() instanceof CustomPlugin) {
                ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver(pluginWrapper.getPluginClassLoader());
                CustomPlugin plugin = (CustomPlugin) pluginWrapper.getPlugin();

                if (plugin.getLinks() != null) {
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

        for (ClassLoaderTemplateResolver pluginTemplateResolver : pluginTemplateResolvers()) {
            templateEngine.addTemplateResolver(pluginTemplateResolver);
        }

        // add dialect spring security
        templateEngine.addDialect(new SpringSecurityDialect());
        return templateEngine;
    }

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

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver clr = new CookieLocaleResolver();
        clr.setDefaultLocale(new Locale("ru"));
        return clr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        List<PluginWrapper> plugins = pluginManager.getPlugins();
        List<String> messageClassPath = new ArrayList<>();
        messageClassPath.add("classpath:messages");
        for (PluginWrapper pluginWrapper : plugins) {
            if (pluginWrapper.getPlugin() instanceof CustomPlugin) {
                messageClassPath.add("classpath:billing-plugin/messages/billing-plugin");
            }
        }
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(0);
        messageSource.setBasenames(messageClassPath.toArray(new String[messageClassPath.size()]));
        return messageSource;
    }
}