import socket

HOST = 'localhost'     # Endereco IP do Servidor
PORT = 50000           # Porta que o Servidor esta

print('Connecting...')
tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
tcp.connect((HOST, PORT))
print('Sending detect command...')
tcp.send(b'detect')
tcp.close()
print('Sent!')
input("Press ENTER to quit...")