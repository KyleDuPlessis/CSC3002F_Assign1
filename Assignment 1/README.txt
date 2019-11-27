Using the terminal in Ubuntu Linux:

Execution of ChatServer.java:

1. Open the terminal and navigate to the Assignment 1 folder where the ChatServer.java is located
2. Type the command "javac ChatServer.java" 
3. Type in the command "java ChatServer 1234" where 1234 is the port number
4. Alternatively, type in the command "java ChatServer" which uses the default port number 3333.

Execution of ChatClient.java:

1. Open the terminal and navigate to the Assignment 1 folder where the ChatClient.java is located
2. Type in the command "javac ChatClient.java" 
3. Type in the command "java ChatClient localhost 1234" where localhost is the host and 1234 is the port number
4. Alternatively, type in the command "java ChatClient" which uses the default host localhost and port number 3333
5. Repeat above steps for as many clients you wish to connect

Message format / structure:

For sending public messages:

<your text message>
or
SEND_MEDIA <your media file>

For sending private messages:

@<TargetClient>: <your text message>
or
@<TargetClient>: SEND_MEDIA <your media file>

For sending blocked messages:

#<TargetClient>: <your text message>
or
#<TargetClient>: SEND_MEDIA <your media file>

NOTE:
* Read report for explanation of functionality.
* Media files to be sent by clients should be stored in the Assignment 1 folder.