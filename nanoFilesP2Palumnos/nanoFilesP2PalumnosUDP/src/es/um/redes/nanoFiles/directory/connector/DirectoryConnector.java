package es.um.redes.nanoFiles.directory.connector;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import es.um.redes.nanoFiles.directory.message.DirMessage;
import es.um.redes.nanoFiles.util.FileInfo;

import java.util.Set;
/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DEFAULT_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private InetSocketAddress directoryAddress;

	public DirectoryConnector(String address) throws IOException {
		/*
		 * Crear el socket UDP para comunicación con el directorio durante el
		 * resto de la ejecución del programa, y guardar su dirección (IP:puerto) en
		 * atributos
		 */
		socket = new DatagramSocket();
		directoryAddress = new InetSocketAddress(address, DEFAULT_PORT);
	}

	/**
	 * Método para enviar y recibir datagramas al/del directorio
	 * 
	 * @param requestData los datos a enviar al directorio (mensaje de solicitud)
	 * @return los datos recibidos del directorio (mensaje de respuesta)
	 */
	public byte[] sendAndReceiveDatagrams(byte[] requestData) throws IOException {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		/*
		 * Enviar datos en un datagrama al directorio y recibir una respuesta.
		 * Debe implementarse un mecanismo de reintento usando temporizador, en caso de
		 * que no se reciba respuesta en el plazo de TIMEOUT. En caso de salte el
		 * timeout, se debe reintentar como máximo en MAX_NUMBER_OF_ATTEMPTS ocasiones
		 */
		DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, directoryAddress);
		socket.send(requestPacket);
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
		int intentos = 0;
		while(intentos < MAX_NUMBER_OF_ATTEMPTS && responsePacket.getAddress() == null) {
			try{
				socket.setSoTimeout(TIMEOUT);
				socket.receive(responsePacket);
			}
			catch(SocketTimeoutException e) {
				System.out.println("* No response from server. Trying again...");
				socket.send(requestPacket);
				intentos++;
			}
		}
		if(intentos == MAX_NUMBER_OF_ATTEMPTS) {
			System.out.println("* No response from server. Maximum number of tries reached.");
			System.out.println("* Closing NanoFiles...");
			System.exit(-1);
		}
		return responseData;
	}
	
	/*
	 * Crear un método distinto para cada intercambio posible de mensajes con
	 * el directorio, haciendo uso
	 * de los métodos adecuados de DirMessage para construir mensajes de petición y
	 * procesar mensajes de respuesta
	 */
	
	public int logIntoDirectory() throws IOException { // Returns number of file servers
		byte[] requestData = DirMessage.buildLoginRequestMessage();
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processLoginResponse(responseData);
	}
	
	public boolean registerNickname(String nick) throws IOException{
		byte[] requestData = DirMessage.buildRegisterRequestMessage(nick);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processRegisterResponseMessage(responseData);
	}
	
	public Set<String> getUserList() throws IOException{
		byte[] requestData = DirMessage.buildUserListRequestMessage();
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processUserListResponseMessage(responseData);
	}
	
	public boolean serveFiles(int port, String nickname) throws IOException{
		byte[] requestData = DirMessage.buildServeFilesRequestMessage(port, nickname);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processServeFilesResponseMessage(responseData);
	}
	
	public InetSocketAddress lookupUser(String nickname) throws IOException{
		byte[] requestData = DirMessage.buildLookupUserRequestMessage(nickname);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processLookupUserResponseMessage(responseData);
	}
	
	public boolean logOffDirectory(String nickname) throws IOException { 
		byte[] requestData = DirMessage.buildLogOffRequestMessage(nickname);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		boolean result = DirMessage.processLogOffResponse(responseData);
		if(result) socket.close();
		return result;
	}
	
	public boolean stopServer(String nickname) throws IOException {
		byte[] requestData = DirMessage.buildStopServerRequestMessage(nickname);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processStopServerResponse(responseData);
	}
	
	public FileInfo[] getFiles() throws IOException {
		byte[] requestData = DirMessage.buildGetFilesRequestMessage();
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processGetFilesResponse(responseData);
	}

}
