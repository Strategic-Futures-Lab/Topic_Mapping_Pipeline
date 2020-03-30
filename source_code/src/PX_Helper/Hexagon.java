package PX_Helper;

/**
 * Created by Tom on 16/05/2018.
 */
public class Hexagon
{
    public int x;
    public int y;
    public int z;
    public int topic;
    public int cluster;

    public Hexagon(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.topic = -1;
        this.cluster = -1;
    }

    public Hexagon(int x, int y, int z, int topic, int cluster)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.topic = topic;
        this.cluster = cluster;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Hexagon hexagon = (Hexagon) o;

        return x == hexagon.x && y == hexagon.y && z == hexagon.z;
    }

    @Override
    public int hashCode()
    {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }
}
