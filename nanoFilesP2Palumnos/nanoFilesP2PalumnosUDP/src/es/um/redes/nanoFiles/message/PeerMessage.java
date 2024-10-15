package es.um.redes.nanoFiles.message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases NFServerComm y NFConnector, y se
 * codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class PeerMessage {
	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea

	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	public static final String FIELDNAME_OPERATION = "operation";
	/*
	 * Definir de manera simbólica los nombres de todos los campos que pueden
	 * aparecer en los mensajes de este protocolo (formato campo:valor)
	 */
	public static final String FIELDNAME_FILEHASH = "hash";
	public static final String FIELDNAME_FILEDATA = "data";
	public static final String FIELDNAME_FILENAME = "name";
	public static final String FIELDNAME_FILESIZE = "size";
	public static final String FIELDNAME_SEQUENCE = "seq";
	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private String operation;
	/*
	 * Crear un atributo correspondiente a cada uno de los campos de los
	 * diferentes mensajes de este protocolo.
	 */
	private String fileHash;
	
	private String fileData;
	
	private List<FileInfo> meta;
	
	private int numMensajes;
	
	/*
	 * Crear diferentes constructores adecuados para construir mensajes de
	 * diferentes tipos con sus correspondientes argumentos (campos del mensaje)
	 */
	
	public PeerMessage(String operation) {
		assert(operation.equals(PeerMessageOps.OP_FILENOTFOUND) || operation.equals(PeerMessageOps.OP_QUERYFILES) || operation.equals(PeerMessageOps.OP_CLOSE));
		this.operation = operation;
	}
	
	public PeerMessage(String operation, String data) {
		assert(operation.equals(PeerMessageOps.OP_DOWNLOAD));
		this.operation = operation;
		this.fileHash = data;
	}
	
	public PeerMessage(String operation, List<FileInfo> meta) {
		assert(operation.equals(PeerMessageOps.OP_SERVEDFILES));
		this.operation = operation;
		this.meta = meta;
	}
	
	public PeerMessage(String operation, String fileData, int seq) {
		assert (operation.equals(PeerMessageOps.OP_FILE));
		this.operation = operation;
		this.numMensajes = seq;
		this.fileData = fileData;
	}

	public String getOperation() {
		return operation;
	}
	
	public String getHash() {
		return fileHash;
	}
	
	public String getFileData() {
		return fileData;
	}
	
	public List<FileInfo> getMeta(){
		return Collections.unmodifiableList(meta);
	}
	
	public int getNumMensajes() {
		return numMensajes;
	}

	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 */
	public static PeerMessage fromString(String message) {
		/*
		 * Usar un bucle para parsear el mensaje línea a línea, extrayendo para
		 * cada línea el nombre del campo y el valor, usando el delimitador DELIMITER, y
		 * guardarlo en variables locales.
		 */
		try{
			BufferedReader reader = new BufferedReader(new StringReader(message));
			String line = reader.readLine();
			LinkedList<String> fields = new LinkedList<String>();
			LinkedList<String> values = new LinkedList<String>();
			while(!line.equals("")) {
				int index = line.indexOf(DELIMITER);
				fields.add(line.substring(0, index).toLowerCase());
				values.add(line.substring(index+1).trim());
				line = reader.readLine();
			}
			/*
			 * En función del tipo del mensaje, llamar a uno u otro constructor con
			 * los argumentos apropiados, para establecer los atributos correpondiente, y
			 * devolver el objeto creado. Se debe detectar que sólo aparezcan los campos
			 * esperados para cada tipo de mensaje.
			 */
			PeerMessage mensaje = null;
			switch(values.get(0)) {
			case PeerMessageOps.OP_DOWNLOAD:
				mensaje = new PeerMessage(values.get(0), values.get(1));
				break;
			case PeerMessageOps.OP_FILENOTFOUND:
				mensaje = new PeerMessage(values.get(0));
				break;
			case PeerMessageOps.OP_FILE:
				mensaje = new PeerMessage(values.get(0), values.get(1), Integer.parseInt(values.get(2)));
				break;
			case PeerMessageOps.OP_QUERYFILES:
				mensaje = new PeerMessage(values.get(0));
				break;
			case PeerMessageOps.OP_SERVEDFILES:
				LinkedList<FileInfo> serverFiles = new LinkedList<FileInfo>();
				for(int i = 1; i < values.size()-1; i = i + 3) {
					serverFiles.add(new FileInfo(values.get(i+2), values.get(i), Long.parseLong(values.get(i+1)),""));
				}
				mensaje = new PeerMessage(values.get(0), serverFiles);
				break;
			case PeerMessageOps.OP_CLOSE:
				mensaje = new PeerMessage(values.get(0));
				break;
			default:
				mensaje = null;
			}
			return mensaje;
				
		} catch(IOException e) {
			System.err.println("* Unexpected IO Exception. Printing stack trace...");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toEncodedString() {
		/*
		 * En función del tipo de mensaje, crear una cadena con el tipo y
		 * concatenar el resto de campos necesarios usando los valores de los atributos
		 * del objeto.
		 */
		LinkedList<String> fields = new LinkedList<String>();
		LinkedList<String> values = new LinkedList<String>();
		StringBuffer sb = new StringBuffer();
		switch (operation) {
		case PeerMessageOps.OP_DOWNLOAD:
			fields.add(FIELDNAME_OPERATION);
			values.add(operation);
			fields.add(FIELDNAME_FILEHASH);
			values.add(fileHash);
			sb.append(fields.get(0) + DELIMITER + values.get(0) + END_LINE);
			sb.append(fields.get(1) + DELIMITER + values.get(1) + END_LINE);
			sb.append(END_LINE);
			break;
		case PeerMessageOps.OP_FILE:
			fields.add(FIELDNAME_OPERATION);
			values.add(operation);
			fields.add(FIELDNAME_FILEDATA);
			values.add(fileData);
			fields.add(FIELDNAME_SEQUENCE);
			values.add(String.valueOf(numMensajes));
			sb.append(fields.get(0) + DELIMITER + values.get(0) + END_LINE);
			sb.append(fields.get(1) + DELIMITER + values.get(1) + END_LINE);
			sb.append(fields.get(2) + DELIMITER + values.get(2) + END_LINE);
			sb.append(END_LINE);
			break;
		case PeerMessageOps.OP_FILENOTFOUND:
			fields.add(FIELDNAME_OPERATION);
			values.add(operation);
			sb.append(fields.get(0) + DELIMITER + values.get(0) + END_LINE);
			sb.append(END_LINE);
			break;
		case PeerMessageOps.OP_QUERYFILES:
			fields.add(FIELDNAME_OPERATION);
			values.add(operation);
			sb.append(fields.get(0) + DELIMITER + values.get(0) + END_LINE);
			sb.append(END_LINE);
			break;
		case PeerMessageOps.OP_SERVEDFILES:
			fields.add(FIELDNAME_OPERATION);
			values.add(operation);
			sb.append(fields.get(0) + DELIMITER + values.get(0) + END_LINE);
			for(FileInfo f : meta) {
				sb.append(FIELDNAME_FILENAME + DELIMITER + f.getName() + END_LINE);
				sb.append(FIELDNAME_FILESIZE + DELIMITER + f.getSize() + END_LINE);
				sb.append(FIELDNAME_FILEHASH + DELIMITER + f.getHash() + END_LINE);
			}
			sb.append(END_LINE);
			break;
		case PeerMessageOps.OP_CLOSE:
			fields.add(FIELDNAME_OPERATION);
			values.add(operation);
			sb.append(fields.get(0) + DELIMITER + values.get(0) + END_LINE);
			sb.append(END_LINE);
			break;
		default:
		}
		return sb.toString();
	}
}
