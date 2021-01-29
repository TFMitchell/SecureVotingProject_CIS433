/**
 Main.java
 Contains the main function/entry point for the program.

 Authors: Kevin Kincaid, Thomas Mitchell
 **/



public class Main
{
    public static void main(String args[])
    {
        GUI myGUI = new GUI(); //set up the GUI

        int size = 9; //size of keys
        int sk[] = Crypto.secretKeyGen(size); //make the secret key

        //print the secret key
        for (int i = 0; i < size + 1 ; i++)
        {
            System.out.printf("%d\n", sk[i]);
        }

        //make public key
        int zMod = 25; //setting the zMod. I think it needs to be a power of a prime number.
        int polyMod = 4; //x^polyMod will be used to divide the polynomials
        int pk[][] = Crypto.publicKeyGen(size, sk, zMod, polyMod);

        for (int i = 0; i < 2; i++)
        {
            for (int c = 0; c < size + 1; c++)
            {
                //System.out.printf("%d\n", pk[i][c]);
            }
        }

    }

}