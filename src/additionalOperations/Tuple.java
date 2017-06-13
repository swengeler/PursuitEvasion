package additionalOperations;

import javafx.util.Pair;

public class Tuple<A, B> extends Pair<A, B> {

    public Tuple(A first, B second) {
        super(first, second);
    }

    public A getFirst() {
        return super.getKey();
    }

    public B getSecond() {
        return super.getValue();
    }

}
