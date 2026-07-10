# Fase 3 — Migração MongoDB

**PR:** `cursor/fase-3-mongodb-8905`  
**Depende de:** Fase 2  
**Bloqueia:** Fases 5, 6, 7  
**Paralelo com:** Fase 4

## Objetivo

Migrar de **Morphia 1.2.3 + MongoDB driver 2.x (API legada)** para **Morphia 2.4 + MongoDB driver 5.x**, eliminando `DB`, `DBCollection`, `GridFS` legado, `MapReduce` e `AggregationOutput`.

---

## Stack alvo

| Componente | Versão |
|------------|--------|
| MongoDB Server | 7.x |
| mongo-java-driver (sync) | 5.2.1 |
| Morphia | 2.4.14 |
| GridFS | API moderna (`com.mongodb.client.gridfs`) |

---

## Tarefas

### 3.1 Atualizar dependências

```xml
<dependency>
    <groupId>dev.morphia.morphia</groupId>
    <artifactId>morphia-core</artifactId>
    <version>${morphia.version}</version>
</dependency>
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>${mongodb.version}</version>
</dependency>
```

**Remover:** `com.github.jmkgreen.morphia:morphia:1.2.3`

### 3.2 Migrar anotações Morphia (modelos)

Pacote muda: `com.github.jmkgreen.morphia.annotations.*` → `dev.morphia.annotations.*`

**Arquivos (~15 modelos):**

```
periscope-ejb/src/main/java/br/ufmt/periscope/model/
├── User.java, Project.java, Patent.java, Rule.java
├── Applicant.java, Inventor.java, Country.java, ...
└── CommonDescriptor.java (indexer)
```

**Mudanças comuns Morphia 2:**

| Morphia 1 | Morphia 2 |
|-----------|-----------|
| `@Entity` | `@Entity` (novo pacote) |
| `@Id` ObjectId | `@Id` ObjectId (mantém) |
| `@Reference` | `@Reference` ou `@DBRef` — validar lazy loading |
| `@Embedded` | `@Embedded` |
| `@Transient` | `@Transient` |

### 3.3 Reescrever `Resources.java`

**Antes (legado):**

```java
@Produces Mongo createMongo() { return new Mongo(); }
@Produces Datastore mongoDs(Mongo mongo, Morphia morphia) {
    return morphia.createDatastore(mongo, "Periscope");
}
@Produces GridFS produceFs(...) { ... new GridFS(db); }
```

**Depois (moderno):**

```java
@ApplicationScoped
public class MongoProducer {
    @Produces @Singleton
    MongoClient createMongoClient() {
        String uri = System.getenv().getOrDefault(
            "MONGODB_URI", "mongodb://localhost:27017");
        return MongoClients.create(uri);
    }

    @Produces @ApplicationScoped
    Morphia morphia() {
        return Morphia.create("br.ufmt.periscope.model");
    }

    @Produces @ApplicationScoped
    Datastore datastore(MongoClient client, Morphia morphia) {
        return morphia.createDatastore(client, "Periscope");
    }
}
```

**GridFS moderno:**

```java
@Produces
GridFSBucket gridFSBucket(MongoClient client) {
    return GridFSBuckets.create(client.getDatabase("Periscope"));
}
```

**Remover:** `DatastoreHolder` (não existe no Morphia 2).

### 3.4 Migrar repositórios — padrão de agregação

**API legada (ex.: `ApplicantRepository`):**

```java
DBCursor cursor = ds.getCollection(Patent.class).find(where, keys);
AggregationOutput output = collection.aggregate(pipeline, opts);
MapReduceCommand cmd = new MapReduceCommand(...);
```

**API moderna:**

```java
// Opção A — Morphia query API
ds.find(Patent.class)
  .filter(Filters.eq("project", project))
  .iterator();

// Opção B — Aggregation Pipeline nativa
MongoCollection<Document> coll = ds.getDatabase()
    .getCollection(ds.getMapper().getEntityModel(Patent.class).getCollectionName());
coll.aggregate(pipeline).into(new ArrayList<>());
```

**Repositórios a migrar (prioridade):**

