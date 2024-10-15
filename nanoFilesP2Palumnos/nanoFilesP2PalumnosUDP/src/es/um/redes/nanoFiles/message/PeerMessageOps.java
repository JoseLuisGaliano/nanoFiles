package es.um.redes.nanoFiles.message;

public class PeerMessageOps {

	/*
	 * Añadir aquí todas las constantes que definen los diferentes tipos de
	 * mensajes del protocolo entre pares.
	 */
	public static final String OP_DOWNLOAD = "download";
	public static final String OP_FILE = "file";
	public static final String OP_FILENOTFOUND = "fileNotFound";
	public static final String OP_QUERYFILES = "getFiles";
	public static final String OP_SERVEDFILES = "servedFiles";
	public static final String OP_CLOSE = "close";
}
