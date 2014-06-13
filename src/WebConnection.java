/* Morgan McCoy
 * CMPT 352
 * Homework 6
 * WebConnection.java --> services connections for WebServer.java
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class WebConnection implements Runnable {
	public static final int SIZE = 4096;   // size of the byte array for sending data to the client
	private Socket client;
	private  Configuration config;	

	public WebConnection(Socket client, Configuration configurator){  // constructor to create the socket
		this.client = client;
		this.config = configurator;
	}

	public void run() { 
		String serverName = config.getServerName();  // server name for the header
		BufferedReader fromClient = null;			 // stream from the client
		DataOutputStream toClient = null;			 // stream to the client
		FileInputStream requestedResource = null;    // stream from the requested resource
		FileOutputStream logStream = null;           // stream to the log file
		PrintStream logPrint = null;                 // print stream to the log file
		String browserReq = null;	                 // top line of the client request
		String dontCare = null;                      // the rest of the client request
		boolean FourOFourError = false;              // flag for a 404 error
		int bytesServed = 0;                         // number of bytes served up from server to the client
		String requestNumber = null;                 // resultant status code for log file
		String httpHeader = null;                    // http header status back to the client
		Date date = new Date();                      // date object for log file
		File servedFile = null;                      // file to be served to client
		String contentType = null;                   // type of content for http header
		String clientIP = null;                      // client IP address for log file
		File log = null;                             // file object for log file output

		try {
			clientIP = client.getInetAddress().getHostAddress();  // obtain the client's IP address for the log file			

			fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));  // get the  IO streams to/from client
			toClient = new DataOutputStream(client.getOutputStream());			

			browserReq = fromClient.readLine();  // read the first line of the HTTP request from the client	
			if (browserReq == null)              // if it's null, close the socket
				client.close();
			
			System.out.println(browserReq); // prints it to the console
			
			if(fromClient.readLine() != null){     // if the next read line from the client is not null
				while((dontCare = fromClient.readLine()).length() != 0){  // print out the rest of the request from the client to the console
						System.out.println(dontCare);
				}
			}
			
			if(browserReq != null){           // if the browser request is not null, implement the rest of the try block
				
				StringTokenizer browserReqTokens = new StringTokenizer(browserReq);  // tokenize the request line for parsing
				browserReqTokens.nextToken();                      // skip over the GET
				String fileName = browserReqTokens.nextToken();    // name of the requested resource
				String filePath = null;

				if(fileName.equals("/"))                      // if no specific resource is requested,
					filePath = config.getDefaultDocument();   // the resource path is the default document			
				else
					filePath = config.getDocumentRoot() + fileName;  // otherwise get the root directory and append the requested resource

				try{
					servedFile = new File(filePath);                       // try to get the file
					requestedResource = new FileInputStream(servedFile);   // and create the file input stream 
				}
				catch (FileNotFoundException f){
					FourOFourError = true;                    // but throw a 404 error if it fails
				}

				if(!FourOFourError){  // if the file input stream was successfully created for the requested resource,
					requestNumber = "200";
					httpHeader = "HTTP/1.1 200 OK";     // set the HTTP status line
					contentType = "Content-Type: " + contentHelper(fileName);  // and set the content type with a helper method
				}
				else if(FourOFourError){   // if the file input stream was not successfully created, set the header info and get the 404.html file
					requestNumber = "404";
					httpHeader = "HTTP/1.1 404 Not Found";
					contentType = "Content-Type: text/html";
					servedFile = new File("./conf/404.html");
					requestedResource = new FileInputStream(servedFile);
				}
				else if(browserReqTokens.nextToken() != "HTTP/1.1"){  // if the next token in the browser request isn't proper HTTP/1.1 syntax,
					requestNumber = "400";
					httpHeader = "HTTP/1.1 400 Bad Request";  // and set the header and 400.html file accordingly
					contentType = "Content-Type: text/html";
					servedFile = new File("./conf/400.html");
					requestedResource = new FileInputStream(servedFile);
				}

				bytesServed = (int)servedFile.length();      // save the size of the file to be served

				toClient.writeBytes(httpHeader);           // write header to client starting with the HTTP status code
				toClient.writeBytes("Date: " + date.toString());      // date of the service
				toClient.writeBytes(serverName);                      // the server name
				toClient.writeBytes(contentType);                     // the content type of the content served
				toClient.writeBytes(Integer.toString(bytesServed));   // the content length
				toClient.writeBytes("Connection: close");             // non-persistent
				toClient.writeBytes("\r\n\r\n");                          // blank line in-between the header and requested resource
				toClient.flush();

				byte[] buffer = new byte[SIZE];           // send the requested resource to the client using this byte buffer
				int bytes = 0;                            // and byte index
				while((bytes = requestedResource.read(buffer)) != -1) {  // writes to output stream until end-of-file (-1)
					toClient.write(buffer, 0, bytes);
					toClient.flush();
				}   

				requestedResource.close();  // close the input stream for the resource
				fromClient.close();         // close the stream from the client
				toClient.close();           // close the stream to the client
				client.close();             // close the socket

				log = new File(config.getLogFile());  // access the log file defined in config.xml
				logStream = new FileOutputStream(log, true);  // get an output stream to the log file
				logPrint = new PrintStream(logStream);  // and wrap it in a PrintStream
				browserReq = "\""+ browserReq +"\"";    // add quotes to the browserReq string

				/* write to the log file */
				logPrint.println(clientIP +" ["+ date.toString() +"] "+ browserReq +" "+ requestNumber +" "+ Integer.toString(bytesServed));

				logPrint.close();   // close the PrintStream
				logStream.close();  // close the output stream
			}
		}
		catch (java.io.IOException e) {   // catch block for io exceptions
			System.err.println(e);
		}		
	}

	private static String contentHelper(String fileName)         // helper method for header construction
	{
		if(fileName.endsWith(".htm") || fileName.endsWith(".html"))  
			return "text/html";                      // if the file name ends in html, gif, jpg, png or txt, return the correct header field
		if(fileName.endsWith(".gif"))
			return "image/gif";
		if(fileName.endsWith(".jpg"))
			return "image/jpg";
		if(fileName.endsWith(".png"))
			return "image/png";
		if(fileName.endsWith(".txt"))
			return "text/plain";		
		return "unknown";
	}
}

