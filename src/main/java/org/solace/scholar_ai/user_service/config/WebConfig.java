package org.solace.scholar_ai.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the application.
 * Configures CORS, content negotiation, and other web-related settings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS is handled by the API Gateway.
     * No CORS configuration needed here.
     */

    /**
     * Provides a RestTemplate bean for making HTTP requests.
     * Used by SocialAuthService for OAuth provider API calls.
     *
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
