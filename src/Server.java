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
import java.util.concurrent.*;

public class Server
{
    public static BigInteger pk[], sk[];
    public static BigInteger clientPQs[][];
    public static ArrayList<String> CandidateNames = new ArrayList<>();
    public static HashMap<String, BigInteger> officesAndVotes = new HashMap<>();
    public static boolean needPQsFromClients = false;
    public static int bitLength = 1024;
    public static int certainty = 64;
    public static ArrayList<Integer> portNums = new ArrayList<>();
    public static boolean running = true;

    public static void main(String args[]) throws Exception //list the clients' ports sequentially
    {
        ServerGUI myGUI = new ServerGUI(); //set up the GUI

        for (String portNum : args)
        {
            portNums.add(new Integer(portNum));
        }

        clientPQs = new BigInteger[Integer.parseInt(args[1])][2];

        for (BigInteger[] clientPQ : clientPQs)
        {
            clientPQ[0] = BigInteger.ZERO;
            clientPQ[1] = BigInteger.ZERO;
        }


        readCandidatesFromFile(); //read the candidate file and load it into the array
        retreiveKeysFromFile();

        ExecutorService executor = Executors.newCachedThreadPool();

        for (int i = 0; i < portNums.size(); i++)
        {
            executor.execute(new ServerComm());
        }

    }


    //file handler for candidate list
    public static void readCandidatesFromFile() throws Exception
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

    public static void pkGen()
    {
        BigInteger pqSums[] = new BigInteger[2];

        for(BigInteger[] clientPQ : clientPQs)
        {
            pqSums[0] = pqSums[0].add(clientPQ[0]);
            pqSums[1] = pqSums[1].add(clientPQ[1]);
        }

        pk = Crypto.getPK(pqSums);

        if (pk[0].signum() != 0)
        {
            needPQsFromClients = false;
        }

    }

    public static void getResults()
    {
        System.out.printf("These are the results:\n");

        for (String office : CandidateNames)
        {
            String names[] = office.split(", ");
            officesAndVotes.get(names[0]); //the first after splitting is the office name

            BigInteger decryptedOffice = Crypto.decrypt(officesAndVotes.get(names[0]), pk, sk);

            BigInteger remainder = decryptedOffice;

            for (int i = names.length - 1; i > 0; i--)
            {
                BigInteger whole = remainder.divide(new BigInteger( Integer.toString( (int) Math.pow(Client.totalVoters + 1, i - 1)) ));
                System.out.printf("%s got %d votes.\n", names[i], whole);
                remainder = decryptedOffice.mod(new BigInteger( Integer.toString( (int) Math.pow(Client.totalVoters + 1, i - 1)) ));

            }

        }
    }

    public static void retreiveKeysFromFile() throws Exception
    {
        File file = new File("serverKeys.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        line = br.readLine();

        if (line == null)
        {
            needPQsFromClients = true;
            return;
        }
        //else
        pk[0] = new BigInteger(line);
        line = br.readLine();
        pk[1] = new BigInteger(line);

        sk[0] = new BigInteger(line);
        line = br.readLine();
        sk[1] = new BigInteger(line);
    }

}

class ServerComm implements Runnable
{
    public void run()
    {
        try
        {
            //getting ready to receive input
            ServerSocket serverSocket = new ServerSocket(Server.portNums.remove(0));
            String line;
            Socket clientSocket = serverSocket.accept();
            ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());


            while (Server.running)
            {

                line = is.readUTF();

                if (line.equals("getPK"))  //send public key
                {
                    os.writeUTF(Server.pk[0].toString());
                    os.writeUTF(Server.pk[1].toString());


                    os.flush();

                } else if (line.equals("getSK"))  //send secret key
                {
                    os.writeUTF(Server.pk[0].toString());
                    os.writeUTF(Server.pk[1].toString());

                    os.flush();

                } else if (line.equals("getCandidates"))  //send candidates
                {

                    for (String name : Server.CandidateNames) {
                        os.writeUTF(name);
                    }

                    os.writeUTF("END"); //tells client that the list is done

                    os.flush();

                } else if (line.equals("sendingResults")) //listen for the results
                {
                    String office = is.readUTF();
                    String votes;

                    while (!office.equals("END")) {

                        votes = is.readUTF();
                        Server.officesAndVotes.put(office, new BigInteger(votes));
                        office = is.readUTF();

                    }

                    Server.getResults();


                } else if (line.equals("needKey?")) {
                    if (Server.needPQsFromClients)
                        os.writeUTF("yes");
                    else
                        os.writeUTF("no");

                    //regardless, we write back parameters
                    os.writeUTF(Integer.toString(Server.bitLength));
                    os.writeUTF(Integer.toString(Server.certainty));

                    os.flush();
                } else if (line.equals("sendingPQ")) {
                    int clientNum = Integer.parseInt(is.readUTF());
                    Server.clientPQs[clientNum][0] = new BigInteger(is.readUTF());
                    Server.clientPQs[clientNum][1] = new BigInteger(is.readUTF());

                    //try to calculate pk
                    Server.pkGen();

                } else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);
            }

        } catch(Exception e) {System.out.printf("Server couldn't start connection. %s\n", e);}


    }
}