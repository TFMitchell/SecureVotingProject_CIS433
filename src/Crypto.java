/**
 Crypto.java

 Includes static methods pertaining to cryptographic functions.

 Authors: Kevin Kincaid, Thomas Mitchell

 **/

import java.math.*;
import java.util.*;

public class Crypto
{

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
