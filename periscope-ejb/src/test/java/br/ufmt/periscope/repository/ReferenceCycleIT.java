package br.ufmt.periscope.repository;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.ConnectionCheckedInEvent;
import com.mongodb.event.ConnectionCheckedOutEvent;
import com.mongodb.event.ConnectionPoolListener;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

import static dev.morphia.query.filters.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Regression guard for the circular {@code @Reference} graph
 * ({@code Project.patents} ⇄ {@code Patent.project}, {@code Project.rules} ⇄ {@code Rule.project},
 * {@code Project.owner} ⇄ {@code User.projects}).
 * <p>
 * When those collections are resolved eagerly, Morphia recurses through the cycle and each nested
 * query holds a pooled connection, exhausting the pool. The tiny pool configured here (5
 * connections, 3s wait) turns that regression into a {@code MongoTimeoutException} instead of a
 * 120s hang.
 * <p>
 * Real execution requires Docker (GitHub Actions {@code integration} job with {@code -Pit}).
 * Without Docker the tests are skipped via assumption so {@code mvn verify -Pit} still succeeds
 * on VMs that lack Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReferenceCycleIT {

    private static final int POOL_SIZE = 5;

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    private final AtomicInteger finds = new AtomicInteger();
    private final AtomicInteger checkedOut = new AtomicInteger();
    private final AtomicInteger maxCheckedOut = new AtomicInteger();

    private MongoClient client;
    private Datastore datastore;

    @BeforeAll
    void requireDockerAndConnect() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available — IT skipped (runs in CI with Testcontainers)");
        assumeTrue(MONGO.isRunning(), "MongoDB container did not start");

        client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(MONGO.getReplicaSetUrl()))
                .addCommandListener(new CommandListener() {
                    @Override
                    public void commandStarted(CommandStartedEvent event) {
                        if ("find".equals(event.getCommandName())) {
                            finds.incrementAndGet();
                        }
                    }
                })
                .applyToConnectionPoolSettings(builder -> builder
                        .addConnectionPoolListener(new ConnectionPoolListener() {
                            @Override
                            public void connectionCheckedOut(ConnectionCheckedOutEvent event) {
                                maxCheckedOut.accumulateAndGet(checkedOut.incrementAndGet(), Math::max);
                            }

                            @Override
                            public void connectionCheckedIn(ConnectionCheckedInEvent event) {
                                checkedOut.decrementAndGet();
                            }
                        })
                        .maxSize(POOL_SIZE)
                        .maxWaitTime(3, TimeUnit.SECONDS))
                .build());

        MapperOptions options = MapperOptions.builder().storeEmpties(true).build();
        datastore = Morphia.createDatastore(client, "PeriscopeCycleIT", options);
        datastore.getMapper().map(User.class, Project.class, Patent.class, Rule.class);
        datastore.ensureIndexes();
    }

    @AfterAll
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @BeforeEach
    void resetCounters() {
        assumeTrue(datastore != null);
        for (String collection : new String[]{"user", "project", "patent", "rule"}) {
            datastore.getDatabase().getCollection(collection).deleteMany(new Document());
        }
        finds.set(0);
        checkedOut.set(0);
        maxCheckedOut.set(0);
    }

    @Test
    void loadingAProjectDoesNotRecurseThroughItsBackReferences() {
        Project project = seedCycle(3);

        finds.set(0);
        maxCheckedOut.set(0);
        Project loaded = datastore.find(Project.class).filter(eq("_id", project.getId())).first();

        assertThat(loaded).isNotNull();
        assertThat(loaded.getTitle()).isEqualTo("Cycle Project");
        assertThat(loaded.getOwner().getUsername()).isEqualTo("cycle-user");
        // project + owner + observer: a recursive resolution would run dozens of nested queries
        assertThat(finds.get()).isLessThanOrEqualTo(5);
        assertThat(maxCheckedOut.get()).isLessThan(POOL_SIZE);
    }

    @Test
    void patentAndRuleCountsComeFromStoredIdsWithoutQuerying() {
        Project project = seedCycle(3);
        Project loaded = datastore.find(Project.class).filter(eq("_id", project.getId())).first();

        finds.set(0);
        assertThat(loaded.getPatents()).hasSize(3);
        assertThat(loaded.getPatents().isEmpty()).isFalse();
        assertThat(loaded.getRules()).hasSize(1);
        assertThat(finds.get()).isZero();
    }

    @Test
    void savingALoadedProjectKeepsItsPatentAndRuleReferences() {
        Project project = seedCycle(3);
        Project loaded = datastore.find(Project.class).filter(eq("_id", project.getId())).first();

        loaded.setDescription("edited");
        datastore.save(loaded);

        Document raw = datastore.getDatabase().getCollection("project")
                .find(new Document("_id", project.getId())).first();
        assertThat(raw.getString("description")).isEqualTo("edited");
        assertThat((List<?>) raw.get("patents")).hasSize(3);
        assertThat((List<?>) raw.get("rules")).hasSize(1);
    }

    @Test
    void loadingChildrenResolvesTheParentWithoutRecursion() {
        seedCycle(3);

        finds.set(0);
        maxCheckedOut.set(0);
        Patent patent = datastore.find(Patent.class).filter(eq("publicationNumber", "PN1")).first();
        Rule rule = datastore.find(Rule.class).filter(eq("name", "cycle-rule")).first();
        User user = datastore.find(User.class).filter(eq("username", "cycle-user")).first();

        assertThat(patent.getProject().getTitle()).isEqualTo("Cycle Project");
        assertThat(rule.getProject().getTitle()).isEqualTo("Cycle Project");
        assertThat(user.getProjects()).hasSize(1);
        assertThat(maxCheckedOut.get()).isLessThan(POOL_SIZE);
        assertThat(finds.get()).isLessThanOrEqualTo(15);
    }

    @Test
    void iteratingTheLazyCollectionsReturnsRealEntities() {
        Project project = seedCycle(2);
        Project loaded = datastore.find(Project.class).filter(eq("_id", project.getId())).first();

        List<String> titles = new ArrayList<>();
        for (Patent patent : loaded.getPatents()) {
            titles.add(patent.getTitleSelect());
        }

        assertThat(titles).containsExactlyInAnyOrder("P0", "P1");
        // Rule#getName() upper-cases on read
        assertThat(loaded.getRules().get(0).getName()).isEqualToIgnoringCase("cycle-rule");
        assertThat(maxCheckedOut.get()).isLessThan(POOL_SIZE);
    }

    /**
     * Creates a project whose every reference cycle is populated on both sides.
     */
    private Project seedCycle(int patentCount) {
        User owner = new User();
        owner.setUsername("cycle-user");
        owner.setPassword("secret");
        owner.setFirstname("Cycle");
        owner.setLastname("User");
        owner.setEmail("cycle@example.com");
        owner.setUserLevel(UserLevel.ADMIN);
        datastore.save(owner);

        Project project = new Project();
        project.setTitle("Cycle Project");
        project.setCreatedAt(new Date());
        project.setOwner(owner);
        project.getObservers().add(owner);
        datastore.save(project);

        for (int i = 0; i < patentCount; i++) {
            Patent patent = new Patent();
            patent.setTitleSelect("P" + i);
            patent.setPublicationNumber("PN" + i);
            patent.setProject(project);
            datastore.save(patent);
            project.getPatents().add(patent);
        }

        Rule rule = new Rule();
        rule.setName("cycle-rule");
        rule.setType(RuleType.APPLICANT);
        rule.setProject(project);
        datastore.save(rule);
        project.getRules().add(rule);

        owner.getProjects().add(project);
        datastore.save(owner);
        datastore.save(project);
        return project;
    }
}
