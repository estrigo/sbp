package kz.spt.app.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.utils.StaticValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.*;

@Log
@Configuration
@EnableAuthorizationServer
@RequiredArgsConstructor
public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {


    private final AuthenticationManager authenticationManager;

    private final PluginService pluginService;

    @Value("${admin-place.pass}")
    private String adminPass;
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.pathMapping("/oauth/token", "/user/ext_login");
        endpoints.authenticationManager(authenticationManager);
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
                if (billingPluginRegister != null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("client_id", clientId);
                    node.put("command", "getPasswordHash");

                    try {
                        JsonNode result = billingPluginRegister.execute(node);
                        if (result.get("passwordHash") != null) {
                            details.setClientSecret(result.get("passwordHash").textValue());
                            return details;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    details = new BaseClientDetails();
                    details.setClientId(clientId);
                    details.setAuthorizedGrantTypes(List.of("client_credentials"));
                    details.setScope(List.of("admin-place"));
                    details.setAccessTokenValiditySeconds(10800);
                    details.setClientSecret(adminPass);
                    return details;
                } catch (Exception e) {
                    e.printStackTrace();
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