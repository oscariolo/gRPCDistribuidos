package grpc_trabajo.grpcImpl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.proto.services.ChatMessage;
import com.example.proto.services.Room;
import com.example.proto.services.User;
import com.google.protobuf.Empty;

import grpc_trabajo.repository.UserRepository;

import com.example.proto.services.ChatServiceGrpc;
import com.example.proto.services.ConnectToRoomRequest;
import com.example.proto.services.ConnectToRoomResponse;
import com.example.proto.services.GetRoomsResponse;
import io.grpc.stub.StreamObserver;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
    // variable para almacenar los observadores de las salas de chat.
    private final ConcurrentMap<Integer, CopyOnWriteArrayList<StreamObserver<ChatMessage>>> roomObservers = new ConcurrentHashMap<>();
    
    // Mapa para vincular usuarios a salas de chat.
    private final ConcurrentMap<Integer, Integer> userRoomMapping = new ConcurrentHashMap<>();

    
    @Override
    public void getRooms(Empty request, StreamObserver<GetRoomsResponse> responseObserver) {

        // La peticion no necesita datos, simplemente devolvemos la lista de salas.

        //Obtener la lista de salas del repositorio.
        List<Room> rooms = grpc_trabajo.repository.RoomRepository.rooms;
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

        List<Room> rooms = grpc_trabajo.repository.RoomRepository.rooms; //Obtenemos la lista de salas del repositorio.

        int roomId = request.getRoom();
        // Buscar la sala por ID en la lista de salas.
        Room room = rooms.stream()
                .filter(r -> r.getId() == roomId)
                .findFirst()
                .orElse(null);

        if (room != null) { //Verifica si existe 
            User user = request.getUser();
            int userId = user.getId();
            // Verificar que el usuario esté registrado en el sistema con un ID válido.
            boolean userExists = UserRepository.userDatabase.values().stream()
                    .anyMatch(u -> u.getId() == userId);
            if (!userExists) {
                responseObserver.onError(new Throwable("Usuario no registrado en el sistema."));
                return;
            }
            //Vinculamos el usuario a la sala.
            userRoomMapping.put(userId, roomId);
            
            ConnectToRoomResponse response = ConnectToRoomResponse.newBuilder() //si todo va bien, construimos la respuesta
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
            boolean registered = false; //variable para verificar una sola vez la conexión del usuario a la sala
            int userId = -1;
            int roomId = -1;
            
            @Override
            public void onNext(ChatMessage msg) {
                if (!registered) { // Si el usuario no está registrado, obtenemos su ID y la sala a la que se conecta.
                    userId = msg.getSender().getId();
                    Integer mappedRoomId = userRoomMapping.get(userId);
                    if (mappedRoomId == null) {
                        responseObserver.onError(new Throwable("Usuario no registrado en ninguna sala. Conéctate a una sala primero mediante connectToRoom."));
                        return;
                    }
                    roomId = mappedRoomId;
                    //Agregamos un observador para el usuario en la sala correspondiente.
                    // Si el usuario ya está registrado en la sala, no lo volvemos a agregar.
                    roomObservers.computeIfAbsent(roomId, r -> new CopyOnWriteArrayList<>()).add(responseObserver); 
                    registered = true;
                }

                //De la lista de observadores de la sala, enviamos el mensaje a todos los clientes conectados.
                CopyOnWriteArrayList<StreamObserver<ChatMessage>> observers = roomObservers.get(roomId);
                if (observers != null) {
                    for (StreamObserver<ChatMessage> client : observers) {
                        // Llamando al metodo onNext del streamobserver enviamos el mensaje solo al stream de ese canal (room).
                        client.onNext(msg);
                    }
                }
            }

            //En caso de algun error retiramos el observador de la sala (cerramos la conexión).

            @Override
            public void onError(Throwable t) {
                if (registered) {
                    CopyOnWriteArrayList<StreamObserver<ChatMessage>> observers = roomObservers.get(roomId);
                    if (observers != null) {
                        observers.remove(responseObserver);
                    }
                }
            }

            //si es que se completa la conexión, retiramos el observador de la sala (cerramos la conexión).

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