package grpc_trabajo.grpcImpl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.proto.services.ChatMessage;
import com.example.proto.services.Room;
import com.example.proto.services.User;
import com.google.protobuf.Empty;
import com.example.proto.services.ChatServiceGrpc;
import com.example.proto.services.ConnectToRoomRequest;
import com.example.proto.services.ConnectToRoomResponse;
import com.example.proto.services.GetRoomsResponse;
import io.grpc.stub.StreamObserver;
import grpc_trabajo.services.UserService; // Import the service holding the user database

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
    private final List<Room> rooms = new CopyOnWriteArrayList<>();
    // Maps a room ID to a list of connected chat observers.
    private final ConcurrentMap<Integer, CopyOnWriteArrayList<StreamObserver<ChatMessage>>> roomObservers = new ConcurrentHashMap<>();
    // Maps a username to the room ID they are connected to.
    private final ConcurrentMap<String, Integer> userRoomMapping = new ConcurrentHashMap<>();

    public ChatServiceImpl() {
        // Initialize default rooms.
        Room room1 = Room.newBuilder().setName("GamingRoom").setDescription("Sala de gaming").setId(1).build();
        Room room2 = Room.newBuilder().setName("StudyRoom").setDescription("Sala de estudio").setId(2).build();
        rooms.add(room1);
        rooms.add(room2);
    }
    
    @Override
    public void getRooms(Empty request, StreamObserver<GetRoomsResponse> responseObserver) {
        GetRoomsResponse.Builder responseBuilder = GetRoomsResponse.newBuilder();
        for (Room room : rooms) {
            responseBuilder.addRooms(room);
        }
        GetRoomsResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void connectToRoom(ConnectToRoomRequest request, StreamObserver<ConnectToRoomResponse> responseObserver) {
        int roomId = request.getRoom();
        // Search for the room by its ID.
        Room room = rooms.stream()
                .filter(r -> r.getId() == roomId)
                .findFirst()
                .orElse(null);

        if (room != null) {
            User user = request.getUser();
            String username = user.getRegistration().getUsername();
            // Verify that the user is registered using the shared userDatabase.
            boolean userExists = UserService.userDatabase.values().stream()
                    .anyMatch(u -> u.getRegistration().getUsername().equals(username));
            if (!userExists) {
                responseObserver.onError(new Throwable("Usuario no registrado en el sistema."));
                return;
            }
            // Vinculate (associate) the user with the room.
            userRoomMapping.put(username, roomId);
            
            ConnectToRoomResponse response = ConnectToRoomResponse.newBuilder()
                    .setRoom(room)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(new Throwable("Sala no encontrada con ID: " + roomId));
        }
    }

    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
        return new StreamObserver<ChatMessage>() {
            boolean registered = false;
            String username = null;
            int roomId = -1;
            
            @Override
            public void onNext(ChatMessage msg) {
                if (!registered) {
                    username = msg.getSender();
                    Integer mappedRoomId = userRoomMapping.get(username);
                    if (mappedRoomId == null) {
                        responseObserver.onError(new Throwable("Usuario no registrado en ninguna sala. Conéctate a una sala primero mediante connectToRoom."));
                        return;
                    }
                    roomId = mappedRoomId;
                    roomObservers.computeIfAbsent(roomId, r -> new CopyOnWriteArrayList<>()).add(responseObserver);
                    registered = true;
                }
                CopyOnWriteArrayList<StreamObserver<ChatMessage>> observers = roomObservers.get(roomId);
                if (observers != null) {
                    for (StreamObserver<ChatMessage> client : observers) {
                        client.onNext(msg);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                if (registered) {
                    CopyOnWriteArrayList<StreamObserver<ChatMessage>> observers = roomObservers.get(roomId);
                    if (observers != null) {
                        observers.remove(responseObserver);
                    }
                }
            }

            @Override
            public void onCompleted() {
                if (registered) {
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