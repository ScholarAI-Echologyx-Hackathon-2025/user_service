package org.solace.scholar_ai.user_service.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginAttempt {
	private String email;
	private long timestamp;
	private boolean successful;
	private String ipAddress;
}
