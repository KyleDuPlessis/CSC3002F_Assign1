import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.net.ServerSocket;

/* Kyle(dplkyl002)
 * Diya(sbrdiy001)
 * Suzan(mbssuz001)
 * 
 * CSC3002F Assignment 1
 * Title: Multi-threaded client-server based chat application with media file transmission using TCP sockets
 * 23/03/2018
 * 
 * */

// the code below is the multi-threaded chat SERVER that makes use of TCP sockets

// the ChatServer class
// - is responsible for the overall control and coordination of communication between clients
// - accepts connection requests from clients
// - creates a new client thread every time a connection is established (i.e. a new connection request from a client is accepted)
// - uses a separate thread for each client
// - shows number of clients connected to the chat server

public class ChatServer {

	private static ServerSocket serverSocket = null; // server socket
	private static Socket clientSocket = null; // client socket
	public static ArrayList<clientThread> clientThreads = new ArrayList<clientThread>(); // array list that holds clientThread objects (the chat server can accept any number of client connections)

	public static void main(String args[]) {

		
		int portNum = 3333; // the default port number dedicated to listening for connection requests from clients (has to be > 1023)


		if (args.length < 1) 
		{

			System.out.println("No port number specified by user.\nChat server started using default port number: " + portNum);  // using default port number

		} 
		else 
		{
			portNum = Integer.valueOf(args[0]).intValue();

			System.out.println("Chat server started using port number: " + portNum); // using specified port number
		}

		// open the server socket using the port number
		try {
			serverSocket = new ServerSocket(portNum);
		} catch (IOException e) {
			System.out.println("Server socket cannot be created: " + e);
		}

		// creates a client socket for each connection and pass it to a new client thread
		int numClients = 1;
		
		while (true) {
			try {

				clientSocket = serverSocket.accept(); // accept connection request from client
				clientThread currClient =  new clientThread(clientSocket, clientThreads); // pass the client socket to the new clientThread object
				clientThreads.add(currClient); // add current clientThread object to array list holding clientThread objects
				currClient.start(); // a thread is created for each new clientThread object
				System.out.println(numClients + " client(s) connected to the chat server!"); // shows number of clients connected to the chat server
				numClients++;

			} catch (IOException e) {

				System.out.println("Client could not be connected: " + e);
			}


		}

	}
}

// the clientThread class - thread for a particular client (handles individual clients in their respective threads)
// this thread:
// - opens the input and the output streams for a specific client
// - asks the client to enter their name
// - sends a welcome message to the particular client
// - notifies all the other clients that a new client has joined the chat room
// - receives data from client and checks message type (blocked, private or public)
// - transfers (blocked) text messages or media files to all clients present in the chat room, except a particular client
// - transfers (private) text messages or media files to a particular client present in the chat room
// - transfers (public) text messages or media files to all clients present in the chat room
// - notifies the client that the blocked, private or public text message or media file has been sent successfully
// - notifies all the clients present in the chat room that a client has left

class clientThread extends Thread {

	private String clientName = null;
	private ObjectInputStream inStream = null;
	private ObjectOutputStream outStream = null;
	private Socket clientSocket = null;
	private final ArrayList<clientThread> clientThreads;
	
	// for a particular client
	public clientThread(Socket clientSocket, ArrayList<clientThread> clientThreads) {

		this.clientSocket = clientSocket;
		this.clientThreads = clientThreads;

	}

