package experiments;

public class IndexPair {

    public final int index1, index2;

    public IndexPair(int index1, int index2) {
        this.index1 = index1;
        this.index2 = index2;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IndexPair && ((((IndexPair) o).index1 == this.index1 && ((IndexPair) o).index2 == this.index2) || (((IndexPair) o).index1 == this.index2 && ((IndexPair) o).index2 == this.index1));
    }

}