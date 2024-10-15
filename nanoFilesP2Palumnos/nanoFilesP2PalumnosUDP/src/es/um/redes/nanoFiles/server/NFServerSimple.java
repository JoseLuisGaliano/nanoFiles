package es.um.redes.nanoFiles.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class NFServerSimple {

	private static final int SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS = 1000;
	private static final String STOP_SERVER_COMMAND = "fgstop";
	private ServerSocket serverSocket;

	public NFServerSimple(int port) throws IOException {
		/*
		 * Crear una direción de socket a partir del puerto especificado
		 */
		InetSocketAddress FileServerSocketAddress = new InetSocketAddress(port);
		/*
		 * Crear un socket servidor y ligarlo a la dirección de socket anterior
		 */
		serverSocket = new ServerSocket();
		serverSocket.bind(FileServerSocketAddress);
	}

	/**
	 * Método para ejecutar el servidor de ficheros en primer plano. Sólo es capaz
	 * de atender una conexión de un cliente. Una vez se lanza, ya no es posible
	 * interactuar con la aplicación a menos que se implemente la funcionalidad de
	 * detectar el comando STOP_SERVER_COMMAND (opcional)
	 * 
	 */
	public void run() {
		/*
		 * Comprobar que el socket servidor está creado y ligado
		 */
		assert(serverSocket != null);

		try{
			boolean stopServer = false;
			BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.println("Enter '" + STOP_SERVER_COMMAND + "' to stop the server");
			while (!stopServer) {
				Socket cliente = null;
				while(cliente == null && !stopServer) {
					try {
						/*
						 * Usar el socket servidor para esperar conexiones de otros peers que
						 * soliciten descargar ficheros
						 */
						serverSocket.setSoTimeout(SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS);
						cliente = serverSocket.accept();
					} catch (SocketTimeoutException e) {
						/*
						 * (Para poder detener el servidor y volver a aceptar comandos).
						 * Establecer un temporizador en el ServerSocket antes de ligarlo, para
						 * comprobar mediante standardInput.ready()) periódicamente si se ha tecleado el
						 * comando "fgstop", en cuyo caso se cierra el socket servidor y se sale del
						 * bucle
						 */
						if(standardInput.ready()) {
							if(standardInput.readLine().equals(STOP_SERVER_COMMAND)) {
								stopServer = true;
							}
						}
					}
				}
				/*
				 * Al establecerse la conexión con un peer, la comunicación con dicho
				 * cliente se hace en el método NFServerComm.serveFilesToClient(socket), al cual
				 * hay que pasarle el objeto Socket devuelto por accept (retorna un nuevo socket
				 * para hablar directamente con el nuevo cliente conectado)
				 */
				if(!stopServer) NFServerComm.serveFilesToClient(cliente);
				cliente = null;
			}
			serverSocket.close();
			System.out.println("* NFServerSimple stopped");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
