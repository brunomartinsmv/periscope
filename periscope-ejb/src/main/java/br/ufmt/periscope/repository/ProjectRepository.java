package br.ufmt.periscope.repository;

import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.query.FindOptions;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.or;
import java.util.ArrayList;
import java.util.List;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.bson.Document;
import org.bson.types.ObjectId;

@ApplicationScoped
@Named
public class ProjectRepository {

    @Inject
    private Datastore ds;
    @Inject
    private PatentIndexer patentIndexer;
    @Inject
    private Instance<GridFSBucket> fsProvider;

    public List<Project> getProjectList(User user) {
        FindOptions options = new FindOptions()
                .sort(dev.morphia.query.Sort.ascending("title"));
        if (user.getUserLevel().getAccessLevel() != 10) {
            return ds.find(Project.class)
                    .filter(or(
                            eq("owner", user),
                            eq("observers", user),
                            eq("isPublic", true)))
                    .iterator(options)
                    .toList();
        }
        return ds.find(Project.class).iterator(options).toList();
    }

    public List<String> getProjectFiles(Project project) {
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());

        List<String> lista = new ArrayList<String>();
        for (Document novo : coll.find(Filters.eq("project.$id", project.getId()))
                .projection(Projections.include("presentationFile", "patentInfo"))) {
            Object preFile = novo.get("presentationFile");
            Object pInfo = novo.get("patentInfo");
            addDbRefId(lista, preFile);
            addDbRefId(lista, pInfo);
        }
        return lista.isEmpty() ? null : lista;
    }

    private void addDbRefId(List<String> lista, Object ref) {
        if (ref == null) {
            return;
        }
        if (ref instanceof Document) {
            Object id = ((Document) ref).get("$id");
            if (id != null) {
                lista.add(id.toString());
            }
        } else if (ref instanceof org.bson.types.ObjectId) {
            lista.add(ref.toString());
        }
    }

    public void deleteProject(String id) {
        Project p = new Project();
        p.setId(new ObjectId(id));
        patentIndexer.deleteIndexesForProject(p);
        List<String> files = getProjectFiles(p);

        if (files != null) {
            GridFSBucket fs = fsProvider.get();
            for (String file : files) {
                fs.delete(new ObjectId(file));
            }
        }

        deleteProject(p);
    }

    public boolean isEmptyPatent(Project currentProject) {
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Project.class).getCollectionName());
        Document doc = coll.find(Filters.eq("_id", currentProject.getId()))
                .projection(Projections.fields(Projections.slice("patents", 1)))
                .first();
        if (doc == null) {
            return true;
        }
        Object patents = doc.get("patents");
        return patents == null || (patents instanceof List && ((List<?>) patents).isEmpty());
    }

    public void deleteProject(Project project) {
        DeleteOptions multi = new DeleteOptions().multi(true);
        ds.find(Patent.class).filter(eq("project", project)).delete(multi);
        ds.find(Rule.class).filter(eq("project", project)).delete(multi);
        ds.delete(project);
    }
}
