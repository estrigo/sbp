package kz.spt.app;

import kz.spt.lib.plugin.CustomPlugin;
import kz.spt.app.service.SpringDataUserDetailsService;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final PluginManager pluginManager;

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
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService()).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/admin/**", "/user/delete/**").hasRole("ADMIN")
                .antMatchers("/pdf-generator", "/search/**", "/customer/**", "/user/edit/**", "/register/**", "/user/list",
                        "/contract/**", "/cars/**", "/parking/**", "/arm/**", "/events/**", "/journal/**")
                .hasAnyRole( "ADMIN", "USER", "MANAGER", "OWNER");

        List<PluginWrapper> plugins = pluginManager.getPlugins();

        for(PluginWrapper pluginWrapper: plugins) {
            if (pluginWrapper.getPlugin() instanceof CustomPlugin) {
                CustomPlugin plugin = (CustomPlugin) pluginWrapper.getPlugin();

                if(plugin.getTemplateUrl() != null){
                    http.authorizeRequests().antMatchers("/" + plugin.getTemplateUrl() + "/**").fullyAuthenticated();
                }

                if (plugin.getLinks() != null) {
                    for(Map<String,Object> link: plugin.getLinks()){
                        if(link.containsKey("url") && link.containsKey("role")){
                            http.authorizeRequests().antMatchers("/" + link.get("url")).hasRole((String) link.get("role"));
                        }
                        if(link.containsKey("subMenus")){
                            List<Map<String, Object>> subMenus = (List<Map<String, Object>>) link.get("subMenus");
                            for(Map<String,Object> subLink: subMenus){
                                if(subLink.containsKey("url") && subLink.containsKey("role")){
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
                .formLogin().loginPage("/login").permitAll()
                .and()
                .logout().logoutSuccessUrl("/login").permitAll()
                .and()
                .exceptionHandling().accessDeniedPage("/403");

        http.csrf().disable();
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
