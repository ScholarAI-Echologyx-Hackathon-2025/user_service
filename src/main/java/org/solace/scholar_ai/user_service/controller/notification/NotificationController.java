package org.solace.scholar_ai.user_service.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solace.scholar_ai.user_service.dto.response.APIResponse;
import org.solace.scholar_ai.user_service.model.User;
import org.solace.scholar_ai.user_service.repository.UserRepository;
import org.solace.scholar_ai.user_service.service.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Cross-Service Notifications", description = "Endpoints for other services to trigger user notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private static final String USER_NOT_FOUND_ERROR = "User not found";
    private static final String NOTIFICATION_DISPATCHED_MSG = "Notification dispatched";
    
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public record CrossServiceNotificationRequest(
            @NotNull UUID userId, @NotBlank String notificationType, Map<String, Object> templateData) {}

    @Operation(summary = "Send a notification to a user by userId (for internal services)")
    @PostMapping("/send")
    public ResponseEntity<APIResponse<String>> sendNotification(@RequestBody CrossServiceNotificationRequest req) {
        try {
            User user = findUserById(req.userId());
            String recipientName = determineRecipientName(user);
            
            notificationService.sendGenericNotificationToUser(
                    req.userId(), req.notificationType(), req.templateData(), user.getEmail(), recipientName);
            
            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), NOTIFICATION_DISPATCHED_MSG, null));
        } catch (Exception e) {
            log.error("Failed to dispatch notification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }
    
    private User findUserById(UUID userId) {
        return userRepository
                .findWithProfileById(userId)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_ERROR));
    }
    
    private String determineRecipientName(User user) {
        return user.getProfile() != null ? user.getProfile().getFullName() : user.getEmail();
    }
}
