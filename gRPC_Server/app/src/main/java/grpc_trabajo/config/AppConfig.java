package grpc_trabajo.config;
import java.util.List;

import grpc_trabajo.grpcImpl.ChatServiceImpl;
import grpc_trabajo.grpcImpl.ProductServiceImpl;
import io.grpc.ServerServiceDefinition;

public class AppConfig {
    List<ServerServiceDefinition> implementedServices; //Servicios implementados en GRPC
    int port = 50051; //puerto de la aplicacion por defecto

    public AppConfig() { //Constructor de la clase
        implementedServices = new java.util.ArrayList<>(); //Inicializamos la lista de servicios implementados
        getServices(); //Llamamos al metodo que devuelve la lista de servicios implementados
    }
    
    public List<ServerServiceDefinition> getServices() { 

        implementedServices.add(new ProductServiceImpl().bindService()); //Agregamos por cada servicio implementado
        implementedServices.add(new ChatServiceImpl().bindService()); 

        
        return implementedServices;
    }

    public int getPort() { //Devuelve el puerto de la aplicacion
        return port;
    }


}
