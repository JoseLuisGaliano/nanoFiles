package es.um.redes.nanoFiles.client.application;

import es.um.redes.nanoFiles.client.comm.NFConnector;
import es.um.redes.nanoFiles.client.shell.NFCommands;
import es.um.redes.nanoFiles.client.shell.NFShell;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFController {
	/**
	 * Diferentes estados del cliente de acuerdo con el autómata
	 */
	private static final byte PRE_LOGIN = 0;
	private static final byte PRE_REGISTRATION = 1;
	private static final byte OFF_BROWSER = 2;
	private static final byte IN_BROWSER = 3;
	
	/**
	 * Shell para leer comandos de usuario de la entrada estándar
	 */
	private NFShell shell;
	/**
	 * Último comando proporcionado por el usuario
	 */
	private byte currentCommand;

	/**
	 * Objeto controlador encargado de la comunicación con el directorio
	 */
	private NFControllerLogicDir controllerDir;
	/**
	 * Objeto controlador encargado de la comunicación con otros peers (como
	 * servidor o cliente)
	 */
	private NFControllerLogicP2P controllerPeer;

	/**
	 * El estado en que se encuentra este peer (según el autómata)
	 */
	private byte clientStatus;
	/*
	 * Atributos donde se establecen los argumentos pasados a los distintos comandos
	 * del shell
	 */
	private String nickname; // Nick del usuario (register)
	private int serverPort; // Puerto TCP en el que escucha este peer (bgserve/fgserve)
	private String directory; // Nombre/IP del host donde está el directorio (login)
	private String browseUser; // Nickname del peer con el que conectar (browse)
	private String downloadTargetFileHash; // Hash del fichero a descargar (download)
	private String downloadLocalFileName; // Nombre con el que se guardará el fichero descargado

	// Constructor
	public NFController() {
		shell = new NFShell();
		controllerDir = new NFControllerLogicDir();
		controllerPeer = new NFControllerLogicP2P(controllerDir);
		clientStatus = PRE_LOGIN;
	}

	/**
	 * Devuelve el comando actual introducido por el usuario
	 */
	public byte getCurrentCommand() {
		return this.currentCommand;
	}

	/**
	 * Establece el comando actual
	 * 
	 * @param command el comando tecleado en el shell
	 */
	public void setCurrentCommand(byte command) {
		currentCommand = command;
	}

	/**
	 * Registra en atributos internos los posibles parámetros del comando tecleado
	 * por el usuario.
	 */
	public void setCurrentCommandArguments(String[] args) {
		switch (currentCommand) {
		case NFCommands.COM_LOGIN:
			directory = args[0];
			break;
		case NFCommands.COM_USERLIST:
			break;
		case NFCommands.COM_REGISTER:
			nickname = args[0];
			break;
		case NFCommands.COM_BROWSE:
			browseUser = args[0];
			break;
		case NFCommands.COM_FGSERVE:
			serverPort = Integer.parseInt(args[0]);
			break;
		case NFCommands.COM_DOWNLOAD:
			downloadTargetFileHash = args[0];
			downloadLocalFileName = args[1];
			break;
		default:
		}
	}

	/**
	 * Método para leer un comando general (fuera del browser)
	 */
	public void readGeneralCommandFromShell() {
		// Pedimos el comando al shell
		shell.readGeneralCommand();
		// Establecemos que el comando actual es el que ha obtenido el shell
		setCurrentCommand(shell.getCommand());
		// Analizamos los posibles parámetros asociados al comando
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	/**
	 * Procesa los comandos introducidos por un usuario que aún no está dentro del
	 * browser. Debe comprobar mediante "clientStatus" si el comando es válido según
	 * el estado actual del autómata (no todos los comandos son válidos en cualquier
	 * estado) y actualizar "clientStatus" cuando se produzca un cambio a otro
	 * estado
	 */
	public void processCommand() {
		boolean result;
		switch (currentCommand) {
		case NFCommands.COM_LOGIN:
			// Se loguea en el directorio para comprobar que está disponible
			if (clientStatus == PRE_LOGIN) {
				result = controllerDir.logIntoDirectory(directory);
				if (result) {
					clientStatus = PRE_REGISTRATION;
					System.out.println("* Login successful");
				} else{
					System.out.println("* Failed to log in to directory. Please try again");
				}
			} else {
				System.out.println("* You are already logged in to directory");
			}
			break;
		case NFCommands.COM_MYFILES:
			showMyLocalFiles(); // Muestra los ficheros en el directorio local compartido
			break;
		case NFCommands.COM_USERLIST:
			/*
			 * Pedir la lista de usuarios registrados en el directorio (a través del
			 * controllerDir) Obtiene los nicknames de los peers que se han registrado
			 */
			if(clientStatus == PRE_REGISTRATION || clientStatus == OFF_BROWSER) {
				result = controllerDir.getUserListFromDirectory();
				if(!result) System.out.println("* Error when displaying userlist. Please try again");
			} else {
				System.out.println("* You must log in first to view list of users");
			}
			break;
		case NFCommands.COM_FILELIST:
			/*
			 * Pedir la lista de ficheros en el directorio (a través del
			 * controllerDir) Obtiene los ficheros que otros peers están sirviendo (comando
			 * fgserve)
			 */
			if(clientStatus == OFF_BROWSER) {
				result = controllerDir.getFileListFromDirectory();
				if(!result) System.out.println("* Error when displaying filelist. Please try again");
			}
			else System.out.println("* You must be registered to view list of files");
			break;
		case NFCommands.COM_REGISTER:
			/*
			 * Registrar un nombre de usuario en el directorio (a través del
			 * controllerDir) Este comando sólo se debe poder procesar una vez se ha hecho
			 * el login Comprobar la respuesta para ver si se ha registrado correctamente En
			 * caso de éxito, podemos pasar a otro estado del autómata que permite nuevos
			 * comandos
			 */
			if(clientStatus == PRE_REGISTRATION) {
				result = controllerDir.registerNickInDirectory(nickname);
				if(result) {
					clientStatus = OFF_BROWSER;
					System.out.println("* Register successful");
				}
				else System.out.println("* Failed to register nickname in directory");
			}
			else if(clientStatus == OFF_BROWSER) {
				System.out.println("* You are already registered");
			}
			else {
				System.out.println("* You need to log into the directory first (login command)");
			}
			break;
		case NFCommands.COM_BROWSE:
			if (clientStatus == OFF_BROWSER) {
				enterBrowser();
			} else {
				System.out.println("* You must register before you can browse other user's files");
			}
			break;
		case NFCommands.COM_FGSERVE:
			/*
			 * Lanzar un servidor en primer plano (a través del controllerPeer)
			 * serverPort: el puerto en el que escuchar nickname: nick a usar para publicar
			 * ficheros
			 */
			if (clientStatus == OFF_BROWSER) {
				controllerPeer.foregroundServeFiles(serverPort, nickname);
			} else {
				System.out.println("* You must register before you can serve files to other users");
			}
			break;
		case NFCommands.COM_QUIT:
			/*
			 * Dar de baja el nick, cerrar sockets, etc. (a través del controllerDir)
			 */
			result = true;
			if(clientStatus == OFF_BROWSER) {
				result = controllerDir.logout(nickname);
			}
			if(result) {
				System.out.println("* Closing NanoFiles...");
				System.exit(0); // FIN
			}
			else System.out.println("* Failure to log out. Please try again");
			break;
		default:
		}
	}

	private void showMyLocalFiles() {
		System.out.println("List of files in local folder:");
		FileInfo.printToSysout(NanoFiles.db.getFiles());
	}

	/**
	 * Método para leer un comando en el browser
	 */
	private void readBrowserCommandFromShell() {
		// Pedimos un nuevo comando de browser al shell (es posible pasar el objeto
		// NFConnector para avisar de si nos llega un mensaje asíncrono del servidor)
		shell.readBrowserCommand(null);
		// Establecemos el comando tecleado (o el mensaje recibido) como comando actual
		setCurrentCommand(shell.getCommand());
		// Procesamos los posibles parámetros (si los hubiera)
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	/**
	 * Método para procesar los comandos específicos del browser
	 */
	private void enterBrowser() {
		boolean result = controllerPeer.browserEnter(browseUser);
		if (result) {
			System.out.println("* Browsing files from " + browseUser);
			clientStatus = IN_BROWSER;
			do {
				readBrowserCommandFromShell();
				processBrowserCommand();
			} while (currentCommand != NFCommands.COM_CLOSE);
			System.out.println("* You are out of the file browser");
		} else {
			System.out.println("* Cannot browse files from user " + browseUser);
		}
		// Cambiamos el estado del autómata para aceptar nuevos comandos
		clientStatus = OFF_BROWSER;
	}

	/**
	 * Método para procesar los comandos específicos del browser
	 */
	private void processBrowserCommand() {
		switch (currentCommand) {
		case NFCommands.COM_DOWNLOAD:
			controllerPeer.browserDownloadFile(downloadTargetFileHash, downloadLocalFileName);
			break;
		case NFCommands.COM_QUERYFILES:
			controllerPeer.browserQueryFiles();
			break;
		case NFCommands.COM_CLOSE:
			controllerPeer.browserClose();
			break;
		default:
		}
	}

	/**
	 * Método que comprueba si el usuario ha introducido el comando para salir de la
	 * aplicación
	 */
	public boolean shouldQuit() {
		return currentCommand == NFCommands.COM_QUIT;
	}

}
