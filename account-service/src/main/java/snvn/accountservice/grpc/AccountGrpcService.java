package snvn.accountservice.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import snvn.common.grpc.AccountRequest;
import snvn.common.grpc.AccountResponse;
import snvn.common.grpc.AccountServiceGrpc;

@GrpcService
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    @Override
    public void getAccount(AccountRequest request,
                           StreamObserver<AccountResponse> responseObserver) {

        AccountResponse response =
                AccountResponse.newBuilder()
                        .setAccountId(1)
                        .setStatus("ACTIVE")
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
