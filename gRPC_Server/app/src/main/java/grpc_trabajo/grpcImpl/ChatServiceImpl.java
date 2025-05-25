package grpc_trabajo.grpcImpl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.proto.services.ChatMessage;
import com.example.proto.services.ChatServiceGrpc;
import io.grpc.stub.StreamObserver;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
    private final List<StreamObserver<ChatMessage>> clients = new CopyOnWriteArrayList<>();

    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
        clients.add(responseObserver);
        return new StreamObserver<>() {
            @Override
            public void onNext(ChatMessage msg) {
                for (StreamObserver<ChatMessage> client : clients) {
                    client.onNext(msg);
                }
            }

            @Override
            public void onError(Throwable t) {
                clients.remove(responseObserver);
            }

            @Override
            public void onCompleted() {
                clients.remove(responseObserver);
                responseObserver.onCompleted();
            }
        };
    }

}
