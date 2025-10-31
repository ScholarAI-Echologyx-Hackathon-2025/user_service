package org.solace.scholar_ai.user_service.controller;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    private static final String TEST_KEY = "health_check_test";
    private static final String TEST_VALUE = "test_value";
    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    
    private final RedisTemplate<String, String> redisTemplate;

    public HealthController(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> checkRedisHealth() {
        try {
            validateRedisConnection();
            logger.info("Redis health check passed");
            return ResponseEntity.ok(buildHealthResponse(STATUS_UP, "Redis is healthy and responding"));
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            return ResponseEntity
                    .status(503)
                    .body(buildHealthResponse(STATUS_DOWN, "Redis connection failed: " + e.getMessage()));
        }
    }
    
    private void validateRedisConnection() {
        redisTemplate.opsForValue().set(TEST_KEY, TEST_VALUE);
        String retrievedValue = redisTemplate.opsForValue().get(TEST_KEY);
        redisTemplate.delete(TEST_KEY);

        if (!TEST_VALUE.equals(retrievedValue)) {
            throw new IllegalStateException("Value mismatch - Redis not responding correctly");
        }
    }
    
    private Map<String, Object> buildHealthResponse(String status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put(STATUS_KEY, status);
        response.put(MESSAGE_KEY, message);
        response.put(TIMESTAMP_KEY, System.currentTimeMillis());
        return response;
    }
}
