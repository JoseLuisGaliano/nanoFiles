# Aplicación de transmisión de ficheros NanoFiles

Aplicación de escritorio Linux. Escrita en Java para la comunicación Cliente-Servidor con un Directorio en Internet, así como la comunicación y transmisión de ficheros Peer-to-Peer, apoyada en ese Directorio. Para ello, se hace uso del protocolo sin conexión y no confiable UDP para la comunicación con el Directorio, y el protocolo orientado a conexión y confiable TCP para la comunicación entre peers. La funcionalidad básica de la aplicación incluye, entre otras, conectarse y registrarse en el servidor, ofrecer tus ficheros a otros peers, conectarse a otros peers y descargar sus ficheros, etc.

Para detalles sobre el diseño y la implementación, consultar la memoria.

## Instrucciones de uso

Se incluyen dos ejecutables jar: Directory y NanoFiles. Directory se debe ejecutar en el ordenador que se desee que actúe como servidor remoto. NanoFiles es la aplicación cliente. Los ficheros que se el cliente desee compartir deben ser colocados en la carpeta nf-shared. En esa misma carpeta recibirá aquellos ficheros que descargue. 

**Fecha de desarrollo**: Febrero - Mayo 2022

Desarrollado como proyecto de prácticas de la asignatura REDES DE COMUNICACIONES en la Facultad de Informática de la Universidad de Murcia
