package au.com.codeka.warworlds.common;

public class ObjectPool<T extends ObjectPool.Pooled> {
    private PooledCreator creator;
    private Pooled[] objects;
    private int freeIndex;

    public ObjectPool(int poolSize, PooledCreator creator) {
        this.creator = creator;
        objects = new Pooled[poolSize];
        freeIndex = -1;
    }

    @SuppressWarnings("unchecked")
    synchronized public T borrow() {
        if (freeIndex < 0) {
            return (T) creator.create();
        }

        T obj = (T) objects[freeIndex--];
        obj.reset();
        return obj;
    }

    synchronized public void release(Pooled obj) {
        if (obj == null)
            return;

        if (freeIndex >= objects.length - 1) {
            return;
        }

        objects[++freeIndex] = obj;
    }

    /**
     * Pooled objects need to implement this interface.
     */
    public interface Pooled {
        void reset();
    }

    public interface PooledCreator {
        Pooled create();
    }
}
