package org.solace.scholar_ai.user_service.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solace.scholar_ai.user_service.dto.response.APIResponse;
import org.solace.scholar_ai.user_service.dto.user.UserProfileDTO;
import org.solace.scholar_ai.user_service.dto.user.UserProfileResponseDTO;
import org.solace.scholar_ai.user_service.model.User;
import org.solace.scholar_ai.user_service.repository.UserRepository;
import org.solace.scholar_ai.user_service.service.user.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "User Profile", description = "User profile management endpoints for viewing and updating user information")
@RequiredArgsConstructor
public class UserProfileController {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);
    private final UserProfileService userProfileService;
    private final UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

	@SecurityRequirement(name = "jwtAuth")
	@Operation(summary = "Get user profile")
	@GetMapping
	public ResponseEntity<APIResponse<UserProfileResponseDTO>> getProfile(Principal principal) {
        try {
            String email = principal.getName();
            logger.info("Get profile endpoint hit with email: {}", email);
            UserProfileResponseDTO userProfile = userProfileService.getProfileByEmail(email);
            logger.info("Fetched user profile details: {}", objectMapper.writeValueAsString(userProfile));

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Profile fetched successfully", userProfile));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        }
	}

	@SecurityRequirement(name = "jwtAuth")
	@Operation(summary = "Update user profile")
	@PatchMapping
	public ResponseEntity<APIResponse<UserProfileResponseDTO>> updateProfile(
			Principal principal, @Valid @RequestBody UserProfileDTO userProfileDTO) {
        try {
            String email = principal.getName();
            logger.info("Update profile endpoint hit with email: {}", email);

            User user = userRepository
                    .findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

            UserProfileResponseDTO updatedProfile = userProfileService.updateProfile(user.getId(), userProfileDTO);
            logger.info("Updated user profile details: {}", objectMapper.writeValueAsString(updatedProfile));

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Profile updated successfully", updatedProfile));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
	}

	@SecurityRequirement(name = "jwtAuth")
	@Operation(summary = "Upload avatar image")
	@PostMapping("/avatar")
	public ResponseEntity<APIResponse<Map<String, String>>> uploadAvatar(
			Principal principal,
			@Parameter(description = "Avatar image file") @RequestParam("avatar") MultipartFile file)
			throws IOException {
        try {
            String email = principal.getName();
            logger.info("Upload avatar endpoint hit with email: {}", email);

            User user = userRepository
                    .findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

            String avatarUrl = uploadToStorage(file);
            UserProfileResponseDTO updatedProfile = userProfileService.updateAvatarUrl(user.getId(), avatarUrl);

            Map<String, String> response = Map.of("avatarUrl", avatarUrl);

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Avatar uploaded successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
	}

	@SecurityRequirement(name = "jwtAuth")
	@Operation(summary = "Delete avatar image")
	@DeleteMapping("/avatar")
	public ResponseEntity<APIResponse<String>> deleteAvatar(Principal principal) {
        try {
            String email = principal.getName();
            logger.info("Delete avatar endpoint hit with email: {}", email);

            User user = userRepository
                    .findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

            userProfileService.deleteAvatarUrl(user.getId());

            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Avatar deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    private String uploadToStorage(MultipartFile file) {
        // TODO: Implement actual file upload logic
        // This is a placeholder - you would typically upload to a cloud storage service
        // like AWS S3, Google Cloud Storage, or Azure Blob Storage

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // For now, return a placeholder URL
        // In a real implementation, you would:
        // 1. Validate file type and size
        // 2. Upload to cloud storage
        // 3. Return the actual URL
        return "https://placeholder.com/avatar/" + UUID.randomUUID() + ".jpg";
    }
}
