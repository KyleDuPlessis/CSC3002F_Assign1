import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Delayed;

/* Kyle(dplkyl002)
 * Diya(sbrdiy001)
 * Suzan(mbssuz001)
 * 
 * CSC3002F Assignment 1
 * Title: Multi-threaded client-server based chat application with media file transmission using TCP sockets
 * 23/03/2018
 * 
 * */

// the code below is the multi-threaded chat CLIENT that makes use of TCP sockets

// two threads are used:

// one thread (thread 1):
// - captures data from the standard input (user)
// - checks if there is a media file to be transfered (for blocked, private and public messages)
// - processes media file to be sent
// - sends text message / media file to chat server


// the other thread (thread 2):
// - checks for / creates a received folder for each client for media file transfer
// - receives media file from chat server
// - transfers media file to the particular client's received folder
// - the client has the option of receiving or declining the media file (incorporates bandwidth constraint - media file transmission)
// - receives text messages from the server and prints it to the screen (standard output)
// - terminates the client on quit

public class ChatClient implements Runnable {

	private static Socket clientSocket = null; // client socket
	private static ObjectOutputStream outStream = null; // object output stream
	private static ObjectInputStream inStream = null; // object input stream
	private static BufferedReader userInput = null; // standard input / output
	// from the user
	private static BufferedInputStream buffInStream = null; // for media file transfer
	private static boolean socketClosed = false; // close the client socket
	public static int flag = 0; // flag value for accepting or declining media files

	public static void main(String[] args) {

		int portNum = 3333; // default port number
		String host = "localhost"; // default host

		if (args.length < 2) {
			System.out.println("Using default chat server: " + host + "\nUsing default port number: " + portNum); // using default host and port number
		} else {
			host = args[0];
			portNum = Integer.valueOf(args[1]).intValue();
			System.out.println("Using chat server: " + host + "\nUsing port number: " + portNum); // using specified host and port number
		}

		// open a client socket using the specified host and port number, and open the
		// input and output streams
		try {
			clientSocket = new Socket(host,portNum);
			outStream = new ObjectOutputStream(clientSocket.getOutputStream());
			inStream = new ObjectInputStream(clientSocket.getInputStream());
			
			userInput  = new BufferedReader(new InputStreamReader(System.in)); // standard input from the user
		} catch (UnknownHostException e) {
			System.err.println("Unable to connect to host. \n" + host
					+ " not found.");
		} catch (IOException e) {
			System.err.println("Unable to acquire I/O to connect to the host: "
					+ host);
		}

		// after successful initialisation
		if (clientSocket != null && outStream != null && inStream != null) {
			try {

				// thread 1
				new Thread(new ChatClient()).start();
				while (!socketClosed) { // while the client socket is open

					String message = (String) userInput .readLine().trim(); // captures data from the standard input
					
					if (message.equalsIgnoreCase("YES")){
						
						flag = 1;
						System.out.println("Please wait a moment...");
						continue;
						
					}
					if (message.equalsIgnoreCase("NO")){
						
						System.out.println("Please wait a moment...");
						continue;
						
					}

					// checks if there is a media file to be transfered (for blocked and private messages)
					if ((message.split(": ").length > 1))
					{
						if (message.split(": ")[1].startsWith("SEND_MEDIA"))
						{
							File f = new File((message.split(": ")[1]).split(" ", 2)[1]); // get the media file name first
							
							if (!f.exists()) // if the media file does not exist
							{
								System.out.println("Media file entered does NOT exist.");
								continue;
							}
							
							// media file processing and transfer - send media file to chat server
							byte [] bArray  = new byte [(int)f.length()];
							buffInStream = new BufferedInputStream(new FileInputStream(f));
							while (buffInStream.read(bArray,0,bArray.length)>=0)
							{
								buffInStream.read(bArray,0,bArray.length);
							}
							outStream.writeObject(message); // sends text message to the chat server (if no media file specified)
							outStream.writeObject(bArray); // sends media file to the chat server
							outStream.flush();

						}
						else
						{
							outStream.writeObject(message); 
							outStream.flush();
						}

					}

					// checks if there is a media file to be transfered (public messages)
					else if (message.startsWith("SEND_MEDIA")) 
					{

						File f = new File(message.split(" ", 2)[1]); // get the media file name first
						
						if (!f.exists()) // if the media file does not exist
						{
							System.out.println("Media file entered does NOT exist.");
							continue;
						}
						
						// media file processing and transfer - send media file to chat server
						byte [] bArray  = new byte [(int)f.length()];
						buffInStream = new BufferedInputStream(new FileInputStream(f));
						while (buffInStream.read(bArray,0,bArray.length)>=0)
						{
							buffInStream.read(bArray,0,bArray.length);
						}
						outStream.writeObject(message); // sends text message to the chat server (if no media file specified)
						outStream.writeObject(bArray); // sends media file to the chat server
						outStream.flush();

					}

					
					else 
					{
						outStream.writeObject(message); // sends text message to the chat server (if no media file specified)
						outStream.flush();
					}


				}

				
				// close the client socket, and input and output streams
				outStream.close();
				inStream.close();
				clientSocket.close();
			} catch (IOException e) 
			{
				System.err.println("IOException: " + e);
			}
		
			
		}
	}

