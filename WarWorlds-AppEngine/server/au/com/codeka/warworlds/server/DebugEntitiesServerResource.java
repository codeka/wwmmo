package au.com.codeka.warworlds.server;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import au.com.codeka.warworlds.shared.DebugEntitiesResource;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.tools.mapreduce.DatastoreInputFormat;

public class DebugEntitiesServerResource extends ServerResource
        implements DebugEntitiesResource{
    private String mEntityName;

    @Override
    public void doInit() throws ResourceException {
        ConcurrentMap<String, Object> attrs = getRequest().getAttributes();

        if (attrs.containsKey("entityName")) {
            mEntityName = (String) attrs.get("entityName");
        }
    }

    @Override
    public ArrayList<String> listEntities() {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        Query q = new Query(Query.KIND_METADATA_KIND);

        ArrayList<String> entityNames = new ArrayList<String>();
        for(Entity e : ds.prepare(q).asIterable()) {
            entityNames.add(e.getKey().getName());
        }

        return entityNames;
    }

    /**
     * Delete all entities of a given kind using the Mapper API.
     */
    @Override
    public void deleteAllEntities() {
        if (mEntityName == null || mEntityName.isEmpty()) {
            // error?
            return;
        }

        TaskOptions task = TaskOptions.Builder.withUrl("/mapreduce/command/start_job")
            .method(Method.POST)
            .header("X-Requested-With", "XMLHttpRequest")
            .param("name", "delete-all-entities");
        task.param("mapper_params."+DatastoreInputFormat.ENTITY_KIND_KEY, mEntityName);

        Queue q = QueueFactory.getDefaultQueue();
        q.add(task);
    }
}
