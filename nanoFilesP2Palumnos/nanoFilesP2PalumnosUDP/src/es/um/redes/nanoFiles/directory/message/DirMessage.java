package es.um.redes.nanoFiles.directory.message;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.rmi.UnknownHostException;
import java.util.Set;

import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.util.FileInfo;

import java.util.HashSet;

public class DirMessage {

	public static final int PACKET_MAX_SIZE = 65507;

	public static final byte OPCODE_SIZE_BYTES = 1;
	
	public static final String SERVER_IDENTIFIER = "   <SERVER>";

	private byte opcode;

	private int servers;
	
	private String userName;
	
	private Set<String> userlist;
	
	private int port;
	
	private FileInfo[] meta;
	
	private InetSocketAddress serverAddress;

	public DirMessage(byte operation) {
		assert (operation == DirMessageOps.OPCODE_LOGIN || operation == DirMessageOps.OPCODE_SERVE_FILES_STOP || operation == DirMessageOps.OPCODE_QUIT || operation == DirMessageOps.OPCODE_SERVE_FILES_STOP_OK || operation == DirMessageOps.OPCODE_GETFILES || operation == DirMessageOps.OPCODE_QUIT || operation == DirMessageOps.OPCODE_SERVE_FILES_OK || operation == DirMessageOps.OPCODE_SERVE_FILES_STOP_OK || operation == DirMessageOps.OPCODE_LOOKUP_USERNAME_NOTFOUND);
		opcode = operation;
	}
	
	public DirMessage(byte operation, int servers) {
		assert (operation == DirMessageOps.OPCODE_LOGIN_OK);
		opcode = operation;
		this.servers = servers;
	}
	
	public DirMessage(byte operation, String nick) {
		assert (operation == DirMessageOps.OPCODE_REGISTER_USERNAME || operation == DirMessageOps.OPCODE_LOOKUP_USERNAME || operation == DirMessageOps.OPCODE_LOGOFF || operation == DirMessageOps.OPCODE_SERVE_FILES_STOP);
		opcode = operation;
		userName = nick;
	}
	
	public DirMessage(byte operation, Set<String> userlist) {
		assert(operation == DirMessageOps.OPCODE_USERLIST);
		opcode = operation;
		this.userlist = userlist;
	}
	
	public DirMessage(byte operation, String nick, int port, FileInfo[] meta) {
		assert(operation == DirMessageOps.OPCODE_SERVE_FILES);
		opcode = operation;
		userName = nick;
		this.port = port;
		this.meta = meta;
	}
	
	public DirMessage(byte operation, FileInfo[] meta) {
		assert(operation == DirMessageOps.OPCODE_FILELIST);
		opcode = operation;
		this.meta = meta;
	}
	
	public DirMessage(byte operation, InetSocketAddress addr) {
		assert(operation == DirMessageOps.OPCODE_LOOKUP_USERNAME_FOUND);
		opcode = operation;
		serverAddress = addr;
	}
	/**
	 * Método para obtener el tipo de mensaje (opcode)
	 * @return
	 */
	public byte getOpcode() {
		return opcode;
	}

	public int getServers() {
		return servers;
	}
	
