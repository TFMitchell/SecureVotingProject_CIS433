import java.math.BigInteger;
import java.util.Random;

public class Polynomial
{
    private final BigInteger a[];

    public Polynomial(int degree, BigInteger intercept, int bitLength, Random rand)
    {
        a = new BigInteger[degree + 1];
        a[0] = intercept;
        for (int i = 1; i < degree + 1; i++) {
            a[i] = new BigInteger(bitLength, rand);
        }
    }

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