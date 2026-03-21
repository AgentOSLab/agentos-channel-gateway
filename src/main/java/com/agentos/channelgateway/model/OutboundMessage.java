package com.agentos.channelgateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Placeholder for channel replies (scaffold). Future: map from Conversation / HITL responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OutboundMessage(
        String channel,
        String externalThreadId,
        String text) {
}
