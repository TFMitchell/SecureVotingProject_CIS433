/**
 Server.java
 Contains the main function/entry point for the program to be run by the Tally Authority (TA).

 Generates the keypair and distributes the public key to the polling places' client software.


 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.io.*;
import java.net.*;
import java.math.*;
import java.util.*;

public class Server
{
    private static BigInteger keyPair[][];
    private static ArrayList<String> CandidateNames = new ArrayList<>();

    public static void main(String args[]) throws Exception
    {
        ServerGUI myGUI = new ServerGUI(); //set up the GUI

        //setting up arbitrary bitlength and certainty (for Paillier_cryptosystem)
        int bitLength = 512;
        int certainty = 68;

        keyPair = Crypto.keyPairGen(bitLength, certainty); //keyPair[0] is the public key and keyPair[1] is the secret key

        readCandidatesFromFile(); //read the candidate file and load it into the array

        //keep serving until the window is closed
        while(true)
        {
            Serve();
        }
    }

    //basic socket that tried to connect with a client on port 1337 of the localhost
    private static void Serve()
    {
        try
        {
            //getting ready to receive input
            ServerSocket serverSocket = new ServerSocket(1337);
            String line;
            Socket clientSocket = serverSocket.accept();
            ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

            while(true)
            {
                line = is.readUTF();

                if (line.equals("getPK"))  //send public key
                {
                    os.writeUTF(keyPair[0][0].toString());
                    os.writeUTF(keyPair[0][1].toString());

                    os.flush();
                    serverSocket.close();
                    return;
                }

                else if (line.equals("getSK"))  //send secret key
                {
                    os.writeUTF(keyPair[1][0].toString());
                    os.writeUTF(keyPair[1][1].toString());

                    os.flush();
                    serverSocket.close();
                    return;
                }

                else if (line.equals("getCandidates"))  //send candidates
                {
                    for (String name : CandidateNames)
                    {
                        os.writeUTF(name);
                    }

                    os.writeUTF("END"); //tells client that the list is done

                    os.flush();
                    serverSocket.close();
                    return;
                }

            }

        } catch(Exception e) {System.out.printf("Server couldn't start connection\n"); return;}


    }

    //file handler for candidate list
    private static void readCandidatesFromFile() throws Exception
    {
        File file = new File("candidate_list.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        line = br.readLine();

        while (line != null)
        {
            CandidateNames.add(line);
            line = br.readLine();
        }
    }

}
