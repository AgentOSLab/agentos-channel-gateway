package com.agentos.channelgateway.service;

import com.agentos.channelgateway.model.InboundMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Forwards normalized inbound events toward Conversation. The internal ingress route is not yet
 * implemented server-side; failures are tolerated so webhooks remain testable in scaffold mode.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelIngressService {

    private final org.springframework.web.reactive.function.client.WebClient conversationWebClient;

    /**
     * POST to Conversation internal ingress. Returns when the HTTP exchange completes or when a
     * non-fatal error is swallowed (scaffold).
     */
    public Mono<Void> forwardToConversation(InboundMessage message) {
        return conversationWebClient.post()
                .uri("/api/internal/v1/channel/inbound")
                .bodyValue(message)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.debug("Conversation channel ingress HTTP {}", r.getStatusCode()))
                .then()
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND || ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        log.debug("Conversation channel ingress not ready ({}): {}", ex.getStatusCode(), ex.getMessage());
                        return Mono.empty();
                    }
                    return Mono.error(ex);
                })
                .onErrorResume(e -> {
                    log.warn("Conversation channel ingress error: {}", e.toString());
                    return Mono.empty();
                });
    }

    public Mono<Void> requireJsonBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty body"));
        }
        return Mono.empty();
    }
}
