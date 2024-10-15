package es.um.redes.nanoFiles.directory.server;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import es.um.redes.nanoFiles.directory.message.DirMessage;
import es.um.redes.nanoFiles.directory.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class DirectoryThread extends Thread {

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	protected DatagramSocket socket = null;

	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	protected double messageDiscardProbability;

	/**
	 * Estructura para guardar los nicks de usuarios registrados, y la fecha/hora de
	 * registro
	 * 
	 */
	private HashMap<String, LocalDateTime> nicks;
	/**
	 * Estructura para guardar los usuarios servidores (nick, direcciones de socket
	 * TCP)
	 */
	// TCP)
	private HashMap<String, InetSocketAddress> servers;
	/**
	 * Estructura para guardar la lista de ficheros publicados por todos los peers
	 * servidores, cada fichero identificado por su hash
	 */
	private HashMap<String, FileInfo> files;
	/**
	 * Estructura para asociar cada fichero (identificado por su hash) con
	 * su servidor propietario (necesario para mantener actualizado filelist)
	 */
	private HashMap<String, String> owners;

	public DirectoryThread(int directoryPort, double corruptionProbability) throws SocketException {
		// Crear dirección de socket con el puerto en el que escucha el directorio
		InetSocketAddress serverAddress = new InetSocketAddress(directoryPort);
		// Crear el socket UDP asociado a la dirección de socket anterior
		socket = new DatagramSocket(serverAddress);
		// Creamos los mapas de nicks, servers y ficheros
		nicks = new HashMap<String, LocalDateTime>();
		servers = new HashMap<String, InetSocketAddress>();
		files = new HashMap<String, FileInfo>();
		owners = new HashMap<String, String>();
		// Probabilidad de que nos llegue un mensaje corrupto
		messageDiscardProbability = corruptionProbability;
	}

	public void run() {
		byte[] receptionBuffer = new byte[DirMessage.PACKET_MAX_SIZE];
		DatagramPacket requestPacket = new DatagramPacket(receptionBuffer, receptionBuffer.length);
		InetSocketAddress clientId = null;

		System.out.println("Directory starting...");

		while (true) {
			try {

				// Recibimos a través del socket el datagrama con mensaje de solicitud
				socket.receive(requestPacket);
				
				// Averiguamos quién es el cliente
				clientId = (InetSocketAddress) requestPacket.getSocketAddress();
				
				// Vemos si el mensaje debe ser descartado por la probabilidad de descarte

				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println("Directory DISCARDED datagram from " + clientId);
					continue;
				}
				
				// Mostramos lo que hemos recibido
				byte opcode = receptionBuffer[0];
				System.out.println("Datagram received from client at addr " + clientId);
				System.out.println("Operation: " + DirMessageOps.opcodeToOperation(opcode));
				
				
				if (requestPacket.getData().length > 0) {
					processRequestFromClient(requestPacket.getData(), clientId);
				} else {
					System.err.println("Directory received EMPTY datagram from " + clientId);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Directory received EMPTY datagram from " + clientId);				
				break;
			}
		}
		// Cerrar el socket
		socket.close();
	}

	// Método para procesar la solicitud enviada por clientAddr
	// Actualizar estado del directorio y enviar una respuesta en función del
	// tipo de mensaje recibido
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException {
		// Construir un objeto mensaje (DirMessage) a partir de los datos recibidos
		DirMessage request = DirMessage.buildMessageFromReceivedData(data);
		switch(request.getOpcode()) {
		case DirMessageOps.OPCODE_LOGIN:
			sendLoginOK(clientAddr);
			break;
		case DirMessageOps.OPCODE_REGISTER_USERNAME:
			String nuevonick = request.getUserName();
			if(nicks.containsKey(nuevonick)) sendRegisterFAIL(clientAddr);
			else{
				nicks.put(nuevonick, LocalDateTime.now());
				sendRegisterOK(clientAddr);
			}
			break;
		case DirMessageOps.OPCODE_GETUSERS:
			sendUserList(clientAddr);
			break;
		case DirMessageOps.OPCODE_SERVE_FILES:
			servers.put(request.getUserName(), new InetSocketAddress(clientAddr.getAddress(), request.getPort()));
			FileInfo[] ficheros = request.getMeta();
			for(FileInfo f : ficheros) {
				files.put(f.getHash(), f);
				owners.put(f.getHash(), request.getUserName());
			}
			sendServeOK(clientAddr);
			break;
		case DirMessageOps.OPCODE_LOOKUP_USERNAME:
			String buscarNick = request.getUserName();
			if(servers.containsKey(buscarNick)) {
				sendLookupFound(servers.get(buscarNick), clientAddr);
			} else sendLookupNotFound(clientAddr);
			break;
		case DirMessageOps.OPCODE_LOGOFF:
			nicks.remove(request.getUserName());
			sendQuit(clientAddr);
			break;
		case DirMessageOps.OPCODE_SERVE_FILES_STOP:
			servers.remove(request.getUserName());
			HashSet<String> hashFicheros = new HashSet<String>();
			for(String s : owners.keySet()) {
				hashFicheros.add(s);
			};
			for(String s : hashFicheros) {
				if(owners.get(s).equals(request.getUserName())) {
					owners.remove(s);
					files.remove(s);
				}
			}
			sendStopOk(clientAddr);
			break;
		case DirMessageOps.OPCODE_GETFILES:
			FileInfo[] meta = new FileInfo[files.keySet().size()];
			int i = 0;
			for(String s : files.keySet()) {
				meta[i] = files.get(s);
				i++;
			}
			sendFileList(meta, clientAddr);
		default:
			break;
		}
	}

	// Construir el datagrama con la respuesta y enviarlo por el socket al cliente
	
	private void sendLoginOK(InetSocketAddress clientAddr) throws IOException {
		byte[] responseData = DirMessage.buildLoginOKResponseMessage(servers.size());
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendRegisterOK(InetSocketAddress clientAddr) throws IOException {
		byte[] responseData = DirMessage.buildRegisterOKResponseMessage();
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendRegisterFAIL(InetSocketAddress clientAddr) throws IOException{
		byte[] responseData = DirMessage.buildRegisterFAILResponseMessage();
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendUserList(InetSocketAddress clientAddr) throws IOException{
		byte[] responseData = DirMessage.buildUserListResponseMessage(nicks.keySet(), servers.keySet());
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendServeOK(InetSocketAddress clientAddr) throws IOException{
		byte[] responseData = DirMessage.buildServeFilesResponseMessage();
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendLookupFound(InetSocketAddress server, InetSocketAddress clientAddr) throws IOException{
		byte[] responseData = DirMessage.buildLookupUserFoundResponseMessage(server);
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendLookupNotFound(InetSocketAddress clientAddr) throws IOException{
		byte[] responseData = DirMessage.buildLookupUserNotFoundResponseMessage();
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendQuit(InetSocketAddress clientAddr) throws IOException{
		byte[] responseData = DirMessage.buildLogOffResponseMessage();
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendStopOk(InetSocketAddress clientAddr) throws IOException{
		byte[] responseData = DirMessage.buildStopServerResponseMessage();
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendFileList(FileInfo[] ficheros, InetSocketAddress clientAddr) throws IOException{
		byte[] responseData = DirMessage.buildFileListResponseMessage(ficheros);
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
}
