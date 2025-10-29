package org.solace.scholar_ai.user_service.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

	private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
	private final JwtUtils jwtUtils;

	public void blacklistToken(String token) {
		if (token != null && !token.isEmpty()) {
			blacklistedTokens.add(token);
			log.info("Token blacklisted: {}", maskToken(token));
		}
	}

	public void blacklistTokenFromRequest(HttpServletRequest request) {
		String token = jwtUtils.getJwtFromHeader(request);
		if (token != null) {
			blacklistToken(token);
		}
	}

	public boolean isBlacklisted(String token) {
		return token != null && blacklistedTokens.contains(token);
	}

	public void cleanExpiredTokens() {
		int initialSize = blacklistedTokens.size();
		blacklistedTokens.removeIf(token -> {
			try {
				return isTokenExpired(token);
			} catch (Exception e) {
				log.warn("Error checking token expiry, removing from blacklist: {}", e.getMessage());
				return true;
			}
		});
		int removed = initialSize - blacklistedTokens.size();
		if (removed > 0) {
			log.info("Cleaned {} expired tokens from blacklist", removed);
		}
	}

	private boolean isTokenExpired(String token) {
		try {
			String username = jwtUtils.getUserNameFromJwtToken(token);
			return false;
		} catch (Exception e) {
			return true;
		}
	}

	private String maskToken(String token) {
		if (token == null || token.length() < 10) {
			return "***";
		}
		return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
	}

	public int getBlacklistSize() {
		return blacklistedTokens.size();
	}
}
