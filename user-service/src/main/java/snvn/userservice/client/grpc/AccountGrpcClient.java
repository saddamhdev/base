package snvn.userservice.client.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import snvn.grpc.AccountServiceGrpc;
import snvn.grpc.AccountRequest;
import snvn.grpc.AccountResponse;

@Service
public class AccountGrpcClient {

    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub accountStub;

    public AccountResponse getAccount(Long userId) {

        AccountRequest request =
                AccountRequest.newBuilder()
                        .setUserId(userId)
                        .build();

        return accountStub.getAccount(request);
    }
}
