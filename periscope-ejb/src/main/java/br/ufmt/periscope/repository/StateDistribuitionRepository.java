package br.ufmt.periscope.repository;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.report.Pair;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.bson.Document;

@Named
public class StateDistribuitionRepository {

    private @Inject
    Datastore ds;

    public List<Pair> getStateDistribuitions(Project currentProject) {

//        db.Patent.aggregate({$unwind:"$applicants"},
//        {$project:{applicants:1}},
//        {$match:{"applicants.country.acronym":"BR"}}, {$project:{"applicants.state":1}}
//        ,{$group:{_id:{_id:"$_id",state:"$applicants.state"}}},{$group:{_id:"$_id.state.acronym",count:{$sum:1}}},{$sort : {count: -1}})
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());

        List<Document> pipeline = new ArrayList<Document>();
        pipeline.add(new Document("$match", new Document("project.$id", currentProject.getId())));
        pipeline.add(new Document("$unwind", "$applicants"));
        pipeline.add(new Document("$project", new Document("applicants", 1)));
        pipeline.add(new Document("$match", new Document("applicants.country.acronym", "BR")));
        pipeline.add(new Document("$project", new Document("applicants.state", 1)));

        Document id = new Document("_id", "$_id").append("state", "$applicants.state");
        pipeline.add(new Document("$group", new Document("_id", id)));

        pipeline.add(new Document("$group", new Document("_id", "$_id.state.acronym")
                .append("count", new Document("$sum", 1))));

        pipeline.add(new Document("$sort", new Document("count", -1)));

        List<Document> outputResult = coll.aggregate(pipeline).into(new ArrayList<Document>());

        List<Pair> pairs = new ArrayList<Pair>();
        for (Document aux : outputResult) {
            if (aux.get("_id") != null && aux.get("_id") != "") {
                String state = aux.get("_id").toString();
                Number count = (Number) aux.get("count");
                pairs.add(new Pair(state, count));
            }
        }
        return pairs;

    }
}
