package grpc_trabajo.grpcImpl;
import com.example.proto.services.UserServiceGrpc.UserServiceImplBase;

import grpc_trabajo.services.UserService;

import java.util.HashMap;

import com.example.proto.services.User;
import com.example.proto.services.UserRegistration;
import com.example.proto.services.UserLogin;
import io.grpc.stub.StreamObserver;


public class UserServiceImpl extends UserServiceImplBase {

    
    @Override
    public void registerUser(UserRegistration request, StreamObserver<User> responseObserver) {
        HashMap<Integer, User> userDatabase = UserService.userDatabase; // Obtenemos la base de datos simulada de usuarios
        // Creamos un usuario y lo agregamos a la base de datos simulada
        String username = request.getUsername();
        String email = request.getEmail();
        String password = request.getPassword();
        //Agregamos el usuario a la base de datos simulada
        User newUser = User.newBuilder()
                .setId(userDatabase.size() + 1) // Simulated user ID
                .setRegistration(UserRegistration.newBuilder()
                        .setUsername(username)
                        .setEmail(email)
                        .setPassword(password)
                        .build())
                .build();
        userDatabase.put(newUser.getId(), newUser); // Simulated database insertion

        
        // Enviamos la respuesta al cliente
        responseObserver.onNext(newUser);
        responseObserver.onCompleted();
    }

    @Override
    public void loginUser(UserLogin request, StreamObserver<User> responseObserver) {

        HashMap<Integer, User> userDatabase = UserService.userDatabase; // Obtenemos la base de datos simulada de usuarios
        // Verificamos por el email y la contraseña del usuario
        String email = request.getEmail();
        String password = request.getPassword();
        
        // Check if the user exists in the database
        for (User user : userDatabase.values()) {
            if (user.getRegistration().getEmail().equals(email) &&
                user.getRegistration().getPassword().equals(password)) {
                // User found, send the user object
                responseObserver.onNext(user);
                responseObserver.onCompleted();
                return;
            }
        }
        // If user not found, send an error
        responseObserver.onError(new Throwable("Invalid username or password"));
    }
    
}
