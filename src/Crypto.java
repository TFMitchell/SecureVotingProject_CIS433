/**
 Crypto.java

 Includes static methods pertaining to cryptographic functions.

 Authors: Kevin Kincaid, Thomas Mitchell

 **/

import java.math.*;
import java.util.*;

public class Crypto
{
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


    public static BigInteger decrypt(BigInteger c, BigInteger sk[], BigInteger n)
    {
        //makes the return statement easier to understand to declare here
        BigInteger lambda = sk[0];
        BigInteger u = sk[1];
        BigInteger nSquared = n.multiply(n);

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

        if (one.equals(new BigInteger("0")))
            return two;
        else if (two.equals(new BigInteger("0")))
            return one;
        else
            return one.multiply(two)
                    .mod(nSquared);
    }

}
