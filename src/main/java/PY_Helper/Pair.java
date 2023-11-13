package PY_Helper;

/**
 * Class implementing a pair of two object instances with different generic types.
 *
 * @author P. Le Bras
 * @version 1
 */
public class Pair<L,R> {

    /** Left object. */
    private L left;
    /** Right object. */
    private R right;

    /**
     * Constructor.
     * @param left Instance of left object.
     * @param right Instance of right object.
     */
    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Getter for the left object.
     * @return The left object.
     */
    public L getLeft() { return left; }

    /**
     * Getter for the right object.
     * @return The right object.
     */
    public R getRight() { return right; }

    /**
     * Setter for the left object.
     * @param left New value for the left object.
     */
    public void setLeft(L left) { this.left = left; }

    /**
     * Setter for the right object.
     * @param right New value for the right object.
     */
    public void setRight(R right) { this.right = right; }

    /**
     * Method returning a hash code for the pair.
     * @return The pair's hash code.
     */
    @Override
    public int hashCode() { return left.hashCode() ^ right.hashCode(); }

    /**
     * Method to test equality with another pair.
     * @param o Pair to compare with.
     * @return Equality boolean.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) return false;
        Pair pairo = (Pair) o;
        return this.left.equals(pairo.getLeft()) &&
                this.right.equals(pairo.getRight());
    }

}
