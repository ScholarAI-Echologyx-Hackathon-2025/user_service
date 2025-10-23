package org.solace.scholar_ai.user_service.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solace.scholar_ai.user_service.dto.auth.AuthResponse;
import org.solace.scholar_ai.user_service.dto.response.APIResponse;
import org.solace.scholar_ai.user_service.service.auth.SocialAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth/social")
@Tag(name = "Social Authentication", description = "Social authentication endpoints for Google and GitHub OAuth")
@RequiredArgsConstructor
public class SocialAuthController {
    private static final Logger logger = LoggerFactory.getLogger(SocialAuthController.class);
    private final SocialAuthService socialAuthService;

	@Operation(summary = "Login with Google OAuth token")
	@PostMapping("/google-login")
	public ResponseEntity<APIResponse<AuthResponse>> loginWithGoogle(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Google OAuth ID token", content = @Content(examples = @ExampleObject(name = "Google ID Token Example",
                                                            value =
                                                                    """
                                        {
                                          "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJKV1QifQ..."
                                        }
                                        """)))
                    @RequestBody
                    Map<String, String> payload,
            HttpServletResponse httpServletResponse) {
        try {
            String idToken = payload.get("idToken");
            logger.info("social-login hits with idToken: '{}'", idToken);

            if (idToken == null || idToken.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), "ID token is missing.", null));
            }

            AuthResponse authResponseFromService = socialAuthService.loginWithGoogle(idToken);

            // Set the refreshToken as an HttpOnly cookie using ResponseCookie
            if (authResponseFromService.getRefreshToken() != null
                    && !authResponseFromService.getRefreshToken().isEmpty()) {
                ResponseCookie refreshCookie = ResponseCookie.from(
                                "refreshToken", authResponseFromService.getRefreshToken())
                        .httpOnly(false) // Allow JavaScript access for debugging
                        .secure(false) // Allow over plain HTTP for development
                        .sameSite("None") // Important: cross-origin cookie
                        .path("/") // Send for all paths
                        .maxAge(Duration.ofDays(7))
                        .build();

                httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

                // Keep refresh token in response body for frontend to set cookie on its domain
                // Don't nullify it - frontend needs it to set cookie on :3000 domain
            } else {
                // This indicates an issue if refresh token rotation/issuance is expected
                logger.warn("Refresh token was not provided by authService.loginWithGoogle() for social login.");
            }

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Google login successful", authResponseFromService));

        } catch (BadCredentialsException e) {
            logger.warn("Google login failed (BadCredentialsException): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.error(
                            HttpStatus.UNAUTHORIZED.value(), "Google login failed: " + e.getMessage(), null));
        } catch (IllegalArgumentException e) { // Catch specific exceptions if id token validation fails
            logger.warn("Google ID token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.error(
                            HttpStatus.UNAUTHORIZED.value(), "Invalid Google ID token: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Unexpected error during Google social login: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error during Google login.", null));
        }
	}

	@Operation(summary = "Login with GitHub OAuth code")
	@PostMapping("/github-login")
	public ResponseEntity<APIResponse<AuthResponse>> loginWithGithub(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "GitHub OAuth authorization code", content = @Content(examples = @ExampleObject(name = "GitHub Authorization Code Example", value =
                                                                    """
                                        {
                                          "code": "abc123def456ghi789"
                                        }
                                        """)))
                    @RequestBody
                    Map<String, String> payload,
            HttpServletResponse httpServletResponse) {
        try {
            logger.info("github-login endpoint hits");
            String code = payload.get("code");

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(APIResponse.error(
                                HttpStatus.BAD_REQUEST.value(), "GitHub authorization code is missing", null));
            }

            AuthResponse authResponse = socialAuthService.loginWithGithub(code);

            if (authResponse.getRefreshToken() != null
                    && !authResponse.getRefreshToken().isEmpty()) {
                ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                        .httpOnly(false) // Allow JavaScript access for debugging
                        .secure(false) // Allow over plain HTTP for development
                        .sameSite("None") // Important: cross-origin cookie
                        .path("/") // Send for all paths
                        .maxAge(Duration.ofDays(7))
                        .build();

                httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
                // Keep refresh token in response body for frontend to set cookie on its domain
                // Don't remove it - frontend needs it to set cookie on :3000 domain
            }

            logger.info("cookie set for github auth");
            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "GitHub login successful", authResponse));

        } catch (BadCredentialsException e) {
            logger.warn("GitHub login failed (BadCredentialsException): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.error(
                            HttpStatus.UNAUTHORIZED.value(), "GitHub login failed: " + e.getMessage(), null));
        } catch (IllegalStateException e) {
            logger.warn("GitHub token exchange failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(
                            HttpStatus.BAD_REQUEST.value(),
                            "Failed to get access token from GitHub: " + e.getMessage(),
                            null));
        } catch (Exception e) {
            logger.error("Unexpected error during GitHub login: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unexpected error during GitHub login: " + e.getMessage(),
                            null));
        }
    }
}
