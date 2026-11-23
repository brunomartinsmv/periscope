package br.ufmt.periscope.api.security;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import dev.morphia.Datastore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bson.Document;

/**
 * Tracks revoked JWT IDs ({@code jti}) until expiration.
 * Uses an in-memory map for fast checks and mirrors to MongoDB ({@code revoked_tokens})
 * when a Morphia {@link Datastore} is active.
 */
@ApplicationScoped
public class TokenRevocationStore {

    private static final String COLLECTION = "revoked_tokens";
    private static final String USER_VERSIONS_COLLECTION = "revoked_token_versions";

    @Inject
    private Instance<Datastore> datastoreInstance;

    private final ConcurrentHashMap<String, Long> revokedMemory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> minUserVersionMemory = new ConcurrentHashMap<>();
    private volatile boolean ttlIndexEnsured = false;

    public void revoke(String jti, long expEpochSeconds) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        purgeExpiredMemory();
        revokedMemory.put(jti, expEpochSeconds);

        MongoCollection<Document> coll = mongoCollection();
        if (coll != null) {
            try {
                ensureTtlIndex(coll);
                Document doc = new Document("_id", jti)
                        .append("expireAt", Date.from(Instant.ofEpochSecond(expEpochSeconds)));
                coll.replaceOne(Filters.eq("_id", jti), doc, new ReplaceOptions().upsert(true));
            } catch (RuntimeException ignored) {
                // In-memory revocation still protects the current instance
            }
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        purgeExpiredMemory();
        Long exp = revokedMemory.get(jti);
        if (exp != null && exp > Instant.now().getEpochSecond()) {
            return true;
        }
        MongoCollection<Document> coll = mongoCollection();
        if (coll != null) {
            try {
                Document found = coll.find(Filters.eq("_id", jti)).first();
                if (found != null) {
                    Date expireAt = found.getDate("expireAt");
                    if (expireAt == null || expireAt.after(new Date())) {
                        revokedMemory.put(jti,
                                expireAt != null ? expireAt.toInstant().getEpochSecond() : Long.MAX_VALUE);
                        return true;
                    }
                }
            } catch (RuntimeException ignored) {
                // Ignore transient DB lookup failures
            }
        }
        return false;
    }

    public void revokeAllForUserBeforeVersion(String username, long minValidVersion) {
        if (username == null || username.isBlank() || minValidVersion <= 0L) {
            return;
        }
        minUserVersionMemory.merge(username, minValidVersion, Math::max);
        MongoCollection<Document> coll = mongoCollection(USER_VERSIONS_COLLECTION);
        if (coll != null) {
            try {
                Document doc = new Document("_id", username)
                        .append("minTokenVersion", minValidVersion)
                        .append("updatedAt", new Date());
                coll.replaceOne(Filters.eq("_id", username), doc, new ReplaceOptions().upsert(true));
            } catch (RuntimeException ignored) {
                // In-memory revocation still protects the current instance
            }
        }
    }

    public boolean isVersionRevoked(String username, long tokenVersion) {
        if (username == null || username.isBlank()) {
            return false;
        }
        Long memMin = minUserVersionMemory.get(username);
        if (memMin != null && tokenVersion < memMin) {
            return true;
        }
        MongoCollection<Document> coll = mongoCollection(USER_VERSIONS_COLLECTION);
        if (coll != null) {
            try {
                Document found = coll.find(Filters.eq("_id", username)).first();
                if (found != null) {
                    Object raw = found.get("minTokenVersion");
                    if (raw instanceof Number n) {
                        long dbMin = n.longValue();
                        minUserVersionMemory.merge(username, dbMin, Math::max);
                        return tokenVersion < dbMin;
                    }
                }
            } catch (RuntimeException ignored) {
                // Ignore transient DB lookup failures
            }
        }
        return false;
    }

    private void purgeExpiredMemory() {
        long now = Instant.now().getEpochSecond();
        revokedMemory.entrySet().removeIf(e -> e.getValue() <= now);
    }

    private MongoCollection<Document> mongoCollection() {
        return mongoCollection(COLLECTION);
    }

    private MongoCollection<Document> mongoCollection(String collectionName) {
        if (datastoreInstance == null || datastoreInstance.isUnsatisfied() || datastoreInstance.isAmbiguous()) {
            return null;
        }
        try {
            Datastore ds = datastoreInstance.get();
            return ds != null ? ds.getDatabase().getCollection(collectionName) : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void ensureTtlIndex(MongoCollection<Document> coll) {
        if (ttlIndexEnsured) {
            return;
        }
        synchronized (this) {
            if (!ttlIndexEnsured) {
                coll.createIndex(new Document("expireAt", 1),
                        new IndexOptions().expireAfter(0L, TimeUnit.SECONDS));
                ttlIndexEnsured = true;
            }
        }
    }
}
