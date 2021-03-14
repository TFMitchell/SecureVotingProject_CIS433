import java.math.BigInteger;
import java.util.Random;

public class Polynomial
{
    private final BigInteger a[]; //coefficients with a[0] being the constant, or intercept and a[i] being the coefficient of the term degree i

    //make a polynomial with random coefficients for Shamir Secret Sharing
    public Polynomial(int degree, BigInteger intercept, int bitLength, Random rand)
    {
        a = new BigInteger[degree + 1];
        a[0] = intercept;
        for (int i = 1; i < degree + 1; i++) {
            a[i] = new BigInteger(bitLength, rand);
        }
    }

    //get the y value of the polynomial at this x value. Used for generating shares to give to other servers
    public BigInteger getValueAt(int x)
    {
        BigInteger rv = a[0];

        for (int i = 1; i < a.length; i++)
        {
            rv = rv.add(a[i].multiply(new BigInteger(Integer.toString((int) Math.pow(x, i)))));
        }

        return rv;
    }
}