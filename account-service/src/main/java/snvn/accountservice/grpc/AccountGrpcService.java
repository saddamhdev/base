package snvn.accountservice.grpc;



import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import snvn.common.dto.AccountResponse;

@GrpcService
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    @Override
    public void getAccount(AccountRequest request,
                           StreamObserver<AccountResponse> responseObserver) {

        Long userId = request.getUserId();

        AccountResponse response =
                AccountResponse.newBuilder()
                        .setAccountId(1)
                        .setStatus("ACTIVE")
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
