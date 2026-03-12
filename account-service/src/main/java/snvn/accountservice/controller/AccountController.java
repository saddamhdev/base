package snvn.accountservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import snvn.accountservice.dto.AccountResponse;

/**
 * Account Controller for handling account requests
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Account Service is running");
    }
    @GetMapping("/{userId}")

    public AccountResponse getAccount(@PathVariable Long userId) {

        AccountResponse response = new AccountResponse();
        response.setId(1L);
        response.setUserId(userId);
        response.setBalance(5000.0);

        return response;
    }

}

