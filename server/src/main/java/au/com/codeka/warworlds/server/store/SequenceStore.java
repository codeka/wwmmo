package au.com.codeka.warworlds.server.store;

/**
 * A store for storing a sequence of unique IDs.
 */
public class SequenceStore extends BaseStore {
  SequenceStore(String fileName) {
    super(fileName);
  }

  /** Returns the next identifier in the sequence. */
  public long nextIdentifier() {
    return 0;
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    return 1;
  }
}
