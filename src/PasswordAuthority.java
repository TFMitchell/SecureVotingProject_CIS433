/**
 PasswordAuthority.java
 Receives only pins from Servers and concatenates them into a composite pin a voter can use (in a txt file).

 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.io.*;
import java.net.*;

public class PasswordAuthority
{
    public static void main(String args[]) throws Exception //args are in readme.md
    {
        int numServers = Integer.parseInt(args[0]);
        int listeningPort = Integer.parseInt(args[1]);
        int totalVoters = Integer.parseInt(args[2]);
        String passwords[][] = new String[totalVoters][numServers]; //form [voter password][stub number]

        //keep listening for servers until we've received password stubs from all of them
        boolean receivedAll = false;
        while (!receivedAll)
        {
            try
            {
                //getting ready to receive input
                ServerSocket serverSocket = new ServerSocket(listeningPort);
                Socket clientSocket = serverSocket.accept();
                ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

                //server sends its index and each password
                int serverIndex = Integer.parseInt(is.readUTF());
                for (String password[] : passwords)
                    password[serverIndex - 1] = is.readUTF();

                //assume we've received all stubs, until proven otherwise
                receivedAll = true;
                for (int i = 0; i < numServers; i++)
                {
                    if (passwords[0][i] == null) //we didn't receive password stubs from one of the servers
                    {
                        receivedAll = false;
                        break; //no need to check for other missing password stubs
                    }
                }
                serverSocket.close();

            } catch (Exception e) {}
        }

        //past the while loop, we're ready to write the stubs to file
        FileWriter file = new FileWriter("compositePasswords.txt");

        for (String password[] : passwords) //for each password,
        {
            for (String partialPassword : password) //for each stub of the password,
            {
                file.write(partialPassword); //...write it to the file
            }
            file.write("\n"); //add a newline for each new whole password
        }

        file.close();
    }
}