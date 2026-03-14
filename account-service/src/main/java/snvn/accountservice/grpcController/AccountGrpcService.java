package snvn.accountservice.grpcController;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import snvn.grpc.AccountServiceGrpc;
import snvn.grpc.AccountRequest;
import snvn.grpc.AccountResponse;

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
