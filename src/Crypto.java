/**
 Crypto.java

 Includes static methods pertaining to cryptographic functions.

 Authors: Kevin Kincaid, Thomas Mitchell

 References: java2s.com, geeksforgeeks.com

 **/

import java.math.*;
import java.util.*;

public class Crypto
{
    //commonly used BigInts
    private static final BigInteger THREE = new BigInteger("3");
    private static final BigInteger FOUR = new BigInteger("4");

    //generate random p and q values
    public static BigInteger[] genPQ(int myIndex, int rsaPrimesBitlength, Random rand)
    {
        BigInteger rv[] = new BigInteger[2];

        if(myIndex == 1) //the p and q have special parameters for the party with the first index
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

    //tests to see if the n candidate is biprimal (can be divided only by two prime numbers) using the Q shares
    public static boolean isBiprimal(BigInteger n, Random rand, BigInteger Qi[])
    {
        BigInteger Q1 = Qi[0]; //making note of the share of Q from server 1

        //get the product of all other Q shares' inverses
        BigInteger ProductOfQiInverses = BigInteger.ONE;
        for (int i = 1; i < Qi.length; i++)
            ProductOfQiInverses = ProductOfQiInverses.multiply (Qi[i].modInverse(n));

        //if Q1 times the product of other shares' inverses mod N is equal to 1
        return Q1.multiply(ProductOfQiInverses).mod(n).equals(BigInteger.ONE);
    }

    //generate a number that is coprime to N, called gg
    public static BigInteger getGG(BigInteger n, Random rand)
    {
        BigInteger gg;

        do
        {
            gg = new BigInteger(n.bitCount(), rand);
        } while (Jacobi(gg, n) != 1); //using faster method for determining if gg and n are coprime

        return gg;
    }

    //helper function for determining Jacobi symbol of two BigInts. From java2s.com
    private static int Jacobi(BigInteger m, BigInteger n)
    {
        BigInteger TWO = new BigInteger("2");
        BigInteger FOUR = TWO.add(TWO);
        BigInteger SEVEN = FOUR.add(new BigInteger("3"));
        BigInteger EIGHT = FOUR.add(FOUR);

        if (m.compareTo(n) >= 0) {
            m = m.mod(n);
            return Jacobi(m, n);
        }
        if (n.equals(BigInteger.ONE) || m.equals(BigInteger.ONE)) {
            return 1;
        }
        if (m.equals(BigInteger.ZERO)) {
            return 0;
        }
        int twoCount = 0;
        while (m.mod(TWO).equals( BigInteger.ZERO)) {
            twoCount++;
            m = m.divide(TWO);
        }
        int J2n = n.mod(EIGHT).equals(BigInteger.ONE)
                || n.mod(EIGHT).equals(SEVEN) ? 1 : -1;
        int rule8multiplier = (twoCount % 2 == 0) ? 1 : J2n;
        int tmp = Jacobi(n, m);
        int rule6multiplier = n.mod(FOUR).equals(BigInteger.ONE)
                || m.mod(FOUR).equals(BigInteger.ONE) ? 1 : -1;
        return tmp * rule6multiplier * rule8multiplier;
    }

    //Qi encodes p and q so that is can be used to find n's biprimality without needing to reveal
    public static BigInteger getQi(BigInteger n, BigInteger gg, BigInteger pq[], int index)
    {
        if (index == 1)
            return gg.modPow(n.add(BigInteger.ONE).subtract(pq[0]).subtract(pq[1]).divide(FOUR), n);
        else
            return gg.modPow(pq[0].add(pq[1]).divide(FOUR), n);
    }

    //encrypt a message
    public static BigInteger encrypt(BigInteger m, BigInteger r, BigInteger n)
    {
        BigInteger g = n.add(BigInteger.ONE);
        BigInteger nSquared = n.multiply(n);

        return g.modPow(m, nSquared)
                .multiply(r.modPow(n, nSquared))
                .mod(nSquared);
    }

    //decrypt a message
    public static BigInteger decrypt(BigInteger c, BigInteger n, BigInteger lambda)
    {
        BigInteger nSquared = n.multiply(n);
        BigInteger u = n.add(BigInteger.ONE).modPow(lambda, nSquared)
                .subtract(BigInteger.ONE)
                .divide(n)
                .modInverse(n);

        return c.modPow(lambda, nSquared)
                .subtract(BigInteger.ONE)
                .divide(n)
                .multiply(u)
                .mod(n);
    }

    //this adds two encrypted ciphertexts together
    public static BigInteger addEncrypted(BigInteger one, BigInteger two, BigInteger n)
    {
        BigInteger nSquared = n.multiply(n); //for simpler return statement

        if (one.equals(BigInteger.ZERO))
            return two;
        else if (two.equals(BigInteger.ZERO))
            return one;
        else
            return one.multiply(two)
                    .mod(nSquared);
    }

    //given a set of points x, y, returns the y intercept. Used to find secret in Shamir's Secret Sharing. Instpired by geeksforgeeks.com
    public static BigInteger lagrangeGetSecret(BigInteger points[][])
    {
        BigInteger result = BigInteger.ZERO;

        for (int i = 0; i < points.length; i++)
        {
            BigInteger term = points[i][1];
            for (int j = 0; j < points.length; j++)
            {
                if (j == i) //would be division by zero
                    continue;

                term = term.multiply(BigInteger.ZERO.subtract(points[j][0]))
                        .divide(points[i][0].subtract(points[j][0]));
            }

            result = result.add(term);
        }

        return result;
    }

    //make a Biginteger factorial
    public static BigInteger factorial(int n)
    {
        BigInteger rv = BigInteger.ONE;

        for (int i = 2; i <= n; i++)
            rv = rv.multiply(BigInteger.valueOf(i));

        return rv;
    }
}