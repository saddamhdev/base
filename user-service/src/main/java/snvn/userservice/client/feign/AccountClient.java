package snvn.userservice.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import snvn.common.dto.AccountResponse;

@FeignClient(
        name = "${services.account-service.name}",
        url = "${services.account-service.base-url}",
        path = "${services.account-service.endpoints.account}")

public interface AccountClient {

    @GetMapping("/{userId}")
    AccountResponse getAccount(@PathVariable("userId") Long userId);

}