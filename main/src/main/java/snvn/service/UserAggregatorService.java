package snvn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import snvn.exception.ReactiveErrorHandler;
import snvn.config.ServiceClientConfiguration.WebClientRegistry;
import snvn.dto.CreateUserRequest;
import snvn.dto.CreateUserResponse;
import snvn.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for aggregating user-related operations across multiple microservices
 */
@Service
public class UserAggregatorService {

    private static final Logger logger = LoggerFactory.getLogger(UserAggregatorService.class);

    private final WebClientRegistry webClientRegistry;

    public UserAggregatorService(WebClientRegistry webClientRegistry) {
        this.webClientRegistry = webClientRegistry;
    }

    // Convenience getters for WebClients
    private WebClient userServiceClient() {
        return webClientRegistry.getUserServiceClient();
    }

    private WebClient authServiceClient() {
        return webClientRegistry.getAuthServiceClient();
    }

    private WebClient accountServiceClient() {
        return webClientRegistry.getAccountServiceClient();
    }

    private WebClient notificationServiceClient() {
        return webClientRegistry.getNotificationServiceClient();
    }

    private WebClient auditServiceClient() {
        return webClientRegistry.getAuditServiceClient();
    }

    private WebClient kafkaServiceClient() {
        return webClientRegistry.getKafkaServiceClient();
    }

    /**
     * Orchestrate user creation flow across multiple services
     */
    public Mono<CreateUserResponse> createUser(CreateUserRequest request) {
        logger.info("Starting user creation orchestration for username: {}", request.getUsername());

        // Capture MDC context for reactive chain
        final Map<String, String> mdcContext = ReactiveErrorHandler.captureMdcContext();
        CreateUserResponse response = new CreateUserResponse();

        // Step 1: Create user in user-service
        return createUserInUserService(request, mdcContext)
            .flatMap(user -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.info("User created with ID: {}", user.getId());
                response.setUserId(user.getId());
                response.setUsername(user.getUsername());
                response.setEmail(user.getEmail());
                response.setFirstName(user.getFirstName());
                response.setLastName(user.getLastName());
                response.setRole(user.getRole());

                // Step 2: Create auth credentials (parallel with account creation)
                Mono<Map<String, Object>> authMono = createAuthCredentials(user, mdcContext);

                // Step 3: Create default account
                Mono<Map<String, Object>> accountMono = createDefaultAccount(user, mdcContext);

                // Step 4: Combine results
                return Mono.zip(authMono, accountMono)
                    .map(tuple -> {
                        ReactiveErrorHandler.restoreMdcContext(mdcContext);
                        Map<String, Object> authResult = tuple.getT1();
                        Map<String, Object> accountResult = tuple.getT2();

                        // Set auth info
                        response.setAuthToken((String) authResult.get("token"));

                        // Set account info
                        response.setAccountId(((Number) accountResult.get("id")).longValue());
                        response.setAccountNumber((String) accountResult.get("accountNumber"));
                        response.setInitialBalance(((Number) accountResult.get("balance")).doubleValue());

                        return user;
                    });
            })
            .flatMap(user -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);

                // Step 5: Publish event to Kafka (async)
                publishUserCreatedEvent(user, mdcContext).subscribe();

                // Step 6: Send welcome notification (async)
                sendWelcomeNotification(user, mdcContext).subscribe(
                    result -> {
                        response.setNotificationSent(true);
                        logger.info("Welcome notification sent for user: {}", user.getId());
                    },
                    error -> {
                        response.setNotificationSent(false);
                        ReactiveErrorHandler.restoreMdcContext(mdcContext);
                        logger.error("Failed to send notification", error);
                    }
                );

                // Step 7: Log to audit service (async)
                logToAuditService(user, mdcContext).subscribe(
                    result -> {
                        response.setAuditLogged(true);
                        logger.info("Audit log created for user: {}", user.getId());
                    },
                    error -> {
                        response.setAuditLogged(false);
                        ReactiveErrorHandler.restoreMdcContext(mdcContext);
                        logger.error("Failed to log audit", error);
                    }
                );

