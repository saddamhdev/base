package snvn.accountservice.grpcController;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import snvn.grpc.async.AsyncTestServiceGrpc;
import snvn.grpc.async.ChatMessage;
import snvn.grpc.async.StatusRequest;
import snvn.grpc.async.StatusResponse;

@GrpcService
public class AsyncTestServiceImpl extends AsyncTestServiceGrpc.AsyncTestServiceImplBase {

    // ✅ Unary
    @Override
    public void getStatus(StatusRequest request,
                          StreamObserver<StatusResponse> responseObserver) {

        StatusResponse response = StatusResponse.newBuilder()
                .setMessage("User " + request.getUserId() + " OK")
                .setTimestamp(System.currentTimeMillis())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // ✅ Server Streaming
    @Override
    public void streamStatus(StatusRequest request,
                             StreamObserver<StatusResponse> responseObserver) {

        for (int i = 1; i <= 5; i++) {
            StatusResponse response = StatusResponse.newBuilder()
                    .setMessage("Update " + i)
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            responseObserver.onNext(response);

            try { Thread.sleep(500); } catch (Exception ignored) {}
        }

        responseObserver.onCompleted();
    }

    // ✅ Bidirectional Streaming
    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {

        return new StreamObserver<>() {

            @Override
            public void onNext(ChatMessage message) {

                ChatMessage reply = ChatMessage.newBuilder()
                        .setFrom("SERVER")
                        .setText("Echo: " + message.getText())
                        .build();

                responseObserver.onNext(reply);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}