package snvn.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import snvn.userservice.dto.AccountResponse;

@FeignClient(
        name = "account-service",
        path = "/api/accounts"
)
public interface AccountClient {

    @GetMapping("/{userId}")
    AccountResponse getAccount(@PathVariable Long userId);

}