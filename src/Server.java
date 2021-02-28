/**
 Server.java
 Contains the main function/entry point for the program to be run by the Tally Authority (TA).

 Generates the keypair and distributes the public key to the polling places' client software.


 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import com.sun.media.sound.SF2InstrumentRegion;

import java.io.*;
import java.net.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;

public class Server
{
    public static ArrayList<String> CandidateNames = new ArrayList<>();
    public static HashMap<String, BigInteger> officesAndVotes = new HashMap<>();
    public static boolean hasN = false;
    public static int bitLength = 16;
    public static  int myListeningPort;
    public static int serverPortNums[];
    public static int clientPortNums[];
    public static boolean running = true;
    public static HashMap<String, BigInteger> encryptedSubtotals; //keeps track of subtotals by office
    public static Random rand = new Random();
    public static int myIndex, numServers, numClients;
    private static int totalVoters = 100;
    public static int currentClientNum = 0;
    public static BigInteger N, theta;
    private static Socket socket;
    private static ObjectOutputStream os;
    private static ObjectInputStream is;
    public static BigInteger pq[];
    public static BigInteger pShares[];
    public static BigInteger qShares[];
    public static BigInteger gg;
    public static BigInteger Qi[];
    public static boolean myTurn = false;
    public static int nextServerPort;
    private static int iterationNum = 0;

    public static float candidate_counts[][];

    public static void main(String args[]) throws Exception
    {

        ServerGUI myGUI = new ServerGUI(); //set up the GUI
        ExecutorService executor = Executors.newCachedThreadPool();

        //interpret parameters
        myIndex = Integer.parseInt(args[0]);
        numServers = Integer.parseInt(args[1]);



        if (myIndex == 1)
            myTurn = true;




        serverPortNums = new int[numServers];
        for (int i = 2; i < 2 + numServers; i++)
        {
            serverPortNums[i-2] = Integer.parseInt(args[i]);
        }

        myListeningPort = serverPortNums[myIndex - 1];

        executor.execute(new Listening());

        if (myIndex == numServers)
            nextServerPort = serverPortNums[0];
        else
            nextServerPort = serverPortNums[myIndex];


        numClients = Integer.parseInt(args[numServers + 2]);
        clientPortNums = new int[numClients];
        for (int i = numServers + 3; i < numServers + 3 + numClients; i++)
        {
            clientPortNums[i - numServers - 3] = Integer.parseInt(args[i]);
            executor.execute(new ClientComm());
        }

        pShares = new BigInteger[numServers];
        qShares = new BigInteger[numServers];
        Qi = new BigInteger[numServers];

        readCandidatesFromFile(); //read the candidate file and load it into the array
        retreiveKeysFromFile();
        readFile();

        genBiprimalN();

        Thread.sleep(5000);

        System.out.printf("N: %s\n", N);


        hasN = true;

    }

    public static void genBiprimalN() throws Exception
    {
        pq = Crypto.genPQ(myIndex, bitLength, rand);

        while (!myTurn) { Thread.sleep(50);
        //System.out.printf("waiting 114\n");
        }

        for (int portNum : serverPortNums)
        {
            while (true)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("sendingPQ");
                    os.writeUTF(Integer.toString(myIndex - 1));
                    os.writeUTF(pq[0].toString());
                    os.writeUTF(pq[1].toString());
                    os.flush();

                    break;
                } catch (Exception e) {}
            }
        }


        while (true)
        {
            try
            {
                socket = new Socket(InetAddress.getLocalHost().getHostAddress(), nextServerPort);
                os = new ObjectOutputStream(socket.getOutputStream());
                is = new ObjectInputStream(socket.getInputStream());
                os.writeUTF("yourTurn");
                myTurn = false;
                os.flush();

                break;
            } catch (Exception e) {}
        }


        while (!myTurn) { Thread.sleep(50);
            //System.out.printf("waiting 155\n");
        }


        BigInteger pShareSum = BigInteger.ZERO;
        BigInteger qShareSum = BigInteger.ZERO;


        for (int i = 0; i < numServers; i++)
        {
            pShareSum = pShareSum.add(pShares[i]);
            qShareSum = qShareSum.add(qShares[i]);
        }

        N = pShareSum.multiply(qShareSum);


        if (myIndex == 1)
        {
            gg = Crypto.getGG(N, rand);
            for (int portNum : serverPortNums)
            {
                while (true)
                {
                    try
                    {
                        socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                        os = new ObjectOutputStream(socket.getOutputStream());
                        is = new ObjectInputStream(socket.getInputStream());
                        os.writeUTF("sendingGG");

                        os.writeUTF(gg.toString());
                        os.flush();
                        break;
                    } catch (Exception e) { }
                }
            }
        }


        for (int portNum : serverPortNums)
        {
            while (true)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("sendingQi");
                    os.writeUTF(Integer.toString(myIndex - 1));
                    os.writeUTF(Crypto.getQi(N, gg, pq, myIndex).toString());
                    os.flush();

                    break;

                } catch (Exception e) { }
            }
        }
        System.out.printf("N: %s iteration %d\n", N, iterationNum++);

       while (true)
        {
            try
            {
                socket = new Socket(InetAddress.getLocalHost().getHostAddress(), nextServerPort);
                os = new ObjectOutputStream(socket.getOutputStream());
                is = new ObjectInputStream(socket.getInputStream());
                os.writeUTF("yourTurn");
                myTurn = false;
                os.flush();

                break;
            } catch (Exception e) {}
        }


        while (!myTurn) { Thread.sleep(50);
            //System.out.printf("waiting 233\n");
        }




        if (!Crypto.isBiprimal(N, rand, Qi))
            genBiprimalN();

        while (true)
        {
            try
            {
                socket = new Socket(InetAddress.getLocalHost().getHostAddress(), nextServerPort);
                os = new ObjectOutputStream(socket.getOutputStream());
                is = new ObjectInputStream(socket.getInputStream());
                os.writeUTF("yourTurn");
                myTurn = false;
                os.flush();

                break;
            } catch (Exception e) {}
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


    public static void getResults()
    {
        System.out.printf("These are the results:\n");

        candidate_counts = new float [CandidateNames.size()][];
        int row = 0;
        for (String office : CandidateNames)
        {
            String names[] = office.split(", ");


            officesAndVotes.get(names[0]); //the first after splitting is the office name
            candidate_counts[row] = new float[names.length - 1];

            BigInteger decryptedOffice = Crypto.decrypt(officesAndVotes.get(names[0]), N, theta);

            BigInteger remainder = decryptedOffice;

            for (int i = names.length - 1; i > 0; i--)
            {
                BigInteger whole = remainder.divide(new BigInteger( Integer.toString( (int) Math.pow(totalVoters + 1, i - 1)) ));
                System.out.printf("%s got %d votes.\n", names[i], whole);

                //save the count for access by GUI
                candidate_counts[row][i-1] = whole.floatValue();

                remainder = decryptedOffice.mod(new BigInteger( Integer.toString( (int) Math.pow(totalVoters + 1, i - 1)) ));

            }
            row++;
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
            return;
        }
        //else
        N = new BigInteger(line);
        theta = new BigInteger(br.readLine());

        hasN = true;
    }
    //reads existing subtotals from the appropriately-named file in the working directory
    private static void readFile() throws Exception
    {
        encryptedSubtotals = new HashMap<>(); //return value

        File file = new File("encryptedSubtotals.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        String nameVotes[]; //[0]is a candidate's name and [1] is their encrypted subtotal

        //every line of the file is an office and its votes
        line = br.readLine();
        while (line != null)
        {
            nameVotes = line.split(": ");
            encryptedSubtotals.put(nameVotes[0], new BigInteger(nameVotes[1]));
            line = br.readLine();
        }

        //now try to update the file on disk
        try
        {
            updateFile(encryptedSubtotals);
        } catch(Exception e) {System.out.printf("Couldn't write to file.\n");}

    }

    //writes to the subtotal file after voting is done
    private static void updateFile(HashMap<String, BigInteger> records) throws Exception
    {
        FileWriter file = new FileWriter("encryptedSubtotals.txt");

        //for every office
        for (HashMap.Entry<String, BigInteger> entry : records.entrySet())
        {
            file.write(entry.getKey() + ": " + entry.getValue() + "\n");
        }

        file.close();
    }

}

class ClientComm implements Runnable
{

    public void run()
    {
        int myIndex = Server.currentClientNum++;

        try
        {
            //getting ready to receive input
            ServerSocket serverSocket = new ServerSocket(Server.clientPortNums[myIndex]);
            String line;
            Socket clientSocket = serverSocket.accept();
            ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

            while (Server.running)
            {

                line = is.readUTF();

                if (line.equals("getN"))  //send public key
                {
                    os.writeUTF(Server.N.toString());

                    os.flush();
                }

                else if (line.equals("getCandidates"))  //send candidates
                {

                    for (String name : Server.CandidateNames) {
                        os.writeUTF(name);
                    }

                    os.writeUTF("END"); //tells client that the list is done

                    os.flush();

                }

                else if (line.equals("haveKey?"))
                {

                    if (!Server.hasN)
                        os.writeUTF("no");
                    else
                    {
                        os.writeUTF("yes");
                        os.writeUTF(Integer.toString(Server.bitLength));
                    }

                    os.flush();
                }


                else if (line.equals("sendingBallot"))
                {
                    //get the subtotal for this particular office
                    BigInteger officeSubTotal;

                    BigInteger encryptedVote = new BigInteger(is.readUTF());

                    String index = is.readUTF();

                    if (Server.encryptedSubtotals.get(index) == null)
                        officeSubTotal = new BigInteger("0");
                    else
                        officeSubTotal = Server.encryptedSubtotals.get(index);

                    officeSubTotal = Crypto.addEncrypted(officeSubTotal, encryptedVote, Server.N); //add the new vote to it

                    Server.encryptedSubtotals.put(index, officeSubTotal); //update the hashmap

                }

                else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);


            }

        }catch(Exception e) {System.out.printf("Server couldn't start connection. %s\n", e);}


    }
}

class Listening implements Runnable {

    public void run() {


        while (Server.running)
        {
            try
            {
                //getting ready to receive input
                ServerSocket serverSocket = new ServerSocket(Server.myListeningPort);
                String line;
                Socket clientSocket = serverSocket.accept();
                ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

                line = is.readUTF();

                if (line.equals("sendingPQ"))
                {
                    int index = Integer.parseInt(is.readUTF());
                    Server.pShares[index] = new BigInteger(is.readUTF());
                    Server.qShares[index] = new BigInteger(is.readUTF());
                }
                else if (line.equals("sendingGG"))
                {
                    Server.gg = new BigInteger(is.readUTF());
                }
                else if (line.equals("sendingQi"))
                {
                    int index = Integer.parseInt(is.readUTF());
                    Server.Qi[index] = new BigInteger(is.readUTF());
                }

                else if (line.equals("yourTurn"))
                {
                    Server.myTurn = true;
                }

                else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);

                serverSocket.close();
            } catch (Exception e) {
                System.out.printf("Server couldn't start connection. Wonder which line. %s\n", e); }

        }
    }
}