| Repositório | API legada usada | Complexidade |
|-------------|------------------|--------------|
| `ApplicantRepository` | DBCursor, MapReduce, Aggregation | Alta |
| `InventorRepository` | Idem | Alta |
| `PatentRepository` | DBCollection, GridFS | Alta |
| `ProjectRepository` | DBCursor, GridFS, DBRef | Alta |
| `RuleRepository` | Query Morphia, DB | Média |
| `*Repository` (relatórios) | AggregationOutput | Média |
| `Harmonization.java` | BasicDBObjectBuilder, Mapper | Média |

### 3.5 Migrar GridFS (upload/download)

**Arquivos:**

- `PatentController.java` — `GridFS`, `GridFSDBFile`, `GridFSInputFile`
- `ResourcesLazy.java`
- `ProjectRepository.java`

**Padrão moderno:**

```java
// Upload
try (InputStream stream = ...) {
    gridFSBucket.uploadFromStream(filename, stream);
}

// Download
GridFSFile file = gridFSBucket.find(Filters.eq("filename", name)).first();
try (InputStream stream = gridFSBucket.openDownloadStream(file.getObjectId())) {
    ...
}
```

### 3.6 Migrar `SeedBean` — system.js

**Antes:** `DB db = ds.getDB(); db.getCollectionFromString("system.js")`

**Depois:**

```java
MongoDatabase db = client.getDatabase("Periscope");
MongoCollection<Document> systemJs = db.getCollection("system.js");
systemJs.replaceOne(
    Filters.eq("_id", name),
    new Document("_id", name).append("value", function),
    new ReplaceOptions().upsert(true)
);
```

### 3.7 Migrar queries Morphia

| Morphia 1 | Morphia 2 |
|-----------|-----------|
| `ds.getCount(Entity.class)` | `ds.count(Entity.class)` |
| `ds.save(entity)` | `ds.save(entity)` (similar) |
| `ds.find(Entity.class).filter(...)` | `ds.find(Entity.class).filter(...)` |
| `ds.getCollection(Class)` | Removido — usar `ds.getDatabase()` ou query API |
| `mapper.fromDBObject(...)` | `mapper.fromDocument(...)` |
| `Key<T>` | `dev.morphia.Key` ou IDs diretos |

### 3.8 Configuração externa

Variáveis de ambiente (preparação WildFly/Docker):

| Variável | Default | Uso |
|----------|---------|-----|
| `MONGODB_URI` | `mongodb://localhost:27017` | Conexão |
| `MONGODB_DATABASE` | `Periscope` | Nome do banco |

---

## Arquivos afetados (resumo)

| Categoria | Arquivos |
|-----------|----------|
| Producer/Util | `Resources.java`, `ResourcesLazy.java` |
| Modelos | ~15 em `model/` + `CommonDescriptor.java` |
| Repositórios | ~15 em `repository/` |
| Controllers | `PatentController`, `ImportPatentController`, `UserController` |
| Beans | `SeedBean`, `Harmonization.java` |
| POM | `periscope-ejb/pom.xml` |

## Critérios de aceite

- [ ] Zero imports `com.github.jmkgreen.morphia.*`
- [ ] Zero imports `com.mongodb.Mongo`, `DB`, `DBCollection`, `DBCursor`
- [ ] Zero imports `com.mongodb.gridfs.*` (legado)
- [ ] Zero `MapReduceCommand` / `AggregationOutput`
- [ ] GridFS funciona (upload + download de arquivos de patente)
- [ ] Seed data carrega na inicialização
- [ ] Repositórios de relatório retornam dados corretos
- [ ] Conexão configurável via `MONGODB_URI`

## Riscos

| Risco | Mitigação |
|-------|-----------|
| MapReduce → Aggregation Pipeline | Reescrever queries com testes de equivalência |
| `@Reference` lazy loading | Testar carregamento de Project → Patents |
| DBRef vs ObjectId em queries | Validar filtros `project.$id` → novo formato Morphia 2 |

## Validação local

- [ ] Docker MongoDB 7 + app local
- [ ] Importar patentes de teste
- [ ] Verificar relatórios de depositantes/inventores
- [ ] Upload/download de PDF de patente

## Nota sobre homologação futura

Quando homologação existir: snapshot MongoDB anonimizado restaurado via `mongorestore` antes de cada deploy de validação.
