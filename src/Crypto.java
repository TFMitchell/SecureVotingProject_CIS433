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

    public static BigInteger[] genPQ(int myIndex, int rsaPrimesBitlength, Random rand) //generate random P and Q values
    {
        BigInteger rv[] = new BigInteger[2];

        if(myIndex == 1) //the P and Q have special parameters for the party with the first index
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

    public static boolean isBiprimal(BigInteger N, Random rand, BigInteger Qi[]) //tests to see if the N is biprimal (can be divided only by two prime numbers)
    {
        BigInteger Q1 = Qi[0];
        BigInteger ProductOfQiInverses = BigInteger.ONE;

        for (int i = 1; i < Qi.length; i++)
        {
            ProductOfQiInverses = ProductOfQiInverses.multiply (Qi[i].modInverse(N));
        }

        return Q1.multiply(ProductOfQiInverses).mod(N).equals(BigInteger.ONE.mod(N));
    }

    public static BigInteger getGG(BigInteger N, Random rand) //generate a number the is coprime to N, called GG
    {
        BigInteger gg;

        do {
            gg = new BigInteger(N.bitCount(), rand);
        } while (!gg.gcd(N).equals(BigInteger.ONE)
                || Jacobi(gg, N) != 1);

        return gg;
    }

    private static int Jacobi(BigInteger m, BigInteger n) //helper function for get GG. From java2s.com
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

    public static BigInteger getQi(BigInteger Ncandidate, BigInteger gg, BigInteger pq[], int index) //Qi encodes p and q so that is can be used to find N's biprimality without revealing P and Q
    {
        if (index == 1)
            return gg.modPow(Ncandidate.add(BigInteger.ONE).subtract(pq[0]).subtract(pq[1]).divide(FOUR), Ncandidate);
        else
            return gg.modPow(pq[0].add(pq[1]).divide(FOUR), Ncandidate);
    }

    public static BigInteger encrypt(BigInteger m, BigInteger r, BigInteger N) //encrypt a message
    {
        BigInteger g = N.add(BigInteger.ONE);
        BigInteger nSquared = N.multiply(N);

        return g.modPow(m, nSquared)
                .multiply(r.modPow(N, nSquared))
                .mod(nSquared);
    }

    public static BigInteger decrypt(BigInteger c, BigInteger N, BigInteger theta) //decrypt a message
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

    public static BigInteger lagrangeGetSecret(BigInteger points[][])  //given a set of points x, y, returns the y intercept. Used to find secret in Shamir's Secret Sharing. Instpired by geeksforgeeks.com
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

    public static BigInteger factorial(int N) //make a Biginteger factorial
    {
        BigInteger rv = BigInteger.ONE;

        for (int i = 2; i <= N; i++)
            rv = rv.multiply(BigInteger.valueOf(i));

        return rv;
    }
}
