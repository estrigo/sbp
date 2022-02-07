package kz.spt.app;

import kz.spt.app.auth.DefaultUrlAuthenticationSuccessHandler;
import kz.spt.app.config.CorsAllowedOrigins;
import kz.spt.lib.plugin.CustomPlugin;
import kz.spt.app.service.SpringDataUserDetailsService;
import lombok.extern.java.Log;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Log
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final PluginManager pluginManager;
    private CorsAllowedOrigins corsAllowedOrigins;

    public SecurityConfig(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SpringDataUserDetailsService customUserDetailsService() {
        return new SpringDataUserDetailsService();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, CorsAllowedOrigins corsAllowedOrigins) throws Exception {
        auth.userDetailsService(customUserDetailsService()).passwordEncoder(passwordEncoder());
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/users/delete/**").hasRole("ADMIN")
                .antMatchers("/arm/**").fullyAuthenticated()
                .antMatchers( "/events/**", "/journal/**", "/arm/**").hasAnyRole("AUDIT", "ADMIN", "MANAGER", "SUPERADMIN", "OPERATOR", "OPERATOR_NO_REVENUE_SHARE")
                .antMatchers( "/customers/**", "/register/**", "/cars/**", "/parking/**", "/customer/**").hasAnyRole("AUDIT", "ADMIN", "MANAGER", "SUPERADMIN")
                .antMatchers("/customer/edit/**", "/users/**", "/cars/edit/**","/parking/edit/**","/parking/details/**").hasAnyRole( "ADMIN", "MANAGER", "SUPERADMIN")
                .antMatchers("/parking/**").hasAnyRole("ADMIN", "SUPERADMIN", "OPERATOR_NO_REVENUE_SHARE");

        List<PluginWrapper> plugins = pluginManager.getPlugins();

        for (PluginWrapper pluginWrapper : plugins) {
            if (pluginWrapper.getPlugin() instanceof CustomPlugin) {
                CustomPlugin plugin = (CustomPlugin) pluginWrapper.getPlugin();

                if (plugin.getTemplateUrl() != null) {
                    http.authorizeRequests().antMatchers("/" + plugin.getTemplateUrl() + "/**").fullyAuthenticated();
                }

                if (plugin.getLinks() != null) {
                    for (Map<String, Object> link : plugin.getLinks()) {
                        if (link.containsKey("url") && link.containsKey("role")) {
                            http.authorizeRequests().antMatchers("/" + link.get("url")).hasRole((String) link.get("role"));
                        }
                        if (link.containsKey("subMenus")) {
                            List<Map<String, Object>> subMenus = (List<Map<String, Object>>) link.get("subMenus");
                            for (Map<String, Object> subLink : subMenus) {
                                if (subLink.containsKey("url") && subLink.containsKey("role")) {
                                    http.authorizeRequests().antMatchers("/" + subLink.get("url")).hasRole((String) subLink.get("role"));
                                }
                            }
                        }
                    }
                }
            }
        }

        http.authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .formLogin().loginPage("/login").successHandler(defaultAuthenticationSuccessHandler())
                .and()
                .logout().logoutSuccessUrl("/login").permitAll()
                .and()
                .exceptionHandling().accessDeniedPage("/403");

        http.csrf().disable();
        http.cors();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if(corsAllowedOrigins.getOrigins() != null && corsAllowedOrigins.getOrigins().size() > 0){
            log.info("corsAllowedOrigins.getOrigins().size(): " + corsAllowedOrigins.getOrigins().size());
            for(String origin: corsAllowedOrigins.getOrigins()){
                log.info("origin: " + origin);
                configuration.addAllowedOrigin(origin);
            }
        }
        configuration.setAllowedMethods(Arrays.asList("GET","POST"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public AuthenticationSuccessHandler defaultAuthenticationSuccessHandler(){
        return new DefaultUrlAuthenticationSuccessHandler();
    }
}
