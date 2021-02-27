/**
 Crypto.java

 Includes static methods pertaining to cryptographic functions.

 Authors: Kevin Kincaid, Thomas Mitchell

 **/

import java.math.*;
import java.util.*;

public class Crypto
{
    private static final BigInteger THREE = new BigInteger("3");
    private static final BigInteger FOUR = new BigInteger("4");

    public static BigInteger[] genPQ(int myIndex, int rsaPrimesBitlength, Random rand)
    {
        BigInteger rv[] = new BigInteger[2];

        if(myIndex == 1)
        {
            do
            {
                rv[0] = new BigInteger(rsaPrimesBitlength, rand);
                rv[1] = new BigInteger(rsaPrimesBitlength, rand);

            } while (!rv[0].mod(FOUR).equals(THREE) || ! rv[1].mod(FOUR).equals(THREE));

        }
        else
        {
            do
            {
                rv[0] = new BigInteger(rsaPrimesBitlength, rand);
                rv[1] = new BigInteger(rsaPrimesBitlength, rand);

            } while (!rv[0].mod(FOUR).equals(BigInteger.ZERO) || ! rv[1].mod(FOUR).equals(BigInteger.ZERO));
        }

        return rv;
    }


    public static BigInteger encrypt(BigInteger m, BigInteger r, BigInteger N)
    {
        //makes the return statement easier to understand to declare here

        BigInteger g = N.add(BigInteger.ONE);
        BigInteger nSquared = N.multiply(N);

        return g.modPow(m, nSquared)
                .multiply(r.modPow(N, nSquared))
                .mod(nSquared);
    }


    public static BigInteger decrypt(BigInteger c, BigInteger N, BigInteger theta)
    {
        //makes the return statement easier to understand to declare here

        BigInteger nSquared = N.multiply(N);
        BigInteger u = N.add(BigInteger.ONE).modPow(theta, nSquared)
                .subtract(BigInteger.ONE)
                .divide(N)
                .modInverse(N);

        return c.modPow(theta, nSquared)
                .subtract(BigInteger.ONE)
                .divide(N)
                .multiply(u)
                .mod(N);
    }

    //this adds two encrypted ciphertexts together
    public static BigInteger addEncrypted(BigInteger one, BigInteger two, BigInteger N)
    {
        BigInteger nSquared = N.multiply(N); //for simpler return statement

        if (one.equals(BigInteger.ZERO))
            return two;
        else if (two.equals(BigInteger.ZERO))
            return one;
        else
            return one.multiply(two)
                    .mod(nSquared);
    }

}