	// thread 2
	public void run() {
		
		// keep reading from the socket until "<<< Cheers " is received from the
				// server
		String serverResponse;
		FileOutputStream fileOutStream = null;
		BufferedOutputStream buffOutStream = null;
		String fName = null;
		byte[] infile = null;
		File dirName = null;
		String fullPath;
		String clientName;

		try {


			while ((serverResponse = (String) inStream.readObject()) != null)  { // receives data from the server
				
				// checks for / creates a received folder for each client for media file transfer
				if (serverResponse.startsWith("Received folder created: "))
				{
					
					clientName = serverResponse.substring(25, serverResponse.length()); // get client's name
					
					clientName = "Received_Files_" + clientName; // name of received folder
					dirName = new File((String) clientName);

					if (!dirName.exists()) // if received folder for client does NOT exist
					{
						dirName.mkdir(); // creates a new received folder for particular client

						System.out.println("New received folder created for this client!");

					}

					else
					{
						System.out.println("Received folder for this client already exists!");
					}
				}

				// checks for incoming media files
				else if (serverResponse.startsWith("<Sending File>"))
				{

					try
					{
						// media file transfer - receives media file from chat server
						// transfers media file to client's received folder
						fName = serverResponse.split(":")[1];
						fullPath = dirName.getAbsolutePath()+"/"+fName; 
						infile = (byte[]) inStream.readObject();
						fileOutStream = new FileOutputStream(fullPath);
						buffOutStream = new BufferedOutputStream(fileOutStream);
						
						
						
						
						
						
						
						
						
						
						
						
						
						
						
						// incorporates bandwidth constraint - media file transmission
						// the client has the option of receiving or declining the media file
						
						System.out.println("Do you want the media file? (YES/NO)\n[You have 15 seconds to answer else media file is DECLINED]");
						try{
						Thread.sleep(20000);}catch(InterruptedException e){
							System.out.println("Interrupted my sleep: " + e);
						}
						if (flag == 1){
							buffOutStream.write(infile);
							buffOutStream.flush();
							System.out.println("Media file received.");}
						else{
							System.out.println("Media file declined.");
							buffOutStream.flush();
						}
						
						
						
						
						
						
						
						
						
						
						
						
					}
					finally // close the streams
					{
						if (fileOutStream != null) fileOutStream.close();
						if (buffOutStream != null) buffOutStream.close();
					}

				}

				

				else
				{
					System.out.println(serverResponse); // prints server response to the screen (standard output)
				}


				
				// quit application
				if (serverResponse.indexOf("<<< Cheers ") != -1) // once
					// "<<< Cheers "
					// is received
					// from the
					// server, break
				
					break;
			}

			socketClosed = true; // then close the client socket
			System.exit(0); // terminates application

		} catch (IOException | ClassNotFoundException e) {

			System.err.println("Chat server stopped unexpectedly: " + e);

		}
	}
}