	// the thread
	public void run() {

		ArrayList<clientThread> clientThreads = this.clientThreads;

		try {
			
			// opens the input and the output streams for this specific client
			inStream = new ObjectInputStream(clientSocket.getInputStream());
			outStream = new ObjectOutputStream(clientSocket.getOutputStream());

			String name;
			while (true) {

				synchronized(this) // to avoid concurrency issues - threads execute this critical section sequentially (no bad interleaving)
				{
					this.outStream.writeObject("Enter your name: ");  // asks the client to enter their name
					this.outStream.flush();
					name = ((String) this.inStream.readObject()).trim();

					if ((name.indexOf('@') == -1) || (name.indexOf('!') == -1)) { // check for illegal characters
						break;
					} else {
						this.outStream.writeObject("Client username should not contain '@' or '!' characters.");
						this.outStream.flush();
					}
				}
			}

				// sends a welcome message to the particular client
				System.out.println("Client name: " + name); 
				this.outStream.writeObject("Welcome " + name + " to our client-server based chat room. \n- to leave enter 'QUIT'");
				this.outStream.flush();

				this.outStream.writeObject("Received folder created: " + name); // notifies client about received folder
				this.outStream.flush();
				synchronized(this) // - threads execute this critical section sequentially (no bad interleaving)
				{
					
				// creates client name
				for (clientThread currClient : clientThreads)  
				{
					if (currClient != null && currClient == this) {
						clientName = "@" + name; // appends @ character to name
						break;
					}
				}

				// notifies all the other clients that a new client has joined the chat room
				for (clientThread currClient : clientThreads) {
					if (currClient != null && currClient != this) {
						currClient.outStream.writeObject("<<< A new member " + name + " has entered the chat room! >>>");
						currClient.outStream.flush();

					}

				}
			}

			
			// start conversation with clients in chat room
			while (true) {

				this.outStream.writeObject("Please enter message: "); // notifies client that a message can be entered
				this.outStream.flush();

				String responseClient = (String) inStream.readObject(); // receives message from chat client

				// checks message type
				
				// client exits chat room
				if (responseClient.startsWith("QUIT")) {
					break;
				}

				// message is private - send to a particular client in the chat room
				if (responseClient.startsWith("@")) {

					privateMsg(responseClient,name);        	

				}

				// message is blocked from a particular client - send to everyone else present in the chat room
				else if(responseClient.startsWith("#"))
				{
					blockMsg(responseClient,name);
				}

				else // message is public - send to everyone present in the chat room
				{

					publicMsg(responseClient,name);

				}

			}

			
			// end session for a particular member in the chat room
			this.outStream.writeObject("<<< Cheers " + name + " >>>");
			this.outStream.flush();
			System.out.println(name + " has left the chat room.");
			clientThreads.remove(this);


			synchronized(this) { // - threads execute this critical section sequentially (no bad interleaving)

				if (!clientThreads.isEmpty()) {

					for (clientThread currClient : clientThreads) {


						if (currClient != null && currClient != this && currClient.clientName != null) {
						
							// notifies all the clients present in the chat room that a client has left
							currClient.outStream.writeObject("<<< The member " + name + " has exited the chat room! >>>");
							currClient.outStream.flush();
							
						}




					}
				}
			}

			// close the client socket, and input and output streams
			this.inStream.close();
			this.outStream.close();
			clientSocket.close();

		} catch (IOException e) {

			System.out.println("Member session ended: " + e);

		} catch (ClassNotFoundException e) {

			System.out.println("Class not found: " + e);
		}
	}

	
	
	
	
	
	
	
	
	
	
	


