/**
 PinAuthority.java
 Receives only pins from Servers and concatenates them into a composite pin a voter can use.

 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.io.*;
import java.net.*;
import java.math.*;
import java.util.*;


public class PinAuthority
{
    public static void main(String args[]) throws Exception
    {
        int numServers = Integer.parseInt(args[0]);
        int listeningPort = Integer.parseInt(args[1]);
        int totalVoters = Integer.parseInt(args[2]);
        String pins[][] = new String[totalVoters][numServers];
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

                int serverIndex = Integer.parseInt(is.readUTF());

                for (String pin[] : pins) {
                    pin[serverIndex - 1] = is.readUTF();
                }

                for (int i = 0; i < numServers; i++) {
                    receivedAll = true;
                    if (pins[0][i] == null) {
                        receivedAll = false;
                        break;
                    }
                }
            } catch (Exception e) {}

        }

        FileWriter file = new FileWriter("compositePins.txt");

        for (String pin[] : pins)
        {
            for (String partialPin : pin)
            {
                file.write(partialPin);
            }
            file.write("\n");
        }

        file.close();
    }
}
