import sys
sys.path.insert(0, './protoFiles')  # Permite importar los proto generados

import grpc
from protoFiles import services_pb2, services_pb2_grpc
from google.protobuf.empty_pb2 import Empty

def register_user(user_stub):
    print("=== Registrar Nuevo Usuario ===")
    username = input("Nombre de usuario: ")
    email = input("Correo electrónico: ")
    password = input("Contraseña: ")
    request = services_pb2.UserRegistration(
        username=username,
        email=email,
        password=password
    )
    try:
        user = user_stub.RegisterUser(request)
        print(f"Usuario registrado. ID: {user.id}, Nombre de usuario: {user.registration.username}")
        return user
    except grpc.RpcError as e:
        print("Error en el registro:", e.details())
        return None

def login_user(user_stub):
    print("=== Iniciar Sesión ===")
    email = input("Correo electrónico: ")
    password = input("Contraseña: ")
    request = services_pb2.UserLogin(
        email=email,
        password=password
    )
    try:
        user = user_stub.LoginUser(request)
        print(f"Inicio de sesión exitoso. Bienvenido, {user.registration.username}")
        return user
    except grpc.RpcError as e:
        print("Error al iniciar sesión:", e.details())
        return None

def list_rooms(chat_stub):
    try:
        response = chat_stub.GetRooms(Empty())
        print("=== Salas Disponibles ===")
        for room in response.rooms:
            print(f"{room.id}: {room.name} - {room.description}")
        return response.rooms
    except grpc.RpcError as e:
        print("Error al listar salas:", e.details())
        return []

def connect_to_room(chat_stub, room_id, user_id):
    # Se envía sólo el id del usuario
    user = services_pb2.User(
        id=user_id
    )
    request = services_pb2.ConnectToRoomRequest(
        room=room_id,
        user=user
    )
    try:
        response = chat_stub.ConnectToRoom(request)
        print(f"Conectado a la sala: {response.room.name}")
        return response.room
    except grpc.RpcError as e:
        print("Error al conectar a la sala:", e.details())
        return None

def chat(user, chat_stub, room):
    print(f"=== Chat: Estás en la sala '{room.name}' ===")
    print("Escribe '/exit' para salir del chat.")

    def message_stream():
        yield services_pb2.ChatMessage(
            sender=user.id,
            message="__handshake__",
            room=services_pb2.Room(
                id=room.id,
                name=room.name,
                description=room.description
            )
        )
        while True:
            text = input()
            if text.strip() == "/exit":
                break
            yield services_pb2.ChatMessage(
                sender=user.id,
                message=text,
                room=services_pb2.Room(
                    id=room.id,
                    name=room.name,
                    description=room.description
                )
            )
    
    try:
        responses = chat_stub.Chat(message_stream())
        for response in responses:
            if response.message == "__handshake__":
                pass  # Ignorar mensajes de handshake
            else:
                print(f"[{response.sender}]: {response.message}")
    except grpc.RpcError as e:
        print("Error en el chat:", e.details())

def chat_client(user, chat_stub):
    while True:
        list_rooms(chat_stub)
        try:
            room_id = int(input("Ingrese el id de la sala a la que desea unirse: "))
        except ValueError:
            print("Id de sala inválido. Inténtalo de nuevo.")
            continue
        
        room = connect_to_room(chat_stub, room_id, user.id)
        if room is None:
            continue
        
        chat(user, chat_stub, room)
        
        choice = input("Escriba '/join' para unirse a otra sala o cualquier otra tecla para salir: ")
        if choice.strip() != "/join":
            break

def main():
    channel = grpc.insecure_channel('localhost:50051')
    user_stub = services_pb2_grpc.UserServiceStub(channel)
    chat_stub = services_pb2_grpc.ChatServiceStub(channel)

    print("=== Bienvenido a la aplicación de chat ===")
    print("1. Registrar")
    print("2. Iniciar Sesión")
    option = input("Seleccione una opción (1 o 2): ")
    user = None
    if option == "1":
        user = register_user(user_stub)
    elif option == "2":
        user = login_user(user_stub)
    else:
        print("Opción inválida, saliendo.")
        return
    
    if user is None:
        print("La autenticación del usuario falló. Saliendo.")
        return
    
    chat_client(user, chat_stub)

if __name__ == '__main__':
    main()