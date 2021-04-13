import _thread
from detection import ObjectDetection
import socket

detection = ObjectDetection()
detection.detect(False)

HOST = 'localhost'     # Endereco IP do Servidor
PORT = 50000           # Porta que o Servidor esta

def client_connected(con, client):
    print('Connected by', client)

    command = ""
    while True:
        msg = con.recv(1024)
        if not msg: break
        command += msg.decode("UTF-8")
        

    print('Command received: ', command)
    if command == 'detect':
        print('detection started')
        detection.detect(True)

    print('Closing client connection...', client)
    con.close()
    print('Client connection closed.')
    _thread.exit()

print('Creating the socket...')
tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

orig = (HOST, PORT)

print('Binding socket')
tcp.bind(orig)
tcp.listen(1)

while True:
    print('Waiting for new connection...')
    con, client = tcp.accept()
    _thread.start_new_thread(client_connected, tuple([con, client]))

tcp.close()