	public String getUserName() {
		if (userName == null) {
			System.err.println(
					"PANIC: DirMessage.getUserName called but 'userName' field is not defined for messages of type "
							+ DirMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return userName;
	}
	
	public Set<String> getUserList(){
		return userlist;
	}
	
	public int getPort() {
		return port;
	}
	
	public FileInfo[] getMeta() {
		return meta;
	}
	
	public InetSocketAddress getServerAddress() {
		return serverAddress;
	}

	/**
	 * Método de clase para parsear los campos de un mensaje y construir el objeto
	 * DirMessage que contiene los datos del mensaje recibido
	 * 
	 * @param data El
	 * @return
	 */
	public static DirMessage buildMessageFromReceivedData(byte[] data) {
		/*
		 * En función del tipo de mensaje, parsear el resto de campos para extraer
		 * los valores y llamar al constructor para crear un objeto DirMessage que
		 * contenga en sus atributos toda la información del mensaje
		 */
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		DirMessage mensaje = null;
		switch(opcode) {
		case DirMessageOps.OPCODE_LOGIN:
			mensaje = new DirMessage(opcode);
			break;
		case DirMessageOps.OPCODE_LOGIN_OK:
			int servidores = buf.getInt();
			mensaje = new DirMessage(opcode, servidores);
			break;
		case DirMessageOps.OPCODE_REGISTER_USERNAME:
			int longitud = buf.getInt();
			byte [] nombre = new byte[longitud];
			buf.get(nombre);
			mensaje = new DirMessage(opcode, new String(nombre));
			break;
		case DirMessageOps.OPCODE_REGISTER_USERNAME_OK:
			mensaje = new DirMessage(opcode);
			break;
		case DirMessageOps.OPCODE_REGISTER_USERNAME_FAIL:
			mensaje = new DirMessage(opcode);
			break;
		case DirMessageOps.OPCODE_GETUSERS:
			mensaje = new DirMessage(opcode);
			break;
		case DirMessageOps.OPCODE_USERLIST:
			Set<String> users = new HashSet<String>();
			int numerousers = buf.getInt();
			for(int i = 0; i < numerousers; i++) {
				int longitudnombre = buf.getInt();
				byte [] nombreusuario = new byte[longitudnombre];
				buf.get(nombreusuario);
				users.add(new String(nombreusuario));
			}
			mensaje = new DirMessage(opcode, users);
			break;
		case DirMessageOps.OPCODE_SERVE_FILES:
			int longitudNick = buf.getInt();
			byte [] nick = new byte[longitudNick];
			buf.get(nick);
			int port = buf.getInt();
			int numFicheros = buf.getInt();
			FileInfo[] metadatos = new FileInfo[numFicheros];
			for(int i = 0; i < numFicheros; i++) {
				int longitudNombre = buf.getInt();
				byte [] nombreFichero = new byte[longitudNombre];
				buf.get(nombreFichero);
				int longitudHash = buf.getInt();
				byte [] hashFichero = new byte[longitudHash];
				buf.get(hashFichero);
				long tamaño = buf.getLong();
				FileInfo f = new FileInfo(new String(hashFichero), new String(nombreFichero), tamaño, "../nf-shared/" + new String(nombreFichero));
				metadatos[i] = f;
			}
			mensaje = new DirMessage(opcode, new String(nick), port, metadatos);
			break;
		case DirMessageOps.OPCODE_SERVE_FILES_OK:
			mensaje = new DirMessage(opcode);
			break;
		case DirMessageOps.OPCODE_SERVE_FILES_STOP_OK:
			mensaje = new DirMessage(opcode);
			break;
		case DirMessageOps.OPCODE_LOOKUP_USERNAME:
			int longitudBuscar = buf.getInt();
			byte [] nombreBuscar = new byte[longitudBuscar];
			buf.get(nombreBuscar);
			mensaje = new DirMessage(opcode, new String(nombreBuscar));
			break;
		case DirMessageOps.OPCODE_LOOKUP_USERNAME_FOUND:
			try{
				int longitudIP = buf.getInt();
				byte[] ipportBytes = new byte[longitudIP];
				buf.get(ipportBytes);
				String [] IPPORT = new String(ipportBytes).split(":");
				String IP = IPPORT[0];
				String PORT = IPPORT[1];
				InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(IP), Integer.parseInt(PORT));
				mensaje = new DirMessage(opcode, addr);
			} catch (Exception e) { // UnknwownHostException, no debería saltar nunca
				e.printStackTrace();
			}
			break;
		case DirMessageOps.OPCODE_LOOKUP_USERNAME_NOTFOUND:
			mensaje = new DirMessage(opcode);
			break;
		case DirMessageOps.OPCODE_LOGOFF:
			int longitudLoff = buf.getInt();
			byte [] nombreLoff = new byte[longitudLoff];
			buf.get(nombreLoff);
			mensaje = new DirMessage(opcode, new String(nombreLoff));
			break;
		case DirMessageOps.OPCODE_QUIT:
			mensaje = new DirMessage(opcode);
			break;
		case DirMessageOps.OPCODE_SERVE_FILES_STOP:
			int longitudStop = buf.getInt();
			byte [] nombreStop = new byte[longitudStop];
			buf.get(nombreStop);
			mensaje = new DirMessage(opcode, new String(nombreStop));
			break;
		case DirMessageOps.OPCODE_GETFILES:
			mensaje = new DirMessage(opcode);
			break;
		case DirMessageOps.OPCODE_FILELIST:
			int nFiles = buf.getInt();
			FileInfo[] meta = new FileInfo[nFiles];
			for(int i = 0; i < nFiles; i++) {
				int longitudNombre = buf.getInt();
				byte [] nombreFichero = new byte[longitudNombre];
				buf.get(nombreFichero);
				int longitudHash = buf.getInt();
				byte [] hashFichero = new byte[longitudHash];
				buf.get(hashFichero);
				long tamaño = buf.getLong();
				FileInfo f = new FileInfo(new String(hashFichero), new String(nombreFichero), tamaño, "../nf-shared/" + new String(nombreFichero));
				meta[i] = f;
			}
			mensaje = new DirMessage(opcode, meta);
			break;
		default:
			mensaje = null;
		}
		return mensaje;
	}

	/*
	 * Crear métodos buildXXXXRequestMessage/buildXXXXResponseMessage para
	 * construir mensajes de petición/respuesta
	 */
	
	/**
	 * Método para construir una solicitud de ingreso en el directorio
	 * 
	 * @return El array de bytes con el mensaje de solicitud de login
	 */
	public static byte[] buildLoginRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_LOGIN);
		return bb.array();
	}

	/**
	 * Método para construir una respuesta al ingreso del peer en el directorio
	 * 
	 * @param numServers El número de peer registrados como servidor en el
	 *                   directorio
	 * @return El array de bytes con el mensaje de solicitud de login
	 */
	public static byte[] buildLoginOKResponseMessage(int numServers) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES);
		bb.put(DirMessageOps.OPCODE_LOGIN_OK);
		bb.putInt(numServers);
		return bb.array();
	}
	
	public static byte[] buildRegisterRequestMessage(String nick) {
		byte[] nombre = nick.getBytes();
		int longitud = nick.length();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
		bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME);
		bb.putInt(longitud);
		bb.put(nombre);
		return bb.array();
	}
	
	public static byte[] buildRegisterOKResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME_OK);
		return bb.array();
	}
	
	public static byte[] buildRegisterFAILResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME_FAIL);
		return bb.array();
	}
	
	public static byte[] buildUserListRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_GETUSERS);
		return bb.array();
	}
	
	public static byte[] buildUserListResponseMessage(Set<String> nicks, Set<String> servers) {
		int bytesUserList = nicks.size()*Integer.BYTES;
		for(String s : nicks) {
			bytesUserList = bytesUserList + s.length();
			if(servers.contains(s)) bytesUserList = bytesUserList + SERVER_IDENTIFIER.getBytes().length;
		}
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + (byte) bytesUserList);
		bb.put(DirMessageOps.OPCODE_USERLIST);
		bb.putInt(nicks.size());
		for(String s : nicks) {
			if(servers.contains(s)) {
				bb.putInt(s.getBytes().length + SERVER_IDENTIFIER.getBytes().length);
				byte[] buf = (new String(s.getBytes(), StandardCharsets.UTF_8) + new String(SERVER_IDENTIFIER.getBytes(), StandardCharsets.UTF_8)).getBytes();
				bb.put(buf);
			}
			else {
				bb.putInt(s.getBytes().length);
				bb.put(s.getBytes());
			}
		}
		return bb.array();
	}
	
	public static byte[] buildServeFilesRequestMessage(int port, String nickname) {
		FileInfo[] metadatos = NanoFiles.db.getFiles();
		int bytesMetaDatos = metadatos.length * (Integer.BYTES * 2 + Long.BYTES);
		for(FileInfo f : metadatos) {
			bytesMetaDatos = bytesMetaDatos + f.getName().length() + f.getHash().length();
		}
		byte[] nombre = nickname.getBytes();
		int longitud = nickname.length();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud + Integer.BYTES + Integer.BYTES + bytesMetaDatos);
		bb.put(DirMessageOps.OPCODE_SERVE_FILES);
		bb.putInt(longitud);
		bb.put(nombre);
		bb.putInt(port);
		bb.putInt(metadatos.length);
		for(FileInfo f : metadatos) {
			bb.putInt(f.getName().length());
			bb.put(f.getName().getBytes());
			bb.putInt(f.getHash().length());
			bb.put(f.getHash().getBytes());
			bb.putLong(f.getSize());
		}
		return bb.array();
	}
	
	public static byte[] buildServeFilesResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_SERVE_FILES_OK);
		return bb.array();
	}
	
	public static byte[] buildLookupUserRequestMessage(String nickname) {
		byte[] nombre = nickname.getBytes();
		int longitud = nickname.length();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
		bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME);
		bb.putInt(longitud);
		bb.put(nombre);
		return bb.array();
	}
	
	public static byte[] buildLookupUserFoundResponseMessage(InetSocketAddress server) {
		String IP = server.getAddress().getHostAddress();
		String PORT = Integer.toString(server.getPort());
		String ipport = IP + ":" + PORT;
		byte[] buf = ipport.getBytes();
		int longitud = ipport.length();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
		bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME_FOUND);
		bb.putInt(longitud);
		bb.put(buf);
		return bb.array();
	}
	
	public static byte[] buildLookupUserNotFoundResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME_NOTFOUND);
		return bb.array();
	}
	
	public static byte[] buildLogOffRequestMessage(String nickname) {
		byte[] nombre = nickname.getBytes();
		int longitud = nickname.length();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
		bb.put(DirMessageOps.OPCODE_LOGOFF);
		bb.putInt(longitud);
		bb.put(nombre);
		return bb.array();
	}
	
	public static byte[] buildLogOffResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_QUIT);
		return bb.array();
	}
	
	public static byte[] buildStopServerRequestMessage(String nick) {
		byte[] nombre = nick.getBytes();
		int longitud = nick.length();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
		bb.put(DirMessageOps.OPCODE_SERVE_FILES_STOP);
		bb.putInt(longitud);
		bb.put(nombre);
		return bb.array();
	}
	
	public static byte[] buildStopServerResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_SERVE_FILES_STOP_OK);
		return bb.array();
	}
	
	public static byte[] buildGetFilesRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_GETFILES);
		return bb.array();
	}
	
	public static byte[] buildFileListResponseMessage(FileInfo[] files) {
		int bytesMetaDatos = files.length * (Integer.BYTES * 2 + Long.BYTES);
		for(FileInfo f : files) {
			bytesMetaDatos = bytesMetaDatos + f.getName().length() + f.getHash().length();
		}
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + bytesMetaDatos);
		bb.put(DirMessageOps.OPCODE_FILELIST);
		bb.putInt(files.length);
		for(FileInfo f : files) {
			bb.putInt(f.getName().length());
			bb.put(f.getName().getBytes());
			bb.putInt(f.getHash().length());
			bb.put(f.getHash().getBytes());
			bb.putLong(f.getSize());
		}
		return bb.array();
	}
	
	/*
	 * Crear métodos processXXXXRequestMessage/processXXXXResponseMessage para
	 * parsear el mensaje recibido y devolver un objeto según el tipo de dato que
	 * contiene, o boolean si es únicamente éxito fracaso.
	 */
	

	/**
	 * Método que procesa la respuesta a una solicitud de login
	 * 
	 * @param data El mensaje de respuesta recibido del directorio
	 * @return El número de peer servidores registrados en el directorio en el
	 *         momento del login, o -1 si el login en el servidor ha fallado
	 */
	public static int processLoginResponse(byte[] data) {
		DirMessage response = buildMessageFromReceivedData(data);
		byte opcode = response.getOpcode();
		if (opcode == DirMessageOps.OPCODE_LOGIN_OK) {
			return response.getServers(); // Return number of available file servers
		} else {
			return -1;
		}
	}
	
	public static boolean processRegisterResponseMessage(byte[] data) {
		DirMessage response = buildMessageFromReceivedData(data);
		byte opcode = response.getOpcode();
		if (opcode == DirMessageOps.OPCODE_REGISTER_USERNAME_OK) {
			return true;
		} else if(opcode == DirMessageOps.OPCODE_REGISTER_USERNAME_FAIL){
			System.out.println("* Nickname already in use, please use another one");
			return false;
		}
		else return false;
	}
	
	public static Set<String> processUserListResponseMessage(byte[] data){
		DirMessage response = buildMessageFromReceivedData(data);
		byte opcode = response.getOpcode();
		if(opcode == DirMessageOps.OPCODE_USERLIST) {
			return response.getUserList();
		}
		System.out.println("* Error when retrieving user list");
		HashSet<String> vacio = new HashSet<String>();
		return vacio;
	}
	
	public static boolean processServeFilesResponseMessage(byte[] data) {
		DirMessage response = buildMessageFromReceivedData(data);
		byte opcode = response.getOpcode();
		if (opcode == DirMessageOps.OPCODE_SERVE_FILES_OK) {
			return true;
		}
		else return false;
	}
	
	public static InetSocketAddress processLookupUserResponseMessage(byte[] data) {
		DirMessage response = buildMessageFromReceivedData(data);
		byte opcode = response.getOpcode();
		if(opcode == DirMessageOps.OPCODE_LOOKUP_USERNAME_FOUND) {
			return response.getServerAddress();
		}
		else return null;
	}
	
	public static boolean processLogOffResponse(byte[] data) {
		DirMessage response = buildMessageFromReceivedData(data);
		byte opcode = response.getOpcode();
		if (opcode == DirMessageOps.OPCODE_QUIT) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean processStopServerResponse(byte[] data) {
		DirMessage response = buildMessageFromReceivedData(data);
		byte opcode = response.getOpcode();
		if (opcode == DirMessageOps.OPCODE_SERVE_FILES_STOP_OK) {
			return true;
		} else {
			return false;
		}
	}
	
	public static FileInfo[] processGetFilesResponse(byte[] data) {
		DirMessage response = buildMessageFromReceivedData(data);
		byte opcode = response.getOpcode();
		if (opcode == DirMessageOps.OPCODE_FILELIST) {
			return response.getMeta();
		}
		else {
			System.out.println("* Error when retrieving user list");
			return new FileInfo[0];
		}
	}
	
}
