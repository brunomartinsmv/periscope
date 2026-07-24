package br.ufmt.periscope.repository;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static dev.morphia.query.filters.Filters.eq;

/**
 * Morphia save/find round-trip against MongoDB 7 via Testcontainers.
 * <p>
 * Real execution requires Docker (GitHub Actions {@code integration} job with {@code -Pit}).
 * Without Docker the tests are skipped via assumption so {@code mvn verify -Pit} still succeeds
 * on VMs that lack Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PatentRepositoryIT {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    private MongoClient client;
    private Datastore datastore;

    @BeforeAll
    void requireDockerAndConnect() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available — IT skipped (runs in CI with Testcontainers)");
        assumeTrue(MONGO.isRunning(), "MongoDB container did not start");

        client = MongoClients.create(MONGO.getReplicaSetUrl());
        MapperOptions options = MapperOptions.builder().storeEmpties(true).build();
        datastore = Morphia.createDatastore(client, "PeriscopeIT", options);
        datastore.getMapper().map(User.class, Project.class, Patent.class);
        datastore.ensureIndexes();
    }

    @AfterAll
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @BeforeEach
    void cleanCollections() {
        assumeTrue(datastore != null);
        datastore.getDatabase().getCollection("User").deleteMany(new Document());
        datastore.getDatabase().getCollection("Project").deleteMany(new Document());
        datastore.getDatabase().getCollection("Patent").deleteMany(new Document());
    }

    @Test
    void savesAndFindsProjectAndPatent() {
        User owner = new User();
        owner.setUsername("it-user");
        owner.setPassword("secret");
        owner.setFirstname("IT");
        owner.setLastname("User");
        owner.setEmail("it@example.com");
        owner.setUserLevel(UserLevel.ADMIN);
        datastore.save(owner);

        Project project = new Project();
        project.setTitle("IT Project");
        project.setDescription("Testcontainers Morphia round-trip");
        project.setCreatedAt(new Date());
        project.setOwner(owner);
        datastore.save(project);

        Patent patent = new Patent();
        patent.setTitleSelect("Integration Test Patent");
        patent.setPublicationNumber("BRIT0001");
        patent.setLanguage("EN");
        patent.setProject(project);
        datastore.save(patent);

        Project foundProject = datastore.find(Project.class)
                .filter(eq("title", "IT Project"))
                .first();
        assertThat(foundProject).isNotNull();
        assertThat(foundProject.getId()).isNotNull();
        assertThat(foundProject.getDescription()).contains("Testcontainers");

        Patent foundPatent = datastore.find(Patent.class)
                .filter(eq("publicationNumber", "BRIT0001"))
                .first();
        assertThat(foundPatent).isNotNull();
        assertThat(foundPatent.getTitleSelect()).isEqualTo("Integration Test Patent");
        assertThat(foundPatent.getProject()).isNotNull();
        assertThat(foundPatent.getProject().getTitle()).isEqualTo("IT Project");
    }

    @Test
    void pingCommandSucceeds() {
        Document pong = datastore.getDatabase().runCommand(new Document("ping", 1));
        assertThat(pong.getDouble("ok")).isEqualTo(1.0);
    }
}
