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
    public static HashMap<String, BigInteger> encryptedSubtotals; //keeps track of subtotals by office

    public static boolean hasN = false; //do we have an N value yet? used for communicating with client
    public static boolean running = true; //running, used to shutdown listening thread
    public static boolean myTurn = false; //ensures synchronization and eliminates race conditions

    public static int bitLength = 32; //bitlength of p and q values
    public static int myListeningPort; //the port this server listens on
    public static int serverPortNums[]; //a list of all servers' ports
    public static int clientPortNums[]; //a list of my clients' ports
    private static int myIndex, numServers, numClients;
    private static int totalVoters = 100; //this is used for encoding more than two candidates for each office (we mod by this^candidateNumber)
    public static int currentClientNum = 0; //used for enumerating the clients, so they know which portNum to use
    private static int nextServerPort; //the port of the server at the next index (or first index for last server)
    private static int iterationNum = 0; //keeps track of how many iterations to find a biprimal N

    private static Random rand = new Random();

    private static Socket socket;
    private static ObjectOutputStream os;
    private static ObjectInputStream is;

    public static BigInteger N, gg; //public key and coprime
    private static BigInteger theta, delta; //decryption key and delta, which is numServers! (used because interpolation divides by numServers, numservers-1, ..., but needs to be integers
    private static BigInteger pShareSum, qShareSum; //sums of p and q shares, to find the share of N, which can be revealed with lagrange interpolation
    private static BigInteger pq[]; //p and q for this server
    public static BigInteger pShares[], qShares[], nShares[], thetaShares[], Qi[]; //shares come from each of the Servers, including itself

    public static float candidate_counts[][]; //this is used for displaying the results in the GUI

    public static void main(String args[]) throws Exception
    {
        //interpret parameters
        myIndex = Integer.parseInt(args[0]);
        numServers = Integer.parseInt(args[1]);

        serverPortNums = new int[numServers];
        for (int i = 2; i < 2 + numServers; i++)
        {
            serverPortNums[i-2] = Integer.parseInt(args[i]);
        }

        numClients = Integer.parseInt(args[numServers + 2]);
        clientPortNums = new int[numClients];
        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = numServers + 3; i < numServers + 3 + numClients; i++)
        {
            clientPortNums[i - numServers - 3] = Integer.parseInt(args[i]);
            executor.execute(new ClientComm());
        }

        //setting up things we can now infer
        myListeningPort = serverPortNums[myIndex - 1];
        delta = Crypto.factorial(numServers);
        pShares = new BigInteger[numServers];
        qShares = new BigInteger[numServers];
        nShares = new BigInteger[numServers];
        thetaShares = new BigInteger[numServers];
        Qi = new BigInteger[numServers];

        if (myIndex == 1)
            myTurn = true;

        if (myIndex == numServers)
            nextServerPort = serverPortNums[0];
        else
            nextServerPort = serverPortNums[myIndex];

        ServerGUI myGUI = new ServerGUI(); //set up the GUI
        executor.execute(new Listening()); //listening port on its own thread

        readCandidatesFromFile(); //read the candidate file and load it into the array
        retreiveKeysFromFile();
        readFile();

        genBiprimalN(); //try to make an N
        System.out.printf("N: %s\n", N);

        BigInteger message = new BigInteger("12345"); //sample encrypted message
        message = Crypto.encrypt(message, BigInteger.ONE, N); //there would be a real random Bigint here
        message = Crypto.addEncrypted(message, message, N); //testing out the add functionality

        hasN = true; //we were able to make an N, so we note that for when clients ask

        shareDecryptionKey(); //time to reveal the decryption key theta
        System.out.printf("Theta: %s\n", theta);

        System.out.printf("Message: %s\n", Crypto.decrypt(message, N, theta));

    }

    private static void shareDecryptionKey() throws Exception
    {
        BigInteger myTheta = N.add(BigInteger.ONE).subtract(pShareSum).subtract(qShareSum); //my share of theta

        Polynomial thetaSharing = new Polynomial(2, myTheta, bitLength, rand); //polynomial to share my share

        while (!myTurn) { Thread.sleep(5); }

        for (int i = 0; i < numServers; i ++) //send the appropriate share to each server
        {
            while (true)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPortNums[i]);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("sendingTheta");
                    os.writeUTF(Integer.toString(myIndex - 1));
                    os.writeUTF(thetaSharing.getValueAt(i + 1).toString());
                    os.flush();

                    break;
                } catch (Exception e) {}
            }
        }

        passTurn();
        while (!myTurn) { Thread.sleep(5); }

        //adding points to a Biginteger[][] for the lagrange function
        BigInteger tmp[][] = new BigInteger[numServers][2];
        for (int i = 0; i < tmp.length; i++)
        {
            tmp[i][0] = new BigInteger(Integer.toString( i + 1));
            tmp[i][1] = thetaShares[i].multiply(delta); //multiply by delta to prevent decimals
        }

        theta = Crypto.lagrangeGetSecret(tmp).divide(delta);

        passTurn();
    }

    public static void genBiprimalN() throws Exception
    {
        pq = Crypto.genPQ(myIndex, bitLength, rand); //p and q

        //make a polynomial each to share p and q
        Polynomial pSharing = new Polynomial(2, pq[0], bitLength, rand);
        Polynomial qSharing = new Polynomial(2, pq[1], bitLength, rand);

        while (!myTurn) { Thread.sleep(5); }

        for (int i = 0; i < numServers; i ++) //send the right share of p and q to each server
        {
            while (true)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPortNums[i]);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("sendingPQ");
                    os.writeUTF(Integer.toString(myIndex - 1));
                    os.writeUTF(pSharing.getValueAt(i + 1).toString());
                    os.writeUTF(qSharing.getValueAt(i + 1).toString());
                    os.flush();

                    break;
                } catch (Exception e) {}
            }
        }

        passTurn();
        while (!myTurn) { Thread.sleep(5); }

        //add all the shares together to make a share of N Ni
        pShareSum = BigInteger.ZERO;
        qShareSum = BigInteger.ZERO;
        for (int i = 0; i < numServers; i++)
        {
            pShareSum = pShareSum.add(pShares[i]);
            qShareSum = qShareSum.add(qShares[i]);
        }
        BigInteger Ni = pShareSum.multiply(qShareSum);

        for (int portNum : serverPortNums) //send the right share of Ni to each server
        {
            while (true)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("sendingNi");
                    os.writeUTF(Integer.toString(myIndex - 1));
                    os.writeUTF(Ni.toString());
                    os.flush();

                    break;

                } catch (Exception e) { }
            }
        }

        passTurn();
        while (!myTurn) { Thread.sleep(5); }

        //get ready to plug the sum of all Ni's into the secret finder
        BigInteger tmp[][] = new BigInteger[numServers][2];
        for (int i = 0; i < tmp.length; i++)
        {
            tmp[i][0] = new BigInteger(Integer.toString( i + 1));
            tmp[i][1] = nShares[i].multiply(delta);

        }
        N = Crypto.lagrangeGetSecret(tmp).divide(delta); //find N

        if (myIndex == 1) //server index one has to choose a gg
        {
            gg = Crypto.getGG(N, rand);
            for (int portNum : serverPortNums) //then share it with others
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

        for (int portNum : serverPortNums) //Qi is a special number calculated with p and q that allows the N candidate to be checked for biprimality
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

        System.out.printf("N: %s iteration %d\n", N, iterationNum++); //something to watch in the terminal

        passTurn();
        while (!myTurn) { Thread.sleep(5); }

        if (!Crypto.isBiprimal(N, rand, Qi)) //N wasn't biprimal, try again
            genBiprimalN();

    }

    private static void passTurn() //pass turn to next server in series
    {
        myTurn = false;
        while (true)
        {
            try
            {
                socket = new Socket(InetAddress.getLocalHost().getHostAddress(), nextServerPort);
                os = new ObjectOutputStream(socket.getOutputStream());
                is = new ObjectInputStream(socket.getInputStream());
                os.writeUTF("yourTurn");

                os.flush();

                break;
            } catch (Exception e) {}
        }
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

    private static void retreiveKeysFromFile() throws Exception //read the keys from file
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

class ClientComm implements Runnable //one of these listener threads for each client
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

                    for (String name : Server.CandidateNames)
                        os.writeUTF(name);

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

class Listening implements Runnable //listener for other servers to use
{

    public void run()
    {
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
                else if (line.equals("sendingNi"))
                {
                    int index = Integer.parseInt(is.readUTF());
                    Server.nShares[index] = new BigInteger(is.readUTF());
                }
                else if (line.equals("sendingTheta"))
                {
                    int index = Integer.parseInt(is.readUTF());
                    Server.thetaShares[index] = new BigInteger(is.readUTF());
                }

                else if (line.equals("yourTurn"))
                {
                    Server.myTurn = true;
                }

                else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);

                serverSocket.close();
            } catch (Exception e) {
                System.out.printf("Server couldn't start connection.\n"); }
        }
    }
}