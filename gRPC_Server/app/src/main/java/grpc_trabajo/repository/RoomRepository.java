package grpc_trabajo.repository;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.proto.services.Room;

public class RoomRepository {
    public static List<Room> rooms = new CopyOnWriteArrayList<>(Arrays.asList(
        Room.newBuilder().setId(1).setName("GamingRoom").setDescription("Sala de juegos").build(),
        Room.newBuilder().setId(2).setName("StudyRoom").setDescription("Tambien de juegos pero para disimular").build()
    ));
}
