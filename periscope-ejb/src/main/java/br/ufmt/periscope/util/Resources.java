package br.ufmt.periscope.util;

import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Singleton;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import br.ufmt.periscope.indexer.resources.analysis.CommonDescriptor;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Files;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.User;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;

/**
 * CDI producers for MongoDB / Morphia / GridFS (Morphia 2 + driver sync 5).
 */
@ApplicationScoped
public class Resources {

    public static final String DEFAULT_URI = "mongodb://localhost:27017";
    public static final String DEFAULT_DATABASE = "Periscope";

    @Produces
    @Singleton
    public MongoClient createMongoClient() {
        String uri = System.getenv().getOrDefault("MONGODB_URI", DEFAULT_URI);
        return MongoClients.create(uri);
    }

    @Produces
    @ApplicationScoped
    public Datastore mongoDs(MongoClient client) {
        String database = System.getenv().getOrDefault("MONGODB_DATABASE", DEFAULT_DATABASE);
        MapperOptions options = MapperOptions.builder()
                .storeEmpties(true)
                .build();
        Datastore ds = Morphia.createDatastore(client, database, options);
        ds.getMapper().map(
                User.class,
                Project.class,
                Patent.class,
                Rule.class,
                Files.class,
                Country.class,
                ApplicantType.class,
                CommonDescriptor.class);
        ds.ensureIndexes();
        return ds;
    }

    @Produces
    @ApplicationScoped
    public GridFSBucket gridFSBucket(MongoClient client) {
        String database = System.getenv().getOrDefault("MONGODB_DATABASE", DEFAULT_DATABASE);
        return GridFSBuckets.create(client.getDatabase(database));
    }

    @Produces
    public Logger produceLog(InjectionPoint injectionPoint) {
        return Logger.getLogger(injectionPoint.getMember().getDeclaringClass()
                .getName());
    }
}
