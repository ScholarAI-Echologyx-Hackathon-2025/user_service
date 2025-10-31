package org.solace.scholar_ai.user_service.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleVerifierUtil {

    private static final Logger logger = LoggerFactory.getLogger(GoogleVerifierUtil.class);

    @Value("${spring.google.client-id}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    public GoogleVerifierUtil(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    public GoogleVerifierUtil() {
        // Default constructor for Spring
    }

    @PostConstruct
    public void init() throws Exception {
        if (verifier == null) {
            verifier = buildGoogleVerifier();
        }
    }
    
    private GoogleIdTokenVerifier buildGoogleVerifier() throws Exception {
        return new GoogleIdTokenVerifier.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(), 
                        JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            return idToken != null ? idToken.getPayload() : null;
        } catch (Exception e) {
            logger.error("Failed to verify Google ID token", e);
            return null;
        }
    }
}
