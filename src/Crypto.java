/**
 Crypto.java

 Includes static methods pertaining to cryptographic functions.

 Authors: Kevin Kincaid, Thomas Mitchell

 **/

import java.math.*;
import java.util.*;

public class Crypto
{
    /**
    public static BigInteger[][] keyPairGen(int bitLength, int certainty) //outputs [[n, g], [lambda, u]]
    {
        BigInteger rv[][] = new BigInteger[2][2]; //return value
        BigInteger n, g, lambda, u; //declaring the four variables

        //making random p and q
        BigInteger p = new BigInteger(bitLength / 2, certainty, new Random());
        BigInteger q = new BigInteger(bitLength / 2, certainty, new Random());

        n = p.multiply(q); //n is their sum
        BigInteger nSquared = n.multiply(n); //nSquared is commonly used, so it gets its own variable

        g = new BigInteger("2"); //sample g value is 2

        //setting up return values for the public key
        rv[0][0] = n;
        rv[0][1] = g;

        //finding lambda
        lambda = p.subtract(BigInteger.ONE)
                .multiply(q.subtract(BigInteger.ONE))
                .divide(p.subtract(BigInteger.ONE)
                .gcd(q.subtract(BigInteger.ONE)));

        //finding u
        u = g.modPow(lambda, nSquared)
                .subtract(BigInteger.ONE)
                .divide(n)
                .modInverse(n);

        //check to make sure the g we picked was useful
        if (u.gcd(n).intValue() != 1)
        {
            System.out.printf("Bad g. Try another.\n");
            System.exit(-1);
        }

        //return the secret portion of the key
        rv[1][0] = lambda;
        rv[1][1] = u;

        return rv;
    }**/

    public static BigInteger[] getPK(BigInteger pq[])
    {
        BigInteger rv[] = new BigInteger[2];
        BigInteger N = pq[0].multiply(pq[1]);

        if (!N.gcd(pq[0].subtract(BigInteger.ONE).multiply(pq[1].subtract(BigInteger.ONE))).equals(BigInteger.ONE) //if gcd not == 1

                || N.mod(new BigInteger("4")).equals(new BigInteger("3"))) //biprimality check

        //|| factorialCalc(pq[0].subtract(BigInteger.ONE)).add(BigInteger.ONE).mod(pq[0]).signum() != 0    //if p isn't prime
        //|| factorialCalc(pq[1].subtract(BigInteger.ONE)).add(BigInteger.ONE).mod(pq[1]).signum() != 0)   //if q isn't prime
        {
            rv[0] = BigInteger.ZERO;
            rv[1] = BigInteger.ZERO;
            return rv;
        }
        //else
        rv[0] = N;
        rv[1] = N.subtract(BigInteger.ONE);

        return rv;

    }

    public static BigInteger[] getSK(BigInteger pk[], BigInteger pq[])
    {
        //finding lambda (this is (p - 1) * (q - 1) / gcd of (p - 1) * (q - 1), which is the lcm)
        BigInteger lambda = pq[0].subtract(BigInteger.ONE)
                .multiply(pq[1].subtract(BigInteger.ONE))
                .divide(pq[0].subtract(BigInteger.ONE)
                        .gcd(pq[1].subtract(BigInteger.ONE)));

        //finding u: g^lambda mod n^2 - 1 all over N and mod inverse N
        BigInteger u = pk[1].modPow(lambda, pk[0].multiply(pk[0]))
                .subtract(BigInteger.ONE)
                .divide(pk[0])
                .modInverse(pk[0]);


        BigInteger rv[] = new BigInteger[2];
        rv[0] = lambda;
        rv[1] = u;


        return rv;
    }

    public static BigInteger encrypt(BigInteger m, BigInteger r, BigInteger pk[])
    {
        //makes the return statement easier to understand to declare here
        BigInteger n = pk[0];
        BigInteger g = pk[1];
        BigInteger nSquared = n.multiply(n);

        return g.modPow(m, nSquared)
                .multiply(r.modPow(n, nSquared))
                .mod(nSquared);
    }


    public static BigInteger decrypt(BigInteger c, BigInteger pk[], BigInteger sk[])
    {
        //makes the return statement easier to understand to declare here
        BigInteger lambda = sk[0];
        BigInteger N = pk[0];
        BigInteger u = sk[1];
        BigInteger nSquared = N.multiply(N);

        return c.modPow(lambda, nSquared)
                .subtract(BigInteger.ONE)
                .divide(N)
                .multiply(u)
                .mod(N);
    }

    //this adds two encrypted ciphertexts together
    public static BigInteger addEncrypted(BigInteger one, BigInteger two, BigInteger pk[])
    {
        BigInteger nSquared = pk[0].multiply(pk[0]); //for simpler return statement

        if (one.equals(new BigInteger("0")))
            return two;
        else if (two.equals(new BigInteger("0")))
            return one;
        else
            return one.multiply(two)
                    .mod(nSquared);
    }

}
