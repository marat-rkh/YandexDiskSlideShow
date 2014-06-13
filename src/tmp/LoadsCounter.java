package tmp;

public class LoadsCounter {
    private int counter = 0;
    private int maxLoadsNumber;

    public void increment() {
        ++counter;
    }

    public boolean allLoadsHappened() {
        return counter == maxLoadsNumber;
    }

    public void setMaxLoadsNumber(int maxLoadsNumber) {
        this.maxLoadsNumber = maxLoadsNumber;
    }

    public int getValue() { return counter; }
}
