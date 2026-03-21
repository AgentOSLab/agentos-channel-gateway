package com.agentos.channelgateway.api;

import com.agentos.channelgateway.model.InboundMessage;
import com.agentos.channelgateway.service.ChannelIngressService;
import com.agentos.channelgateway.slack.SlackSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * External webhook entry points (scaffold). Protocol-specific validation is minimal; full channel UX is future work.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelWebhookController {

    private final ChannelIngressService ingressService;
    private final SlackSignatureVerifier slackSignatureVerifier;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/slack/events", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<JsonNode>> slackEvents(
            @RequestHeader(value = "X-Slack-Signature", required = false) String slackSignature,
            @RequestHeader(value = "X-Slack-Request-Timestamp", required = false) String slackTimestamp,
            @RequestBody String rawBody) {

        return ingressService.requireJsonBody(rawBody).then(Mono.defer(() -> {
            if (!slackSignatureVerifier.verify(slackTimestamp, rawBody, slackSignature)) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            try {
                JsonNode root = objectMapper.readTree(rawBody);
                String type = root.path("type").asText("");
                if ("url_verification".equals(type)) {
                    String challenge = root.path("challenge").asText("");
                    return Mono.just(ResponseEntity.ok(objectMapper.createObjectNode().put("challenge", challenge)));
                }
                InboundMessage msg = new InboundMessage(
                        "slack",
                        root.path("event_id").asText(UUID.randomUUID().toString()),
                        root.path("event").path("text").asText(null),
                        null,
                        root);
                return ingressService.forwardToConversation(msg)
                        .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).body(root));
            } catch (Exception e) {
                log.warn("Slack payload parse error: {}", e.getMessage());
                return Mono.just(ResponseEntity.badRequest().build());
            }
        }));
    }

    /**
     * WhatsApp Cloud API webhook (scaffold). Signature verification: TODO — use {@code X-Hub-Signature-256} with app secret.
     */
    @PostMapping(value = "/whatsapp/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> whatsappWebhook(@RequestBody String rawBody) {
        log.debug("WhatsApp webhook received ({} bytes)", rawBody != null ? rawBody.length() : 0);
        // TODO: verify Meta X-Hub-Signature-256 when WHATSAPP_APP_SECRET is configured
        InboundMessage msg = new InboundMessage("whatsapp", UUID.randomUUID().toString(), null, null, null);
        return ingressService.forwardToConversation(msg)
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).build());
    }

    /**
     * Microsoft Teams Bot Framework messaging endpoint (scaffold).
     */
    @PostMapping(value = "/teams/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> teamsMessages(@RequestBody String rawBody) {
        log.debug("Teams webhook received ({} bytes)", rawBody != null ? rawBody.length() : 0);
        // TODO: validate JWT from Bot Framework when configured
        InboundMessage msg = new InboundMessage("teams", UUID.randomUUID().toString(), null, null, null);
        return ingressService.forwardToConversation(msg)
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).build());
    }
}