                response.setMessage("User created successfully");
                return Mono.just(response);
            })
            .doOnError(error -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.error("Error in user creation orchestration", error);
            })
            .onErrorMap(ReactiveErrorHandler.wrapWithTraceContext("user-service"));
    }

    /**
     * Create user in user-service
     */
    private Mono<User> createUserInUserService(CreateUserRequest request, Map<String, String> mdcContext) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole() != null ? request.getRole() : "USER");

        return userServiceClient().post()
            .uri("/api/users")
            .bodyValue(user)
            .retrieve()
            .bodyToMono(User.class)
            .doOnSubscribe(s -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
            .doOnSuccess(createdUser -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.info("User created in user-service: {}", createdUser.getId());
            })
            .doOnError(error -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.error("Failed to create user in user-service", error);
            })
            .onErrorMap(ReactiveErrorHandler.wrapWithTraceContext("user-service"));
    }

    /**
     * Create auth credentials in auth-service
     */
    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> createAuthCredentials(User user, Map<String, String> mdcContext) {
        Map<String, String> authRequest = new HashMap<>();
        authRequest.put("username", user.getUsername());
        authRequest.put("password", user.getPassword());
        authRequest.put("userId", String.valueOf(user.getId()));

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("token", "TOKEN_CREATION_FAILED");

        return authServiceClient().post()
            .uri("/api/auth/register")
            .bodyValue(authRequest)
            .retrieve()
            .bodyToMono(Map.class)
            .map(result -> (Map<String, Object>) result)
            .doOnSubscribe(s -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
            .doOnSuccess(result -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.info("Auth credentials created for user: {}", user.getId());
            })
            .doOnError(error -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.error("Failed to create auth credentials", error);
            })
            .onErrorReturn(fallback);
    }

    /**
     * Create default account in account-service
     */
    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> createDefaultAccount(User user, Map<String, String> mdcContext) {
        Map<String, Object> accountRequest = new HashMap<>();
        accountRequest.put("userId", user.getId());
        accountRequest.put("accountType", "CHECKING");
        accountRequest.put("balance", 0.0);
        accountRequest.put("accountNumber", "ACC" + System.currentTimeMillis());

        return accountServiceClient().post()
            .uri("/api/accounts")
            .bodyValue(accountRequest)
            .retrieve()
            .bodyToMono(Map.class)
            .map(result -> (Map<String, Object>) result)
            .doOnSubscribe(s -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
            .doOnSuccess(result -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.info("Default account created for user: {}", user.getId());
            })
            .doOnError(error -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.error("Failed to create default account", error);
            })
            .onErrorReturn(new HashMap<String, Object>() {{
                put("id", 0L);
                put("accountNumber", "ACCOUNT_CREATION_FAILED");
                put("balance", 0.0);
            }});
    }

    /**
     * Publish user created event to Kafka
     */
    private Mono<Void> publishUserCreatedEvent(User user, Map<String, String> mdcContext) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_CREATED");
        event.put("userId", user.getId());
        event.put("username", user.getUsername());
        event.put("email", user.getEmail());
        event.put("timestamp", System.currentTimeMillis());

        return kafkaServiceClient().post()
            .uri("/api/kafka/events")
            .bodyValue(event)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSubscribe(s -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
            .doOnSuccess(result -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.info("User created event published to Kafka for user: {}", user.getId());
            })
            .doOnError(error -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.error("Failed to publish event to Kafka", error);
            })
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Send welcome notification
     */
    private Mono<Void> sendWelcomeNotification(User user, Map<String, String> mdcContext) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", user.getId());
        notification.put("email", user.getEmail());
        notification.put("type", "WELCOME_EMAIL");
        notification.put("subject", "Welcome to Our Platform!");
        notification.put("message", String.format(
            "Hello %s %s,\n\nWelcome to our platform! Your account has been created successfully.",
            user.getFirstName(), user.getLastName()
        ));

        return notificationServiceClient().post()
            .uri("/api/notifications/send")
            .bodyValue(notification)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSubscribe(s -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
            .doOnSuccess(result -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.info("Welcome notification sent to user: {}", user.getId());
            })
            .doOnError(error -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.error("Failed to send welcome notification", error);
            })
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Log user creation to audit service
     */
    private Mono<Void> logToAuditService(User user, Map<String, String> mdcContext) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("eventType", "USER_CREATED");
        auditLog.put("userId", user.getId());
        auditLog.put("username", user.getUsername());
        auditLog.put("action", "CREATE_USER");
        auditLog.put("details", "New user account created");
        auditLog.put("timestamp", System.currentTimeMillis());

        return auditServiceClient().post()
            .uri("/api/audit/logs")
            .bodyValue(auditLog)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSubscribe(s -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
            .doOnSuccess(result -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.info("Audit log created for user: {}", user.getId());
            })
            .doOnError(error -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                logger.error("Failed to create audit log", error);
            })
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Get user by ID (aggregated with account info)
     */
    public Mono<Map<String, Object>> getUserProfile(Long userId) {
        logger.info("Fetching user profile for ID: {}", userId);
        final Map<String, String> mdcContext = ReactiveErrorHandler.captureMdcContext();

        Mono<User> userMono = userServiceClient().get()
            .uri("/api/users/" + userId)
            .retrieve()
            .bodyToMono(User.class)
            .doOnSubscribe(s -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
            .onErrorMap(ReactiveErrorHandler.wrapWithTraceContext("user-service"));

        Mono<Map> accountsMono = accountServiceClient().get()
            .uri("/api/accounts/user/" + userId)
            .retrieve()
            .bodyToMono(Map.class)
            .doOnSubscribe(s -> ReactiveErrorHandler.restoreMdcContext(mdcContext))
            .onErrorReturn(Map.of());

        return Mono.zip(userMono, accountsMono)
            .map(tuple -> {
                ReactiveErrorHandler.restoreMdcContext(mdcContext);
                Map<String, Object> profile = new HashMap<>();
                User user = tuple.getT1();
                Map accounts = tuple.getT2();

                profile.put("user", user);
                profile.put("accounts", accounts);

                return profile;
            });
    }
}
