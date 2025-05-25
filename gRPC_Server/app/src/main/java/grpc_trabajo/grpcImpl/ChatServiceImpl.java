package grpc_trabajo.grpcImpl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.proto.services.ChatMessage;
import com.example.proto.services.Room;
import com.google.protobuf.Empty;
import com.example.proto.services.ChatServiceGrpc;
import com.example.proto.services.ConnectToRoomRequest;
import com.example.proto.services.ConnectToRoomResponse;
import com.example.proto.services.GetRoomsResponse;

import io.grpc.stub.StreamObserver;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
    private final List<StreamObserver<ChatMessage>> clients = new CopyOnWriteArrayList<>();
    private final List<Room> rooms = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<Integer, CopyOnWriteArrayList<StreamObserver<ChatMessage>>> roomObservers = new ConcurrentHashMap<>();;

    public ChatServiceImpl() {
        // Iniciamos cuartos por defecto
        Room room1 = Room.newBuilder().setName("GamingRoom").setDescription("Sala de gaming").setId(1).build();
        Room room2 = Room.newBuilder().setName("StudyRoom").setDescription("Sala de estudio").setId(2).build();
        rooms.add(room1);
        rooms.add(room2);
    }
    
    @Override
    public void getRooms(Empty request,StreamObserver<GetRoomsResponse> responseObserver) {
        GetRoomsResponse.Builder responseBuilder = GetRoomsResponse.newBuilder(); //al haberlo definido con repeated, se puede usar el builder para generar la respuesta
        for (Room room : rooms) {
            responseBuilder.addRooms(room); //agregamos cada sala a la respuesta
        }
        GetRoomsResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void connectToRoom(ConnectToRoomRequest request, StreamObserver<ConnectToRoomResponse> responseObserver) {
        int roomId = request.getRoom();
        Room room = rooms.stream()
                .filter(r -> r.getId() == roomId)
                .findFirst()
                .orElse(null);

        if (room != null) {
            ConnectToRoomResponse response = ConnectToRoomResponse.newBuilder()
                    .setRoom(room)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(new Throwable("Room not found"));
        }
    }


    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
        return new StreamObserver<ChatMessage>() {
            int roomId = -1;
            
            @Override
            public void onNext(ChatMessage msg) {
                // If it's the first message, subscribe the client's observer to the specified room.
                if (roomId == -1 && msg.hasRoom()) {
                    roomId = msg.getRoom().getId();
                    roomObservers.computeIfAbsent(roomId, r -> new CopyOnWriteArrayList<>()).add(responseObserver);
                }
                // Envia mensajes a todos los clientes conectados a la sala.
                CopyOnWriteArrayList<StreamObserver<ChatMessage>> observers = roomObservers.get(roomId);
                if (observers != null) {
                    for (StreamObserver<ChatMessage> client : observers) {
                        client.onNext(msg);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                if (roomId != -1) {
                    CopyOnWriteArrayList<StreamObserver<ChatMessage>> observers = roomObservers.get(roomId);
                    if (observers != null) {
                        observers.remove(responseObserver);
                    }
                }
            }

            @Override
            public void onCompleted() {
                if (roomId != -1) {
                    CopyOnWriteArrayList<StreamObserver<ChatMessage>> observers = roomObservers.get(roomId);
                    if (observers != null) {
                        observers.remove(responseObserver);
                    }
                }
                responseObserver.onCompleted();
            }
        };
    }

}
