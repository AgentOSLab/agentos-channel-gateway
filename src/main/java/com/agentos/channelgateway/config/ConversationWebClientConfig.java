package com.agentos.channelgateway.config;

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

    @Value("${agentos.services.conversation-url}")
    private String conversationBaseUrl;

    @Autowired(required = false)
    private ServiceTokenExchangeFilter serviceTokenExchangeFilter;

    @Bean
    public WebClient conversationWebClient(WebClient.Builder builder) {
        WebClient.Builder b = builder.clone().baseUrl(conversationBaseUrl);
        if (serviceTokenExchangeFilter != null) {
            b.filter(serviceTokenExchangeFilter);
        }
        return b.build();
    }
}
