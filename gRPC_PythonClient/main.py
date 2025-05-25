import sys 
sys.path.insert(0, './protoFiles')

import grpc
import threading
import time
from protoFiles import services_pb2, services_pb2_grpc


def receive_messages(stub):
    for message in stub.Chat(iter([])):  # dummy initial stream
        print(f"[{message.sender}]: {message.message}")

def chat_client(username):
    channel = grpc.insecure_channel('localhost:50051')
    stub = services_pb2_grpc.ChatServiceStub(channel)

    def message_stream():
        while True:
            text = input()
            yield services_pb2.ChatMessage(sender=username, message=text, room="general")

    responses = stub.Chat(message_stream())
    for response in responses:
        print(f"[{response.sender}] {response.message}")

if __name__ == '__main__':
    name = input("Enter your username: ")
    chat_client(name)
