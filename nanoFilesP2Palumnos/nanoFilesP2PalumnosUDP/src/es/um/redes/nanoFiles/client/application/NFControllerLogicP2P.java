package es.um.redes.nanoFiles.client.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import es.um.redes.nanoFiles.client.comm.NFConnector;
import es.um.redes.nanoFiles.server.NFServer;
import es.um.redes.nanoFiles.server.NFServerSimple;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicP2P {
	/**
	 * El servidor de ficheros de este peer
	 */
	private NFServer bgFileServer = null;
	/**
	 * El cliente para conectarse a otros peers
	 */
	NFConnector nfConnector;
	/**
	 * El controlador que permite interactuar con el directorio
	 */
	private NFControllerLogicDir controllerDir;

	protected NFControllerLogicP2P() {
	}

	protected NFControllerLogicP2P(NFControllerLogicDir controller) {
		// Referencia al controlador que gestiona la comunicación con el directorio
		controllerDir = controller;
	}

	/**
	 * Método para ejecutar un servidor de ficheros en primer plano. Debe arrancar
	 * el servidor y darse de alta en el directorio para publicar el puerto en el
	 * que escucha.
	 * 
	 * 
	 * @param port     El puerto en que el servidor creado escuchará conexiones de
	 *                 otros peers
	 * @param nickname El nick de este peer, parar publicar los ficheros al
	 *                 directorio
	 */
	protected void foregroundServeFiles(int port, String nickname) {
		try{
			/*
			 * Las excepciones que puedan lanzarse deben ser capturadas y tratadas en
			 * este método. Si se produce una excepción de entrada/salida (error del que no
			 * es posible recuperarse), se debe informar sin abortar el programa
			 */
			// Crear objeto servidor NFServerSimple ligado al puerto especificado
			NFServerSimple servidor = new NFServerSimple(port);
			// Publicar ficheros compartidos al directorio
			boolean result = controllerDir.publishLocalFilesToDirectory(port, nickname);
			if(result){
				boolean stopSuccessful = false;
				while(!stopSuccessful) {
					// Ejecutar servidor en primer plano
					servidor.run();
					// dejar de servir en el directorio
					result = controllerDir.stopServingFilesToDirectory(nickname);
					if(!result) {
						System.out.println("* Failure to stop serving files, please try again");
					} else {
						stopSuccessful = true;
					}
				}
			} else {
				System.out.println("* Failure to serve files, please try again");
			}
		} catch (IOException e) {
			System.err.println("* TCP Server failure. Printing stack trace...");
			e.printStackTrace();
		}
	}

	/**
	 * Método para establecer una conexión con un peer servidor de ficheros
	 * 
	 * @param nickname El nick del servidor al que conectarse (o su IP:puerto)
	 * @return true si se ha podido establecer la conexión
	 */
	protected boolean browserEnter(String nickname) {
		boolean connected = false;
		/*
		 * Averiguar si el nickname es en realidad una cadena con IP:puerto, en
		 * cuyo caso no es necesario comunicarse con el directorio.
		 */
		InetSocketAddress addr;
		try{
			String IP = null;
			String PORT = null;
			if(nickname.contains(":")) {
				String [] IPPORT = nickname.split(":");
				IP = IPPORT[0];
				PORT = IPPORT[1];
				addr = new InetSocketAddress(InetAddress.getByName(IP), Integer.parseInt(PORT));
			} else {
			/*
			 * Si es un nickname, preguntar al directorio la IP:puerto asociada a
			 * dicho peer servidor.
			 */
				addr = controllerDir.lookupUserInDirectory(nickname);
				if(addr == null) {
					System.out.println("* Failure to resolve IP and Port from nickname, please try again");
					return false;
				}
			}
			/*
			 * Crear un objeto NFConnector para establecer la conexión con el peer
			 * servidor de ficheros. Si la conexión se establece con éxito, informar y
			 * devolver verdadero.
			 */
			nfConnector = new NFConnector(addr);
			connected = true;
		} catch(UnknownHostException uhe) {
			System.out.println("* Requested IP is unknown");
		} catch(IOException e) {
			System.err.println("* TCP Server failure. Printing stack trace...");
			e.printStackTrace();
		}
		
		return connected;
	}

	/**
	 * Método para descargar un fichero del peer servidor de ficheros al que nos
	 * hemos conectador mediante browser Enter
	 * 
	 * @param targetFileHash El hash del fichero a descargar
	 * @param localFileName  El nombre con el que se guardará el fichero descargado
	 */
	protected void browserDownloadFile(String targetFileHash, String localFileName) {
		/*
		 * Usar el NFConnector creado por browserEnter para descargar el fichero
		 * mediante el método "download". Se debe omprobar si ya existe un fichero con
		 * el mismo nombre en esta máquina, en cuyo caso se informa y no se realiza la
		 * descarga
		 */
		boolean downloaded = false;
		File f = new File(localFileName);
		if(!f.exists()) {
			try {
				downloaded = nfConnector.download(targetFileHash, f);
				if(!downloaded) {
					System.out.println("* Download unsuccessful, please try again");
					f.delete();
				}
				else{
					System.out.println("* Downloaded to local folder successfully");
				}
			} catch (IOException e) {
				System.err.println("* TCP Server failure, printing stack trace...");
				e.printStackTrace();
				f.delete();
				System.out.println("* Download unsuccessful, please try again");
			}
		}
		else{
			System.out.println("* A file with this name already exists, please try another name");
		}
	}

	protected void browserClose() {
		/*
		 * Cerrar el explorador de ficheros remoto (informar al servidor de que se
		 * va a desconectar)
		 */
		try{
			nfConnector.close();
		} catch (IOException e) {
			System.err.println("* TCP Server failure. Printing stack trace...");
			e.printStackTrace();
			System.out.println("* Failure to close browser, please try again");
		}
	}

	protected void browserQueryFiles() {
		try{
			List<FileInfo> queryfiles = nfConnector.searchServerFiles();
			FileInfo[] files = new FileInfo[queryfiles.size()];
			if(files.length == 0) {
				System.out.println("* No files in this server");
			}
			else {
				int i = 0;
				for(FileInfo f : queryfiles) {
					files[i] = f;
					i++;
				}
				FileInfo.printToSysout(files);
			}
		} catch (IOException e) {
			System.err.println("* TCP Server failure. Printing stack trace...");
			e.printStackTrace();
			System.out.println("* Failure to search the requested files, please try again");
		}
	}
}
