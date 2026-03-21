package com.agentos.channelgateway.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Validates Slack request signatures (<a href="https://api.slack.com/authentication/verifying-requests-from-slack">Slack docs</a>).
 */
@Component
public class SlackSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(SlackSignatureVerifier.class);
    private static final long MAX_SKEW_SECONDS = 60 * 5;

    private final AtomicBoolean unverifiedWarningLogged = new AtomicBoolean(false);
    private final String signingSecret;
    private final boolean requireVerification;

    public SlackSignatureVerifier(
            @Value("${agentos.channel-gateway.slack.signing-secret:}") String signingSecret,
            @Value("${agentos.channel-gateway.webhooks.require-verification:false}") boolean requireVerification) {
        this.signingSecret = signingSecret;
        this.requireVerification = requireVerification;
    }

    /**
     * When no signing secret is configured (local dev), verification is skipped and a warning is logged once.
     */
    public boolean isVerificationConfigured() {
        return signingSecret != null && !signingSecret.isBlank();
    }

    public boolean verify(String timestampHeader, String rawBody, String signatureHeader) {
        if (!isVerificationConfigured()) {
            if (requireVerification) {
                log.warn("Slack signing secret missing while webhooks.require-verification=true — rejecting");
                return false;
            }
            if (unverifiedWarningLogged.compareAndSet(false, true)) {
                log.warn("Slack signing secret not configured — request signatures are NOT verified (dev only)");
            }
            return true;
        }
        if (timestampHeader == null || signatureHeader == null || rawBody == null) {
            return false;
        }
        if (!signatureHeader.startsWith("v0=")) {
            return false;
        }
        try {
            long ts = Long.parseLong(timestampHeader.trim());
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - ts) > MAX_SKEW_SECONDS) {
                log.warn("Rejecting Slack request: timestamp outside allowed skew");
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        try {
            String base = "v0:" + timestampHeader + ":" + rawBody;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(base.getBytes(StandardCharsets.UTF_8));
            byte[] provided = HexFormat.of().parseHex(signatureHeader.substring(3));
            return MessageDigest.isEqual(expected, provided);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.debug("Slack signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
