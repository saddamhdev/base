package snvn.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import snvn.common.dto.AccountResponse;

@FeignClient(
        name = "account-service",
        url = "${services.account-service.url:http://localhost:8095}",
        path = "/api/accounts"
)
public interface AccountClient {

    @GetMapping("/{userId}")
    AccountResponse getAccount(@PathVariable("userId") Long userId);
}