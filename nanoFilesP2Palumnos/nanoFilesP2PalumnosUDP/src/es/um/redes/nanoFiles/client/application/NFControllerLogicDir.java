package es.um.redes.nanoFiles.client.application;

import java.io.IOException;
import java.util.Set;
import java.net.InetSocketAddress;

import es.um.redes.nanoFiles.directory.connector.DirectoryConnector;
import es.um.redes.nanoFiles.directory.message.DirMessage;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicDir {
	// Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;

	/**
	 * Método para conectar con el directorio y obtener el número de peers que están
	 * sirviendo ficheros
	 * 
	 * @param directoryHostname el nombre de host/IP en el que se está ejecutando el
	 *                          directorio
	 * @return true si se ha conseguido contactar con el directorio.
	 */
	boolean logIntoDirectory(String directoryHostname) {
		/*
		 * Debe crear un objeto DirectoryConnector a partir del parámetro
		 * directoryHostname y guardarlo en el atributo correspondiente. A continuación,
		 * utilizarlo para comunicarse con el directorio y realizar tratar de realizar
		 * el "login", informar por pantalla del éxito/fracaso y devolver dicho valor
		 */
		boolean result = false;
		try{
			directoryConnector = new DirectoryConnector(directoryHostname);
			int servers = directoryConnector.logIntoDirectory();
			if(servers != -1) {
				result = true;
				System.out.println("Number of servers: " + servers);
			}
			else {
				result = false;
			}
		}
		catch(IOException e) {
			System.err.println("* Communication with directory error. Printing stack trace...");
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Método para registrar el nick del usuario en el directorio
	 * 
	 * @param nickname el nombre de usuario a registrar
	 * @return true si el nick es válido (no contiene ":") y se ha registrado
	 *         nickname correctamente con el directorio (no estaba duplicado), falso
	 *         en caso contrario.
	 */
	boolean registerNickInDirectory(String nickname) {
		/*
		 * Registrar un nick. Comunicarse con el directorio (a través del
		 * directoryConnector) para solicitar registrar un nick. Debe informar por
		 * pantalla si el registro fue exitoso o fallido, y devolver dicho valor
		 * booleano. Se debe comprobar antes que el nick no contiene el carácter ':'.
		 */
		boolean result = false;
		try {
			if(!nickname.contains(":")) {
				result = directoryConnector.registerNickname(nickname);
			}
			else System.out.println("* Invalid nickname (cannot contain ':' character)");
		}
		catch(IOException e) {
			System.err.println("* Communication with directory error. Printing stack trace...");
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Método para obtener de entre los peer servidores registrados en el directorio
	 * la IP:puerto del peer con el nick especificado
	 * 
	 * @param nickname el nick del peer por cuya IP:puerto se pregunta
	 * @return La dirección de socket del peer identificado por dich nick, o null si
	 *         no se encuentra ningún peer con ese nick.
	 */
	InetSocketAddress lookupUserInDirectory(String nickname) {
		/*
		 * Obtener IP:puerto asociada a nickname. Comunicarse con el directorio (a
		 * través del directoryConnector) para preguntar la dirección de socket en la
		 * que el peer con 'nickname' está sirviendo ficheros. Si no se obtiene una
		 * respuesta con IP:puerto válidos, se debe devolver null.
		 */
		InetSocketAddress peerAddr = null;
		try{
			peerAddr = directoryConnector.lookupUser(nickname);
		} catch (IOException e) {
			System.err.println("* Communication with directory error. Printing stack trace...");
			e.printStackTrace();
		}
		return peerAddr;
	}

	/**
	 * Método para publicar la lista de ficheros que este peer está compartiendo.
	 * 
	 * @param port     El puerto en el que este peer escucha solicitudes de conexión
	 *                 de otros peers.
	 * @param nickname El nick de este peer, que será asociado a lista de ficheros y
	 *                 su IP:port
	 */
	boolean publishLocalFilesToDirectory(int port, String nickname) {
		/*
		 * Enviar la lista de ficheros servidos. Comunicarse con el directorio (a
		 * través del directoryConnector) para enviar la lista de ficheros servidos por
		 * este peer con nick 'nickname' en el puerto 'port'. Los ficheros de la carpeta
		 * local compartida están disponibles en NanoFiles.db).
		 */
		boolean result = false;
		try {
			result = directoryConnector.serveFiles(port, nickname);
		}
		catch(IOException e) {
			System.err.println("* Communication with directory error. Printing stack trace...");
			e.printStackTrace();
		}
		return result;
	}
	
	public boolean stopServingFilesToDirectory(String nickname) {
		boolean result = false;
		try {
			result = directoryConnector.stopServer(nickname);
		} catch (IOException e) {
			System.err.println("* Communication with directory error. Printing stack trace...");
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Método para obtener y mostrar la lista de nicks registrados en el directorio
	 */
	boolean getUserListFromDirectory() {
		/*
		 * Obtener la lista de usuarios registrados. Comunicarse con el directorio
		 * (a través del directoryConnector) para obtener la lista de nicks registrados
		 * e imprimirla por pantalla.
		 */
		boolean result = false;
		Set<String> userlist;
		try {
			userlist = directoryConnector.getUserList();
			if(userlist.size() == 0) {
				System.out.println("* No users registered yet");
			}
			else {
				System.out.println("USER LIST: ");
				for(String u : userlist) {
					System.out.println(u);
				}
			}
			result = true;
		}
		catch(IOException e) {
			System.err.println("* Communication with directory error. Printing stack trace...");
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Método para obtener y mostrar la lista de ficheros que los peer servidores
	 * han publicado al directorio
	 */
	public boolean getFileListFromDirectory() {
		/*
		 * Obtener la lista de ficheros servidos. Comunicarse con el directorio (a
		 * través del directoryConnector) para obtener la lista de ficheros e imprimirla
		 * por pantalla.
		 */
		boolean result = false;
		FileInfo[] ficheros;
		try {
			ficheros = directoryConnector.getFiles();
			if(ficheros.length == 0) {
				System.out.println("* No files available yet");
			}
			else {
				System.out.println("FILE LIST: ");
				FileInfo.printToSysout(ficheros);
			}
			result = true;
		} catch (IOException e) {
			System.err.println("* Communication with directory error. Printing stack trace...");
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Método para desconectarse del directorio (cerrar sesión) 
	 */
	public boolean logout(String nickname) {
		/*
		 * Dar de baja el nickname. Al salir del programa, se debe dar de baja el
		 * nick registrado con el directorio y cerrar el socket usado por el
		 * directoryConnector.
		 */
		boolean result = false;
		try {
			result = directoryConnector.logOffDirectory(nickname);
		} catch (IOException e) {
			System.err.println("* Communication with directory error. Printing stack trace...");
			e.printStackTrace();
		}
		return result;
	}
}
