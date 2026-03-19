package snvn.userservice.client.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import snvn.grpc.AccountByEmailRequest;
import snvn.grpc.AccountServiceGrpc;
import snvn.grpc.AccountRequest;
import snvn.grpc.AccountResponse;
import snvn.grpc.async.AsyncTestServiceGrpc;
import snvn.grpc.async.ChatMessage;
import snvn.grpc.async.StatusRequest;
import snvn.grpc.async.StatusResponse;

@Service
public class AccountGrpcClient {

    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub accountStub;

    @GrpcClient("account-service")
    private AsyncTestServiceGrpc.AsyncTestServiceStub asyncStub;


    public AccountResponse getAccount(Long userId) {

        AccountRequest request =
                AccountRequest.newBuilder()
                        .setUserId(userId)
                        .build();

        return accountStub.getAccount(request);
    }

    // ✅ NEW METHOD CALL
    public AccountResponse getAccountByEmail(String email) {
        AccountByEmailRequest request = AccountByEmailRequest.newBuilder()
                .setEmail(email)
                .build();

        return accountStub.getAccountByEmail(request);
    }

    public void testUnaryAsync() {

        StatusRequest request = StatusRequest.newBuilder()
                .setUserId(1)
                .build();

        asyncStub.getStatus(request, new StreamObserver<>() {

            @Override
            public void onNext(StatusResponse response) {
                System.out.println("Response: " + response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Done");
            }
        });
    }

    public void testStreaming() {

        StatusRequest request = StatusRequest.newBuilder()
                .setUserId(1)
                .build();

        asyncStub.streamStatus(request, new StreamObserver<>() {

            @Override
            public void onNext(StatusResponse response) {
                System.out.println("Stream: " + response.getMessage());
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                System.out.println("Stream completed");
            }
        });
    }

    public void testChat() {

        StreamObserver<ChatMessage> requestObserver =
                asyncStub.chat(new StreamObserver<>() {

                    @Override
                    public void onNext(ChatMessage response) {
                        System.out.println("Server: " + response.getText());
                    }

                    @Override
                    public void onError(Throwable t) {}

                    @Override
                    public void onCompleted() {
                        System.out.println("Chat ended");
                    }
                });

        // send messages
        requestObserver.onNext(ChatMessage.newBuilder()
                .setFrom("CLIENT")
                .setText("Hello")
                .build());

        requestObserver.onNext(ChatMessage.newBuilder()
                .setFrom("CLIENT")
                .setText("How are you?")
                .build());

        requestObserver.onCompleted();
    }
}
