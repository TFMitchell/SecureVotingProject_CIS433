/**
 Crypto.java

 Includes static methods pertaining to cryptographic functions.

 Authors: Kevin Kincaid, Thomas Mitchell

 Polynomial multiplication code from: https://www.geeksforgeeks.org/multiply-two-polynomials-2/

 **/

import java.util.*;
import static java.lang.Math.max;

public class Crypto
{
    public static int[] secretKeyGen(int size)
    {
        int sk[] = new int[size + 1]; //return value
        Random rand = new Random(); //init random

        //make a polynomial with random binary coefficients and constant
        for (int i = 0; i < size + 1; i++)  //the + 1 is for the constant term
        {
            sk[i] = rand.nextInt(2);
        }

        return sk; //the secret key is in the form [constant, coefficient of power 1, ..., power size]
    }

    public static int[][] publicKeyGen(int size, int[] secretKey, int zModulus, int polyModulus)
    {
        int pk[][] = new int[2][size + 1]; //rv
        Random rand = new Random(); //init random
        //the first component of the public key is the polynomial on the uniform distribution of the modulus
        for (int i = 0; i < size + 1; i++)  //the + 1 is for the constant term
        {
            pk[0][i] = rand.nextInt(zModulus);
        }

        //the second component of the public key is obtained using the following formula:
        //step 1: obtain a polynomial e with a normal coefficient distribution with mean 0 and std div 2 discretized
        //step 2: multiply the negative of each coefficient first component of the public key (which we found earlier) by the secret key
        //step 3: subtract from that product the normal distribution polynomial e

        int[] e = new int[size + 1];

        //step 1:
        for (int i = 0; i < size + 1; i++)  //the + 1 is for the constant term
        {
            e[i] = (int) rand.nextGaussian() * 2;
        }

        //step 2:
        //for

        return null;
    }

    //taken from GeeksForGeeks
    static int[] multiply(int A[], int B[],
                          int m, int n)
    {
        int[] prod = new int[m + n - 1];

        // Initialize the product polynomial
        for (int i = 0; i < m + n - 1; i++)
        {
            prod[i] = 0;
        }

        // Multiply two polynomials term by term
        // Take ever term of first polynomial
        for (int i = 0; i < m; i++)
        {
            // Multiply the current term of first polynomial
            // with every term of second polynomial.
            for (int j = 0; j < n; j++)
            {
                prod[i + j] += A[i] * B[j];
            }
        }

        return prod;
    }

    static int[] add(int A[], int B[], int m, int n)
    {
        int size = max(m, n);
        int sum[] = new int[size];

        // Initialize the product polynomial
        for (int i = 0; i < m; i++)
        {
            sum[i] = A[i];
        }

        // Take every term of first polynomial
        for (int i = 0; i < n; i++)
        {
            sum[i] += B[i];
        }

        return sum;
    }
}
