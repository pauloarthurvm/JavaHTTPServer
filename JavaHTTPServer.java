/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javahttpserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

/**
 *
 * @author paulo
 */
public class JavaHTTPServer implements Runnable {

	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPORTED = "not_supported.html";

	//	Port that will listen connection
	static final int PORT = 8080;

	//	Verbose mode
	static final boolean verbose = true;

	//	Client Connection via Socket Class
	private Socket connect;

	public JavaHTTPServer(Socket c) {
		connect = c;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		// TODO code application logic here
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port: " + PORT + " ...\n");

			//	Listen until user halts server execution
			while (true) {
				JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());

				if (verbose) {
					System.out.println("Connection open. (" + new Date() + ")");
				}

				//	Created dedicated thread to manage the client connection
				Thread thread = new Thread(myServer);
				thread.start();
			}

		} catch (Exception e) {
			System.err.println("Server Connection Error: " + e.getMessage());
		}

	}

	@Override
	public void run() {
		//	Manage particular client connection
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;
		String fileRequested = null;

		try {
			//	Read characters from client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			//	Get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			//	Get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(connect.getOutputStream());

			//	Get first line of the request from the client
			String input = in.readLine();
			//	Parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase();	//	Get the HTTP method of the client
			//	Get file requested
			fileRequested = parse.nextToken().toLowerCase();

			//	Support only GET and HEAD methods, check it
			if (!method.equals("GET") && !method.equals("HEAD")) {	//Method saying others methods are not implemented
				if (verbose) {
					System.out.println("501 Not implemented: " + method + " method.");
				}

				//	Return the not supported file to the client
				File file = new File(WEB_ROOT, METHOD_NOT_SUPORTED);
				int fileLenght = (int) file.length();
				String contentMimeType = "text/html";
				//	Red content to return to client
				byte[] fileData = readFileData(file, fileLenght);

				//Send HTTP Header with data to client
				System.out.println("HTTP/1.1 501 Not Implemented");
				System.out.println("Server: Java HTTP Server from Paulo: 1.0");
				System.out.println("Date: " + new Date());
				System.out.println("Content-type: " + fileRequested.length());
				System.out.println("");
				System.out.flush();	//Flush character output stream buffer
				//	File
				dataOut.write(fileData, 0, fileLenght);
				dataOut.flush();

			} else {	//	GET or HEAD method
				if (fileRequested.endsWith("/")) {
					fileRequested += DEFAULT_FILE;
				}

				File file = new File(WEB_ROOT, fileRequested);
				int fileLenght = (int) file.length();
				String content = getContentType(fileRequested);

				//	GET method
				if (method.equals("GET")) {	//	Return content
					byte[] fileData = readFileData(file, fileLenght);

					//Send HTTP Header with data to client
					System.out.println("HTTP/1.1 200 OK");
					System.out.println("Server: Java HTTP Server from Paulo: 1.0");
					System.out.println("Date: " + new Date());
					System.out.println("Content-type: " + content);
					System.out.println("Content-lenght: " + fileLenght);
					System.out.println("");
					System.out.flush();	//Flush character output stream buffer

					dataOut.write(fileData, 0, fileLenght);
					dataOut.flush();
				}

				if (verbose) {
					System.out.println("File " + fileRequested + " of type " + content + " returned.");
				}

			}

		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (Exception ioe) {
				System.err.println("Error with file not found exception: " + ioe.getMessage());
			}

		} catch (IOException ioe) {
			System.err.println("Server error: " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close(); //	Close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream: " + e.getMessage());
			}
			
			if(verbose){
				System.out.println("Connection closed.\n");
			}
		}
	}

	private byte[] readFileData(File file, int fileLenght) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLenght];
		fileIn = new FileInputStream(file);
		fileIn.read(fileData);
		if (fileIn != null) {
			fileIn.close();
		}

		return fileData;
	}

	//	Return supported MIME Types
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".html") || fileRequested.endsWith(".htm")) {
			return "text/html";
		} else {
			return "text/plain";
		}
	}

	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLenght = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLenght);

		//Send HTTP Header with data to client
		System.out.println("HTTP/1.1 404 File Not Found");
		System.out.println("Server: Java HTTP Server from Paulo: 1.0");
		System.out.println("Date: " + new Date());
		System.out.println("Content-type: " + content);
		System.out.println("Content-lenght: " + fileLenght);
		System.out.println("");
		System.out.flush();	//Flush character output stream buffer

		dataOut.write(fileData, 0, fileLenght);
		dataOut.flush();
		
		if(verbose){
			System.out.println("File " + fileRequested + " not found.");
		}
	}

}