	// this method transfers (blocked) text messages or media files to all clients present in the chat room, except a particular client
	void blockMsg(String responseClient, String name) throws IOException, ClassNotFoundException {

		String[] line = responseClient.split(": ", 2);

		// when transferring a blocked file
		if (line[1].split(" ")[0].equals("SEND_MEDIA"))
		{
			byte[] fileData = (byte[]) inStream.readObject();

			synchronized(this) { // - threads execute this critical section sequentially (no bad interleaving)
				for (clientThread currClient : clientThreads) {
					if (currClient != null && currClient != this && currClient.clientName != null
							&& !currClient.clientName.equals("@"+line[0].substring(1)))
					{
						currClient.outStream.writeObject("<Sending File>: "+line[1].split(" ",2)[1].substring(line[1].split("\\s",2)[1].lastIndexOf(File.separator)+1));
						currClient.outStream.writeObject(fileData);
						currClient.outStream.flush();


					}
				}

				// notifies the client that the blocked file was sent
				this.outStream.writeObject("<Blocked media file sent to all members except: "+line[0].substring(1) + ">");
				this.outStream.flush();
				System.out.println("Blocked media file sent by "+ this.clientName.substring(1) + " to all members except " + line[0].substring(1) + ".");
			}
		}

		// when transferring a blocked text message
		else 
		{
			if (line.length > 1 && line[1] != null) {
				line[1] = line[1].trim();
				if (!line[1].isEmpty()) {
					synchronized (this){ // - threads execute this critical section sequentially (no bad interleaving)
						for (clientThread currClient : clientThreads) {
							if (currClient != null && currClient != this && currClient.clientName != null
									&& !currClient.clientName.equals("@"+line[0].substring(1))) {
								currClient.outStream.writeObject("<" + name + "> " + line[1]);
								currClient.outStream.flush();


							}
						}
						
						// notifies the client that the blocked text message was sent
						this.outStream.writeObject("<Blocked text message sent to all members except: "+line[0].substring(1) + ">");
						this.outStream.flush();
						System.out.println("Blocked text message sent by "+ this.clientName.substring(1) + " to all members except " + line[0].substring(1) + ".");
					}
				}
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	// this method transfers private text messages or media files to a particular client present in the chat room
		void privateMsg(String responseClient, String name) throws IOException, ClassNotFoundException {

			String[] line = responseClient.split(": ", 2); 

			// when transferring a private file
			if (line[1].split(" ")[0].equals("SEND_MEDIA"))
			{
				byte[] fileData = (byte[]) inStream.readObject();

				for (clientThread currClient : clientThreads) {
					if (currClient != null && currClient != this && currClient.clientName != null
							&& currClient.clientName.equals(line[0]))
					{
						currClient.outStream.writeObject("<Sending File>:"+line[1].split(" ",2)[1].substring(line[1].split("\\s",2)[1].lastIndexOf(File.separator)+1));
						currClient.outStream.writeObject(fileData);
						currClient.outStream.flush();
						System.out.println(this.clientName.substring(1) + " sent a private media file to client "+ currClient.clientName.substring(1));

						// notifies the client that the private file was sent
						this.outStream.writeObject("<Private media file sent to " + currClient.clientName.substring(1) + ">");
						this.outStream.flush();
						break;

					}
				}
			}

			// when transferring a private text message
			else
			{

				if (line.length > 1 && line[1] != null) {

					line[1] = line[1].trim();

					if (!line[1].isEmpty()) {

						for (clientThread currClient : clientThreads) {
							if (currClient != null && currClient != this && currClient.clientName != null
									&& currClient.clientName.equals(line[0])) {
								currClient.outStream.writeObject("<" + name + "> " + line[1]);
								currClient.outStream.flush();

								System.out.println(this.clientName.substring(1) + " sent a private text message to client "+ currClient.clientName.substring(1));

								// notifies the client that the private text message was sent
								this.outStream.writeObject("<Private text message sent to " + currClient.clientName.substring(1) + ">");
								this.outStream.flush();
								break;
							}
						}
					}
				}
			}
		}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	// this method transfers public text messages or media files to all clients present in the chat room
	void publicMsg(String responseClient, String name) throws IOException, ClassNotFoundException {

		// when transferring a public file
		if (responseClient.split("\\s")[0].equals("SEND_MEDIA"))
		{

			byte[] fileData = (byte[]) inStream.readObject();
			synchronized(this){ // - threads execute this critical section sequentially (no bad interleaving)
				for (clientThread currClient : clientThreads) {
					if (currClient != null && currClient.clientName != null && currClient.clientName!=this.clientName) 
					{
						currClient.outStream.writeObject("<Sending File>:"+responseClient.split("\\s",2)[1].substring(responseClient.split("\\s",2)[1].lastIndexOf(File.separator)+1));
						currClient.outStream.writeObject(fileData);
						currClient.outStream.flush();

					}
				}

				// notifies the client that the public file was sent
				this.outStream.writeObject("<Public media file sent successfully to all members>");
				this.outStream.flush();
				System.out.println("Public media file sent by " + this.clientName.substring(1) + " to all members.");
			}
		}

		else
		{
			
			// when transferring a public text message
			synchronized(this){ // - threads execute this critical section sequentially (no bad interleaving)

				for (clientThread currClient : clientThreads) {

					if (currClient != null && currClient.clientName != null && currClient.clientName!=this.clientName) 
					{

						currClient.outStream.writeObject("<" + name + "> " + responseClient);
						currClient.outStream.flush();

					}
				}

				// notifies the client that the public text message was sent
				this.outStream.writeObject("<Public text message sent successfully to all members>");
				this.outStream.flush();
				System.out.println("Public text message sent by " + this.clientName.substring(1) + " to all members.");
			}

		}

	}


}




