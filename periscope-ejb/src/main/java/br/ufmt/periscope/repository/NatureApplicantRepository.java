package br.ufmt.periscope.repository;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.util.Filters;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.bson.Document;

@Named
public class NatureApplicantRepository {

    private @Inject
    Datastore ds;

    public List<Pair> getNatureApplicantRepository(Project currentProject, Filters filtro) {

        //db.Patent.aggregate({$match:{blacklisted:false}},
        //{$unwind:"$applicants"},{$group:{_id:"$applicants.nature.name", count:{$sum:1}}})
        MongoCollection<Document> coll = ds.getDatabase()
                .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());

        List<Document> pipeline = new ArrayList<Document>();
        pipeline.add(new Document("$match", new Document("project.$id", currentProject.getId())));

        if (filtro.isComplete()) {
            pipeline.add(new Document("$match", new Document("completed", filtro.isComplete())));
        }

        pipeline.add(new Document("$match", new Document("blacklisted", false)));
        pipeline.add(new Document("$unwind", "$applicants"));

        //db.Patent.aggregate({$match:{blacklisted:false}},
        //{$unwind:"$applicants"},{$group:{_id:{name:"$applicants.name",nature:"$applicants.nature.name"}}},
        //{$group:{_id:"$_id.nature",count:{$sum:1}}},{$sort:{count:-1}})
        Document nature = new Document("nature", "$applicants.nature.name")
                .append("name", "$applicants.name");
        pipeline.add(new Document("$group", new Document("_id", nature)));

        pipeline.add(new Document("$group", new Document("_id", "$_id.nature")
                .append("count", new Document("$sum", 1))));

        pipeline.add(new Document("$sort", new Document("count", -1)));

        List<Document> outputResult = coll.aggregate(pipeline).into(new ArrayList<Document>());

        List<Pair> pairs = new ArrayList<Pair>();
        for (Document aux : outputResult) {
            if (aux.get("_id") != null) {
                String type = aux.get("_id").toString();
                Number count = (Number) aux.get("count");
                pairs.add(new Pair(type, count));
            }
        }

        return pairs;
    }
}
