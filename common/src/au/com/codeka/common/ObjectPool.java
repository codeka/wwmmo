package au.com.codeka.common;

public class ObjectPool<T extends ObjectPool.Pooled> {
    private PooledCreator mCreator;
    private Pooled[] mObjects;
    private int mFreeIndex;

    public ObjectPool(int poolSize, PooledCreator creator) {
        mCreator = creator;
        mObjects = new Pooled[poolSize];
        mFreeIndex = -1;
    }

    @SuppressWarnings("unchecked")
    synchronized public T borrow() {
        if (mFreeIndex < 0) {
            return (T) mCreator.create();
        }

        T obj = (T) mObjects[mFreeIndex--];
        obj.reset();
        return obj;
    }

    synchronized public void release(Pooled obj) {
        if (obj == null)
            return;

        if (mFreeIndex >= mObjects.length - 1) {
            return;
        }

        mObjects[++mFreeIndex] = obj;
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
