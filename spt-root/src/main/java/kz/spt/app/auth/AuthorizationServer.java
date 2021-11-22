package kz.spt.app.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

import java.util.Arrays;

@Log
@Configuration
@EnableAuthorizationServer
public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PluginService pluginService;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .authenticationManager(authenticationManager)
                .pathMapping("/oauth/token", "/user/ext_login");
    }

    public ClientDetailsService clientDetailsService() {
        return new ClientDetailsService() {
            @Override
            public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {

                BaseClientDetails details = new BaseClientDetails();
                details.setClientId(clientId);
                details.setAuthorizedGrantTypes(Arrays.asList("client_credentials"));
                details.setScope(Arrays.asList("payment"));
                details.setAccessTokenValiditySeconds(10800);

                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                if(billingPluginRegister != null){
                    ObjectMapper objectMapper = new ObjectMapper();
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("client_id", clientId);
                    node.put("command", "getPasswordHash");

                    try {
                        JsonNode result = billingPluginRegister.execute(node);
                        if(result.get("passwordHash") != null){
                            details.setClientSecret(result.get("passwordHash").textValue());
                            return details;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(clientDetailsService());
    }
}