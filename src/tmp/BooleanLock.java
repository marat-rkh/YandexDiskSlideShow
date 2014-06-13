package tmp;

public class BooleanLock {
    private boolean isLocked;

    public BooleanLock(boolean value) {
        isLocked = value;
    }

    synchronized public void set(boolean value) {
        isLocked = value;
    }

    synchronized public boolean isTrue() {
        return isLocked;
    }
}
