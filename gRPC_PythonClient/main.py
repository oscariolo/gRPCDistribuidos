import sys
sys.path.insert(0, './protoFiles')

import grpc
from protoFiles import services_pb2, services_pb2_grpc
from google.protobuf.empty_pb2 import Empty

def list_rooms(stub):
    response = stub.GetRooms(Empty())
    print("Available rooms:")
    for room in response.rooms:
        print(f"{room.id}: {room.name} - {room.description}")
    return response.rooms

def connect_to_room(stub, room_id):
    request = services_pb2.ConnectToRoomRequest(room=room_id)
    response = stub.ConnectToRoom(request)
    print(f"Connected to room: {response.room.name}")
    return response.room

def chat(username, stub, room):
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
    responses = stub.Chat(message_stream())
    # This for-loop will exit when the stream is cancelled (after /exit is sent)
    try:
        for response in responses:
            print(f"[{response.sender}]: {response.message}")
    except grpc.RpcError as e:
        # Optionally process RPC errors if needed
        pass

def chat_client(username):
    channel = grpc.insecure_channel('localhost:50051')
    stub = services_pb2_grpc.ChatServiceStub(channel)
    
    while True:
        # List available rooms
        list_rooms(stub)
        try:
            room_id = int(input("Enter the room id you want to join: "))
        except ValueError:
            print("Invalid room id. Try again.")
            continue
        
        # Connect to chosen room
        room = connect_to_room(stub, room_id)
        
        print("You can now start chatting. Type your messages. Type '/exit' to leave the room.")
        chat(username, stub, room)
        
        choice = input("Type '/join' to join another room or any other key to quit: ")
        if choice.strip() != "/join":
            break

if __name__ == '__main__':
    name = input("Enter your username: ")
    chat_client(name)