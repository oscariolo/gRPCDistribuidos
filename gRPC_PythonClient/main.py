import sys
sys.path.insert(0, './protoFiles')  # Allow importing generated proto files

import grpc
from protoFiles import services_pb2, services_pb2_grpc
from google.protobuf.empty_pb2 import Empty

def register_user(user_stub):
    print("=== Register New User ===")
    username = input("Username: ")
    email = input("Email: ")
    password = input("Password: ")
    request = services_pb2.UserRegistration(
        username=username,
        email=email,
        password=password
    )
    try:
        user = user_stub.RegisterUser(request)
        print(f"User registered. ID: {user.id}, Username: {user.registration.username}")
        return user
    except grpc.RpcError as e:
        print("Registration error:", e.details())
        return None

def login_user(user_stub):
    print("=== Login ===")
    email = input("Email: ")
    password = input("Password: ")
    request = services_pb2.UserLogin(
        email=email,
        password=password
    )
    try:
        user = user_stub.LoginUser(request)
        print(f"Login successful. Welcome, {user.registration.username}")
        return user
    except grpc.RpcError as e:
        print("Login error:", e.details())
        return None

def list_rooms(chat_stub):
    try:
        response = chat_stub.GetRooms(Empty())
        print("=== Available Rooms ===")
        for room in response.rooms:
            print(f"{room.id}: {room.name} - {room.description}")
        return response.rooms
    except grpc.RpcError as e:
        print("Error listing rooms:", e.details())
        return []

def connect_to_room(chat_stub, room_id, username):
    # Build a minimal User message (only username is used; adjust as needed)
    user = services_pb2.User(
        registration=services_pb2.UserRegistration(
            username=username,
            email="",
            password=""
        )
    )
    request = services_pb2.ConnectToRoomRequest(
        room=room_id,
        user=user
    )
    try:
        response = chat_stub.ConnectToRoom(request)
        print(f"Connected to room: {response.room.name}")
        return response.room
    except grpc.RpcError as e:
        print("Error connecting to room:", e.details())
        return None

def chat(username, chat_stub, room):
    print(f"=== Chat: You are now in room '{room.name}' ===")
    print("Type your messages. Type '/exit' to leave the room.")
    
    def message_stream():
        while True:
            text = input()
            if text.strip() == "/exit":
                break
            yield services_pb2.ChatMessage(
                sender=username,
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
            print(f"[{response.sender}]: {response.message}")
    except grpc.RpcError as e:
        print("Chat error:", e.details())

def chat_client(username, chat_stub):
    while True:
        list_rooms(chat_stub)
        try:
            room_id = int(input("Enter the room id you want to join: "))
        except ValueError:
            print("Invalid room id. Try again.")
            continue
        
        room = connect_to_room(chat_stub, room_id, username)
        if room is None:
            continue
        
        chat(username, chat_stub, room)
        
        choice = input("Type '/join' to join another room or any other key to exit: ")
        if choice.strip() != "/join":
            break

def main():
    channel = grpc.insecure_channel('localhost:50051')
    user_stub = services_pb2_grpc.UserServiceStub(channel)
    chat_stub = services_pb2_grpc.ChatServiceStub(channel)

    print("=== Welcome to the Chat App ===")
    print("1. Register")
    print("2. Login")
    option = input("Select an option (1 or 2): ")
    user = None
    if option == "1":
        user = register_user(user_stub)
    elif option == "2":
        user = login_user(user_stub)
    else:
        print("Invalid option, exiting.")
        return
    
    if user is None:
        print("User authentication failed. Exiting.")
        return
    
    username = user.registration.username
    chat_client(username, chat_stub)

if __name__ == '__main__':
    main()