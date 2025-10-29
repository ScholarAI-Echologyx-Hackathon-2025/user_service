package org.solace.scholar_ai.user_service.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoginAttemptService {

	@Value("${security.login.max-attempts:5}")
	private int maxAttempts;

	@Value("${security.login.lockout-duration-minutes:15}")
	private int lockoutDurationMinutes;

	private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
	private final Map<String, Long> lockoutCache = new ConcurrentHashMap<>();

	public void loginSucceeded(String key) {
		attemptsCache.remove(key);
		lockoutCache.remove(key);
	}

	public void loginFailed(String key) {
		int attempts = attemptsCache.getOrDefault(key, 0);
		attempts++;
		attemptsCache.put(key, attempts);

		if (attempts >= maxAttempts) {
			long lockoutUntil = Instant.now().plusSeconds(lockoutDurationMinutes * 60).toEpochMilli();
			lockoutCache.put(key, lockoutUntil);
			log.warn("Account locked due to {} failed login attempts: {}", attempts, key);
		}
	}

	public boolean isBlocked(String key) {
		Long lockoutUntil = lockoutCache.get(key);
		if (lockoutUntil == null) {
			return false;
		}

		if (Instant.now().toEpochMilli() > lockoutUntil) {
			lockoutCache.remove(key);
			attemptsCache.remove(key);
			return false;
		}

		return true;
	}

	public int getAttempts(String key) {
		return attemptsCache.getOrDefault(key, 0);
	}

	public long getRemainingLockoutTime(String key) {
		Long lockoutUntil = lockoutCache.get(key);
		if (lockoutUntil == null) {
			return 0;
		}
		long remaining = lockoutUntil - Instant.now().toEpochMilli();
		return Math.max(0, remaining / 1000);
	}
}
