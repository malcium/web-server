/* Morgan McCoy
 * CMPT 352
 * Homework 6
 * WebServer.java --> uses WebConnection.java to service each connection in a new thread
 */

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/* A simple multi-threaded web server server */
public class WebServer {
	public static final int PORT = 2880;  // listen on this port

	private static final Executor executor = Executors.newCachedThreadPool();

	public static void main(String[] args) throws IOException {
		ServerSocket socket = null;         // new socket for the client
		Configuration configurator = null;  // new Configuration object to handle config.xml

		try {
			socket = new ServerSocket(PORT);

			while (true) {   // continually listens on the port and executes request in new thread
				configurator = new Configuration("./conf/config.xml"); //obtain the configuration for the server
				Runnable task = new WebConnection(socket.accept(), configurator);   // create runnable with the socket and config to the WebConnection
				executor.execute(task);  // and execute it
			}
		}
		catch (IOException e) { 
			System.err.println(e);
		}
		catch (ConfigurationException ce) {
			System.out.println(ce);
		}
		finally {
			if (socket != null)
				socket.close();
		}
	}
}
