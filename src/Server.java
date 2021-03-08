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
    public static String approvedPasswords[];
    public static ArrayList<String> CandidateNamesByOffice = new ArrayList<>(); //list of offices in this format <office>, <candidate1>, <candidate2>, etc
    public static HashMap<String, BigInteger> encryptedSubtotals; //each office has one set of votes, division/remainders are used to determine each candidate's totals

    public static boolean hasN = false; //do we have an N value yet? used for communicating with client
    public static boolean running = true; //running, used to shutdown listening thread
    public static boolean myTurn = false; //ensures synchronization and eliminates race conditions

    public static int bitLength = 16; //bitlength of p and q values
    public static int myListeningPort; //the port this server listens on
    public static int serverPortNums[]; //a list of all servers' ports
    public static int clientPortNums[]; //a list of my clients' ports
    private static int myIndex, numServers, numClients;
    public static int totalVoters; //this is used for encoding more than two candidates for each office (we mod by this^candidateNumber), as well as knowing how many passwords to generate
    public static int currentClientNum = 0; //used for enumerating the clients, so they know which portNum to use
    private static int nextServerPort; //the port of the server at the next index (or first index for last server)
    private static int iterationNum = 0; //keeps track of how many iterations to find a biprimal N
    private static int passwordAuthPort;

    private static Random rand = new Random();

    private static Socket socket;
    private static ObjectOutputStream os;
    private static ObjectInputStream is;

    public static BigInteger N, gg; //public key and coprime
    private static BigInteger theta, delta; //decryption key and delta, which is numServers! (used because interpolation divides by numServers, numservers-1, ..., but needs to be integers
    private static BigInteger pShareSum, qShareSum; //sums of p and q shares, to find the share of N, which can be revealed with lagrange interpolation
    private static BigInteger pq[] = new BigInteger[2]; //p and q for this server
    public static BigInteger pShares[], qShares[], nShares[], thetaShares[], Qi[]; //shares come from each of the Servers, including itself


    public static float candidate_counts[][]; //this is used for displaying the results in the GUI

    public static void main(String args[]) throws Exception
    {
        readCandidatesFromFile(); //read the candidate file and load it into the array
        readKeysFromFile();
        readResultsFromFile();
        ServerGUI myGUI = new ServerGUI(); //set up the GUI

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

        passwordAuthPort = Integer.parseInt(args[numServers + 3 + numClients]);

        totalVoters = Integer.parseInt(args[numServers + 4 + numClients]);

        //setting up things we can now infer
        myListeningPort = serverPortNums[myIndex - 1];
        delta = Crypto.factorial(numServers);
        pShares = new BigInteger[numServers];
        qShares = new BigInteger[numServers];
        nShares = new BigInteger[numServers];
        thetaShares = new BigInteger[numServers];
        approvedPasswords = new String[totalVoters];
        Qi = new BigInteger[numServers];

        if (myIndex == 1)
            myTurn = true;

        if (myIndex == numServers)
            nextServerPort = serverPortNums[0];
        else
            nextServerPort = serverPortNums[myIndex];

        executor.execute(new Listening()); //listening port on its own thread

        if (!readApprovedPasswordsFromFile()) //if file is empty, make new passwords and write them
            generateAndWriteApprovedPasswordsToFile();

        sharePasswordsWithPasswordAuth();

        if (!hasN)
            genBiprimalN(); //try to make an N

        System.out.printf("N: %s\n", N);
        writeKeysToFile();

    }

    private static void sharePasswordsWithPasswordAuth()
    {
        while (true)
        {
            try
            {
                socket = new Socket(InetAddress.getLocalHost().getHostAddress(), passwordAuthPort);
                os = new ObjectOutputStream(socket.getOutputStream());
                is = new ObjectInputStream(socket.getInputStream());

                os.writeUTF(Integer.toString(myIndex));

                for (String password: approvedPasswords)
                {
                    os.writeUTF(password);
                }

                os.flush();

                break;
            } catch (Exception e) {}
        }
    }

    private static boolean readApprovedPasswordsFromFile() throws Exception
    {
        File file = new File("approvedPasswords.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        //every line of the file is an office and its votes
        line = br.readLine();
        if (line == null)
            return false;

        int i = 0;
        do
        {
            approvedPasswords[i++] = line;
            line = br.readLine();
        } while (line != null);

        return true;
    }

    private static void generateAndWriteApprovedPasswordsToFile() throws Exception
    {
        FileWriter file = new FileWriter("approvedPasswords.txt");
        String digits;

        for (String password : approvedPasswords)
        {
            digits = "";
            for (int i = 0; i < 2; i++)
            {
                digits += Integer.toString(rand.nextInt(10));
            }
            password = digits;
            file.write(digits + "\n");
        }

        file.close();
    }

    public static void genBiprimalN() throws Exception
    {

        do
        {
            pq = Crypto.genPQ(myIndex, bitLength, rand); //p and q

            //make a polynomial each to share p and q
            Polynomial pSharing = new Polynomial(2, pq[0], bitLength, rand);
            Polynomial qSharing = new Polynomial(2, pq[1], bitLength, rand);

            while (!myTurn) {
                Thread.sleep(5);
            }

            for (int i = 0; i < numServers; i++) //send the right share of p and q to each server
            {
                while (true) {
                    try {
                        socket = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPortNums[i]);
                        os = new ObjectOutputStream(socket.getOutputStream());
                        is = new ObjectInputStream(socket.getInputStream());
                        os.writeUTF("sendingPQ");
                        os.writeUTF(Integer.toString(myIndex - 1));
                        os.writeUTF(pSharing.getValueAt(i + 1).toString());
                        os.writeUTF(qSharing.getValueAt(i + 1).toString());
                        os.flush();

                        break;
                    } catch (Exception e) {
                    }
                }
            }

            passTurn();
            while (!myTurn) {
                Thread.sleep(5);
            }

            //add all the shares together to make a share of N Ni
            pShareSum = BigInteger.ZERO;
            qShareSum = BigInteger.ZERO;
            for (int i = 0; i < numServers; i++) {
                pShareSum = pShareSum.add(pShares[i]);
                qShareSum = qShareSum.add(qShares[i]);
            }
            BigInteger Ni = pShareSum.multiply(qShareSum);

            for (int portNum : serverPortNums) //send the right share of Ni to each server
            {
                while (true) {
                    try {
                        socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                        os = new ObjectOutputStream(socket.getOutputStream());
                        is = new ObjectInputStream(socket.getInputStream());
                        os.writeUTF("sendingNi");
                        os.writeUTF(Integer.toString(myIndex - 1));
                        os.writeUTF(Ni.toString());
                        os.flush();

                        break;

                    } catch (Exception e) {
                    }
                }
            }

            passTurn();
            while (!myTurn) {
                Thread.sleep(5);
            }

            //get ready to plug the sum of all Ni's into the secret finder
            BigInteger tmp[][] = new BigInteger[numServers][2];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i][0] = new BigInteger(Integer.toString(i + 1));
                tmp[i][1] = nShares[i].multiply(delta);

            }
            N = Crypto.lagrangeGetSecret(tmp).divide(delta); //find N

            if (myIndex == 1) //server index one has to choose a gg
            {
                gg = Crypto.getGG(N, rand);
                for (int portNum : serverPortNums) //then share it with others
                {
                    while (true) {
                        try {
                            socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                            os = new ObjectOutputStream(socket.getOutputStream());
                            is = new ObjectInputStream(socket.getInputStream());
                            os.writeUTF("sendingGG");
                            os.writeUTF(gg.toString());
                            os.flush();
                            break;
                        } catch (Exception e) {
                        }
                    }
                }
            }

            for (int portNum : serverPortNums) //Qi is a special number calculated with p and q that allows the N candidate to be checked for biprimality
            {
                while (true) {
                    try {
                        socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                        os = new ObjectOutputStream(socket.getOutputStream());
                        is = new ObjectInputStream(socket.getInputStream());
                        os.writeUTF("sendingQi");
                        os.writeUTF(Integer.toString(myIndex - 1));
                        os.writeUTF(Crypto.getQi(N, gg, pq, myIndex).toString());
                        os.flush();

                        break;

                    } catch (Exception ignored) {
                    }
                }
            }

            System.out.printf("N: %s iteration %d\n", N, iterationNum++); //something to watch in the terminal

            passTurn();
            while (!myTurn) {
                Thread.sleep(5);
            }


        } while (!Crypto.isBiprimal(N, rand, Qi));

        passTurn();

        hasN = true;

    }

    public static boolean distributeAndCheckPassword(String password)
    {
        boolean rv = true;

        for (int i = 0; i < numServers; i++) //Qi is a special number calculated with p and q that allows the N candidate to be checked for biprimality
        {
            while (true)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPortNums[i]);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("checkPartialPassword");
                    os.writeUTF(password.substring(2*i, 2*i + 2));
                    os.flush();

                    if (is.readUTF().equals("no"))
                    {
                        rv = false;
                    }

                    break;

                } catch (Exception e) {}
            }
            if (!rv)
                break;
        }

        return rv;
    }

    public static void shareDecryptionKey() throws Exception
    {
        BigInteger myTheta = N.add(BigInteger.ONE).subtract(pShareSum).subtract(qShareSum); //my share of theta

        while (!myTurn)
        {
            System.out.printf("Waiting for other servers to share decryption key.\n");
            Thread.sleep(500);
        }

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
                    os.writeUTF(myTheta.toString());
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

        System.out.printf("Theta: %s\n", theta);

        passTurn();
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
            CandidateNamesByOffice.add(line);
            line = br.readLine();
        }

    }


    public static void getResults()
    {
        System.out.printf("These are the results:\n");

        candidate_counts = new float [CandidateNamesByOffice.size()][];
        int row = 0;
        for (String office : CandidateNamesByOffice)
        {
            String names[] = office.split(", ");


            encryptedSubtotals.get(names[0]); //the first after splitting is the office name
            candidate_counts[row] = new float[names.length - 1];

            BigInteger decryptedOffice = Crypto.decrypt(encryptedSubtotals.get(names[0]), N, theta);

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

    private static void readKeysFromFile() throws Exception //read the keys from file
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
        pShareSum = new BigInteger(br.readLine());
        qShareSum = new BigInteger(br.readLine());

        hasN = true;
    }

    private static void writeKeysToFile() throws Exception //save N, p, and q to file
    {
        FileWriter file  = new FileWriter("serverKeys.txt");

        file.write(N.toString() + "\n");
        file.write(pShareSum.toString() + "\n");
        file.write(qShareSum.toString() + "\n");

        file.close();

    }

    //reads existing subtotals from the appropriately-named file in the working directory
    private static void readResultsFromFile() throws Exception
    {
        encryptedSubtotals = new HashMap<>(); //fresh value

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

    }

    //writes to the subtotal file after voting is done
    public static void writeResultsToFile() throws Exception
    {
        FileWriter file = new FileWriter("encryptedSubtotals.txt");

        //for every office
        for (HashMap.Entry<String, BigInteger> entry : encryptedSubtotals.entrySet())
        {
            file.write(entry.getKey() + ": " + entry.getValue() + "\n");
        }

        file.close();
    }

    public static void distributeBallot(String password, BigInteger encryptedVote, String index)
    {
        for (int portNum : serverPortNums) //Qi is a special number calculated with p and q that allows the N candidate to be checked for biprimality
        {
            while (true)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("sendingBallot");
                    os.writeUTF(password);
                    os.writeUTF(encryptedVote.toString());
                    os.writeUTF(index);
                    os.flush();

                    break;

                } catch (Exception e) { }
            }
        }
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

                    for (String name : Server.CandidateNamesByOffice)
                        os.writeUTF(name);

                    os.writeUTF("Total Voters:"); //tells client to expect total voters now
                    os.writeUTF(Integer.toString(Server.totalVoters));
                    os.flush();
                }

                else if (line.equals("haveKey?"))
                {
                    if (!Server.hasN)
                        os.writeUTF("no");
                    else
                    {
                        os.writeUTF("yes");
                    }
                    os.flush();
                }

                else if (line.equals("sendingBallot"))
                {
                    System.out.printf("Server updating from a client\n");
                    String password = is.readUTF();

                    BigInteger encryptedVote = new BigInteger(is.readUTF());

                    String index = is.readUTF();

                    Server.distributeBallot(password, encryptedVote, index);

                }

                else if (line.equals("passwordCorrect?"))
                {
                    if (Server.distributeAndCheckPassword(is.readUTF()))
                        os.writeUTF("yes");
                    else
                        os.writeUTF("no");
                    os.flush();
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
                Socket clientSocket = serverSocket.accept();
                ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

                String line = is.readUTF();

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
                else if (line.equals("sendingBallot"))
                {
                    System.out.printf("Server updating from fellow server\n");
                    //get the subtotal for this particular office
                    BigInteger officeSubTotal;

                    String myPassword = is.readUTF();

                    //if myPassword is correct

                    BigInteger encryptedVote = new BigInteger(is.readUTF());

                    String index = is.readUTF();

                    if (Server.encryptedSubtotals.get(index) == null)
                        officeSubTotal = new BigInteger("0");
                    else
                        officeSubTotal = Server.encryptedSubtotals.get(index);


                    officeSubTotal = Crypto.addEncrypted(officeSubTotal, encryptedVote, Server.N); //add the new vote to it


                    Server.encryptedSubtotals.put(index, officeSubTotal); //update the hashmap

                    Server.writeResultsToFile();
                }
                else if (line.equals("checkPartialPassword"))
                {
                    String givenPassword = is.readUTF();
                    boolean acceptable = false;

                    for (String possiblePassword : Server.approvedPasswords)
                    {
                        if (givenPassword.equals(possiblePassword))
                        {
                            acceptable = true;
                            break;
                        }
                    }
                    if (acceptable)
                        os.writeUTF("yes");
                    else
                        os.writeUTF("no");

                    os.flush();
                }

                else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);

                serverSocket.close();
            } catch (Exception e) {
                System.out.printf("Server couldn't start connection.\n"); }
        }
    }
}