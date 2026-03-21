package com.agentos.channelgateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Normalized inbound message from an external channel (Slack, WhatsApp, Teams, …).
 * Populated by channel-specific adapters before forwarding to Conversation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InboundMessage(
        String channel,
        String externalMessageId,
        String text,
        String tenantHint,
        JsonNode rawPayload) {
}
