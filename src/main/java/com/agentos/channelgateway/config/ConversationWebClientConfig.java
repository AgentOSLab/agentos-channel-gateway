package com.agentos.channelgateway.config;

import com.agentos.common.alert.AlertClient;
import com.agentos.common.alert.DependencyAlertFilter;
import com.agentos.common.auth.ServiceTokenExchangeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Outbound HTTP to Conversation. Service JWT is attached when {@link ServiceTokenExchangeFilter} is available.
 */
@Configuration
public class ConversationWebClientConfig {

    private static final String SERVICE_NAME = "channel-gateway";

    @Value("${agentos.services.conversation-url}")
    private String conversationBaseUrl;

    @Autowired(required = false)
    private ServiceTokenExchangeFilter serviceTokenExchangeFilter;

    @Autowired(required = false)
    private AlertClient alertClient;

    @Bean
    public WebClient conversationWebClient(WebClient.Builder builder) {
        WebClient.Builder b = builder.clone().baseUrl(conversationBaseUrl);
        if (serviceTokenExchangeFilter != null) {
            b.filter(serviceTokenExchangeFilter);
        }
        if (alertClient != null) {
            b.filter(new DependencyAlertFilter(alertClient, SERVICE_NAME));
        }
        return b.build();
    }
}
