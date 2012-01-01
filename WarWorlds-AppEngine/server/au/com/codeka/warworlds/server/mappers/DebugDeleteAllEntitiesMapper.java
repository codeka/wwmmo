package au.com.codeka.warworlds.server.mappers;

import java.util.logging.Logger;

import org.apache.hadoop.io.NullWritable;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.mapreduce.AppEngineMapper;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;

/**
 * A mapper which deletes all entities of a given kind. Be careful!
 */
public class DebugDeleteAllEntitiesMapper extends
        AppEngineMapper<Key, Entity, NullWritable, NullWritable> {
    private static final Logger log = Logger.getLogger(DebugDeleteAllEntitiesMapper.class.getName());

    @Override
    public void map(Key key, Entity value, Context context) {
        log.info("Adding key to deletion pool: "+key);
        DatastoreMutationPool mutationPool = this.getAppEngineContext(context).getMutationPool();
        mutationPool.delete(value.getKey());
    }
}
