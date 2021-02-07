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
    private static HashMap<String, BigInteger> officesAndVotes = new HashMap<>();

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
                else if (line.equals("sendingResults")) //listen for the results
                {
                    String office = is.readUTF();
                    String votes;

                    while(!office.equals("END"))
                    {

                        votes = is.readUTF();
                        officesAndVotes.put(office, new BigInteger(votes));
                        office = is.readUTF();

                    }

                    getResults();

                    serverSocket.close();
                    return;

                }

            }

        } catch(Exception e) {System.out.printf("Server couldn't start connection\n");}


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

    private static void getResults()
    {
        System.out.printf("These are the results:\n");

        for (String office : CandidateNames)
        {
            String names[] = office.split(", ");
            officesAndVotes.get(names[0]); //the first after splitting is the office name

            BigInteger decryptedOffice = Crypto.decrypt(officesAndVotes.get(names[0]), keyPair[1], keyPair[0][0]);

            BigInteger remainder = decryptedOffice;

            for (int i = names.length - 1; i > 0; i--)
            {
                BigInteger whole = remainder.divide(new BigInteger( Integer.toString( (int) Math.pow(Client.totalVoters + 1, i - 1)) ));
                System.out.printf("%s got %d votes.\n", names[i], whole);
                remainder = decryptedOffice.mod(new BigInteger( Integer.toString( (int) Math.pow(Client.totalVoters + 1, i - 1)) ));

            }

        }
    }

}
