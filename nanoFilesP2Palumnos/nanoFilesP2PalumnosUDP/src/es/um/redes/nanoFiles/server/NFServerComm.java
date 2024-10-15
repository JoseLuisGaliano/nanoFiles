package es.um.redes.nanoFiles.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;

import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.message.PeerMessage;
import es.um.redes.nanoFiles.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFServerComm {
	
	private static DataInputStream dis;
	private static DataOutputStream dos;
	
	private final static double UTFLimit = 32000.0;
	
	public static void serveFilesToClient(Socket socket) {
		boolean clientConnected = true;
		// Bucle para atender mensajes del cliente
		try {
			/*
			 * Crear dis/dos a partir del socket
			 */
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			while (clientConnected) { // Bucle principal del servidor
				// Leer un mensaje de socket y convertirlo a un objeto PeerMessage
				String dataFromClient = dis.readUTF();
				PeerMessage messageFromClient = PeerMessage.fromString(dataFromClient);
				/*
				 * Actuar en función del tipo de mensaje recibido. Se pueden crear
				 * métodos en esta clase, cada uno encargado de procesar/responder un tipo de petición.
				 */
				switch(messageFromClient.getOperation()) {
				case PeerMessageOps.OP_DOWNLOAD:
					processDownloadRequest(messageFromClient.getHash());
					break;
				case PeerMessageOps.OP_QUERYFILES:
					processQueryFilesRequest();
					break;
				case PeerMessageOps.OP_CLOSE:
					clientConnected = false;
					break;
				default:
					break;
				}
			}
			socket.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	private static void processDownloadRequest(String fileHash) {
		String path = NanoFiles.db.lookupFilePath(fileHash);
		try{
			// sacar bytes del fichero
			File f = new File(path);
			FileInputStream fis = new FileInputStream(f);
			long filelength = f.length();
			byte data[] = new byte[(int) filelength];
			fis.read(data);
			fis.close();
			// enviar bytes en mensajes (UFTLimit es un limite por debajo del maximo de writeUTF)
			int dataLength = data.length;
			int numMensajes = (int) (dataLength / UTFLimit + 1);
			long bytesEnviados = 0;
			PeerMessage mensaje;
			for (int i = 0; i < numMensajes; i++) {
				byte buf[] = new byte[(int) UTFLimit];
				if (numMensajes != 1) {
					if(i == numMensajes - 1) {
						if(dataLength - bytesEnviados > 0) { 
							buf = Arrays.copyOfRange(data, (int) bytesEnviados, dataLength);
						}
					}
					else {
						buf = Arrays.copyOfRange(data, i * (int) UTFLimit, i * (int) UTFLimit + (int) UTFLimit);
						bytesEnviados = bytesEnviados + (long) UTFLimit;
					}
				}
				else { 
					buf = Arrays.copyOfRange(data, i * (int) UTFLimit, dataLength);
				}
					
				String encoded = java.util.Base64.getEncoder().encodeToString(buf);
				mensaje = new PeerMessage(PeerMessageOps.OP_FILE, encoded, numMensajes-i-1);
				String respuesta = mensaje.toEncodedString();
				dos.writeUTF(respuesta);
			}
		} catch (NullPointerException e) {
			System.out.println("* NullPointerException found, printing stack trace...");
			e.printStackTrace();
			PeerMessage mensaje = new PeerMessage(PeerMessageOps.OP_FILENOTFOUND);
			String encoded = mensaje.toEncodedString();
			try{
				dos.writeUTF(encoded);
			} catch(IOException e2) {
				e2.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void processQueryFilesRequest() {
		FileInfo[] files = NanoFiles.db.getFiles();
		LinkedList<FileInfo> serverFiles = new LinkedList<FileInfo>();
		for(FileInfo f : files) {
			serverFiles.add(f);
		}
		PeerMessage response = new PeerMessage(PeerMessageOps.OP_SERVEDFILES, serverFiles);
		String encoded = response.toEncodedString();
		try{
			dos.writeUTF(encoded);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
