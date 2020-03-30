package PX_Helper;

/**
 * Created by Tom on 17/05/2018.
 */
public class EuclidHexagon
{
    public double x, y;
    public int topic, cluster;

    public EuclidHexagon(double x, double y, int topic, int cluster)
    {
        this.x = x;
        this.y = y;
        this.topic = topic;
        this.cluster = cluster;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EuclidHexagon that = (EuclidHexagon) o;

        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && topic == that.topic;
    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + topic;
        return result;
    }
}
