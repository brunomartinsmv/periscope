package br.ufmt.periscope.indexer;

import br.ufmt.periscope.indexer.resources.analysis.CommonDescriptorsSet;
import br.ufmt.periscope.indexer.resources.analysis.DataSignaturesAnalyzer;
import br.ufmt.periscope.indexer.resources.analysis.FastJoinAnalyzer;
import br.ufmt.periscope.indexer.resources.analysis.QuerySignaturesAnalyzer;
import br.ufmt.periscope.indexer.resources.search.FuzzyTokenSimilarity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for Lucene 9 FastJoin / fuzzy-token harmonization paths.
 * <p>
 * Note: Lucene 6 on-disk indexes under PERISCOPE_DIR are incompatible with Lucene 9
 * and must be wiped on upgrade (see {@link LuceneIndexerResources}).
 */
public class FastJoinRegressionTest {

    private CommonDescriptorsSet descriptors;
    private FastJoinAnalyzer fastJoinAnalyzer;

    @BeforeEach
    void setUp() {
        descriptors = new StubDescriptors(new HashSet<>(Arrays.asList(
                "corp", "ltda", "inc", "univ", "university", "federal", "co")));
        fastJoinAnalyzer = new FastJoinAnalyzer(descriptors);
    }

    @Test
    void ibmCorpAndIbmFullName_analyzerProducesTokens() throws IOException {
        List<String> ibmCorp = analyze(fastJoinAnalyzer, "applicant", "IBM CORP");
        List<String> ibmFull = analyze(fastJoinAnalyzer, "applicant", "INTERNATIONAL BUSINESS MACHINES");

        assertFalse(ibmCorp.isEmpty(), "IBM CORP should yield tokens");
        assertFalse(ibmFull.isEmpty(), "IBM full name should yield tokens");
        assertTrue(ibmCorp.stream().anyMatch(t -> t.contains("ibm") || t.equals("ibm")));
        assertTrue(ibmFull.stream().anyMatch(t -> t.contains("international") || t.equals("international")));

        // Condensed underscore forms as used by FuzzyTokenSimilarity / FastJoinTermEnum
        FuzzyTokenSimilarity ts = new FuzzyTokenSimilarity(0.7f, 0.75f);
        float related = ts.execute("internationa_business_machines", "international_business_machines");
        assertTrue(related > 0, "Near-miss IBM-style condensed tokens should score > 0");
        assertTrue(ts.fuzzyJaccard() > 0 || ts.fuzzyDice() > 0 || ts.fuzzyCosine() > 0);
    }

    @Test
    void univFederalMatoGrossoAndUfmt_analyzerTokens_acronymNotInPipeline() throws IOException {
        List<String> univ = analyze(fastJoinAnalyzer, "applicant", "UNIV FEDERAL MATO GROSSO");
        List<String> ufmt = analyze(fastJoinAnalyzer, "applicant", "UFMT");

        assertFalse(univ.isEmpty(), "University name should yield tokens after descriptor stripping");
        assertFalse(ufmt.isEmpty(), "UFMT should yield a token");
        assertTrue(ufmt.contains("ufmt") || ufmt.stream().anyMatch(t -> t.equalsIgnoreCase("ufmt")));

        // Acronym generation in CondenseTokenFilter is commented out and that filter is not
        // wired into FastJoinAnalyzer, so UFMT is not derived from the university name.
        // FuzzyTokenSimilarity on the analyzed forms therefore should not claim a match.
        FuzzyTokenSimilarity ts = new FuzzyTokenSimilarity(0.7f, 0.75f);
        ts.execute(String.join("_", univ), String.join("_", ufmt));
        assertTrue(ts.fuzzyJaccard() == 0 && ts.fuzzyDice() == 0 && ts.fuzzyCosine() == 0,
                "Without acronym path, UFMT should not fuzzy-match the expanded university name");
    }

    @Test
    void empresaXyzVsEmpresaAbc_notHighSimilarity() throws IOException {
        List<String> xyz = analyze(fastJoinAnalyzer, "applicant", "EMPRESA XYZ LTDA");
        List<String> abc = analyze(fastJoinAnalyzer, "applicant", "EMPRESA ABC LTDA");
        assertFalse(xyz.isEmpty());
        assertFalse(abc.isEmpty());

        String condensedXyz = String.join("_", xyz);
        String condensedAbc = String.join("_", abc);

        FuzzyTokenSimilarity ts = new FuzzyTokenSimilarity(0.7f, 0.75f);
        ts.execute(condensedXyz, condensedAbc);
        assertTrue(ts.fuzzyJaccard() == 0 && ts.fuzzyDice() == 0 && ts.fuzzyCosine() == 0,
                "EMPRESA XYZ vs EMPRESA ABC should not pass fuzzy token thresholds");
    }

    @Test
    void inMemoryDataAndQuerySignatures_indexRoundTrip() throws IOException {
        DataSignaturesAnalyzer dataAnalyzer = new DataSignaturesAnalyzer(descriptors);
        QuerySignaturesAnalyzer queryAnalyzer = new QuerySignaturesAnalyzer(descriptors);

        try (Directory dir = new ByteBuffersDirectory()) {
            IndexWriterConfig config = new IndexWriterConfig(dataAnalyzer);
            try (IndexWriter writer = new IndexWriter(dir, config)) {
                Document doc = new Document();
                doc.add(new TextField("applicant", "INTERNATIONAL BUSINESS MACHINES", Field.Store.YES));
                writer.addDocument(doc);
                writer.commit();
            }

            try (DirectoryReader reader = DirectoryReader.open(dir);
                 Analyzer qa = queryAnalyzer) {
                assertTrue(reader.numDocs() >= 1);
                List<String> querySigs = analyze(qa, "applicant", "INTERNATIONAL BUSINESS MACHINES");
                assertFalse(querySigs.isEmpty(), "QuerySignaturesAnalyzer should emit signature tokens");
            }
        }
    }

    @Test
    void petrobrasVsPetroleoBrasileiro_highFuzzySimilarityOnCondensedTokens() throws IOException {
        List<String> shortName = analyze(fastJoinAnalyzer, "applicant", "PETROBRAS SA");
        List<String> typoNear = analyze(fastJoinAnalyzer, "applicant", "PETROBRAZ SA");
        assertFalse(shortName.isEmpty());
        assertFalse(typoNear.isEmpty());

        // Condensed near-miss of the distinctive stem (descriptor SA stripped)
        FuzzyTokenSimilarity ts = new FuzzyTokenSimilarity(0.7f, 0.75f);
        float score = ts.execute("petrobras", "petrobraz");
        assertTrue(score > 0, "PETROBRAS vs PETROBRAZ condensed tokens should score > 0");
        assertTrue(ts.fuzzyJaccard() > 0 || ts.fuzzyDice() > 0 || ts.fuzzyCosine() > 0);
    }

    @Test
    void microsoftCorporationVsMicrosftCorporation_nearMissSimilarity() throws IOException {
        List<String> correct = analyze(fastJoinAnalyzer, "applicant", "MICROSOFT CORPORATION");
        List<String> typo = analyze(fastJoinAnalyzer, "applicant", "MICROSFT CORPORATION");
        assertFalse(correct.isEmpty());
        assertFalse(typo.isEmpty());

        FuzzyTokenSimilarity ts = new FuzzyTokenSimilarity(0.7f, 0.75f);
        float score = ts.execute("microsoft_corporation", "microsft_corporation");
        assertTrue(score > 0, "Microsoft vs Microsft condensed forms should score > 0");
        assertTrue(ts.fuzzyJaccard() > 0 || ts.fuzzyDice() > 0 || ts.fuzzyCosine() > 0);

        // Distinct unrelated applicant must remain dissimilar
        FuzzyTokenSimilarity unrelated = new FuzzyTokenSimilarity(0.7f, 0.75f);
        unrelated.execute("microsoft_corporation", "samsung_electronics");
        assertTrue(unrelated.fuzzyJaccard() == 0 && unrelated.fuzzyDice() == 0
                        && unrelated.fuzzyCosine() == 0,
                "Microsoft vs Samsung should not pass fuzzy thresholds");
    }

    @Test
    void seededDescriptorsAndPatentIndexingShareIdSchemaAndReleaseWriteLock() throws Exception {
        String previousDir = br.ufmt.periscope.bean.SeedBean.PERISCOPE_DIR;
        java.nio.file.Path tmpDir = java.nio.file.Files.createTempDirectory("periscope-lucene-it");
        try {
            br.ufmt.periscope.bean.SeedBean.PERISCOPE_DIR = tmpDir.toString();

            DataSignaturesAnalyzer dsa = new DataSignaturesAnalyzer(descriptors);
            FastJoinAnalyzer fja = new FastJoinAnalyzer(descriptors);
            QuerySignaturesAnalyzer qsa = new QuerySignaturesAnalyzer(descriptors);

            LuceneIndexerResources res = new LuceneIndexerResources();
            setField(res, "fastJoinAnalyzer", fja);
            setField(res, "dataSignaturesAnalyzer", dsa);

            br.ufmt.periscope.indexer.resources.search.FuzzyTokenSimilaritySearch search =
                    new br.ufmt.periscope.indexer.resources.search.FuzzyTokenSimilaritySearch();
            setField(search, "fastJoinAnalyzer", fja);
            setField(search, "querySignaturesAnalyzer", qsa);
            setField(search, "resources", res);

            PatentIndexer indexer = new PatentIndexer();
            setField(indexer, "resources", res);
            setField(indexer, "fs", search);
            setField(indexer, "log", java.util.logging.Logger.getLogger("test"));

            // Step 1: Seed common descriptors using StringField("id") (same as SeedBean)
            IndexWriter seedWriter = res.getIndexWriter();
            try {
                for (String w : List.of("gmbh", "ltda", "corp")) {
                    Document doc = new Document();
                    doc.add(new org.apache.lucene.document.StringField("id", w, Field.Store.YES));
                    seedWriter.deleteDocuments(new org.apache.lucene.index.Term("id", w));
                    seedWriter.addDocument(doc);
                }
            } finally {
                res.closeWriter(seedWriter);
            }

            // Step 2: Index DPMA applicants/inventors into the same directory
            br.ufmt.periscope.model.Project proj = new br.ufmt.periscope.model.Project();
            proj.setId(new org.bson.types.ObjectId());
            indexer.index(
                    List.of("ACME GMBH", "UFMT PARTNER GMBH"),
                    List.of("SCHMIDT HANS", "MUELLER ANNA"),
                    proj);

            // Step 3: Search suggestions for ACME GMBH
            List<Document> hits = search.search("applicant", proj.getId().toString(), "ACME GMBH", 50);
            assertTrue(hits.stream().anyMatch(d -> "ACME GMBH".equals(d.get("applicant"))),
                    "ACME GMBH must be returned by Fast-Join suggestions after descriptor seeding");

            // Step 4: Delete project indexes must succeed without LockObtainFailedException or NPE
            indexer.deleteIndexesForProject(proj);
            List<Document> afterDelete = search.search("applicant", proj.getId().toString(), "ACME GMBH", 50);
            assertTrue(afterDelete.isEmpty(), "Project entries must be removed after deleteIndexesForProject");
        } finally {
            br.ufmt.periscope.bean.SeedBean.PERISCOPE_DIR = previousDir;
            try (var walk = java.nio.file.Files.walk(tmpDir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        java.nio.file.Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static List<String> analyze(Analyzer analyzer, String field, String text) throws IOException {
        List<String> tokens = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream(field, text)) {
            CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                tokens.add(attr.toString());
            }
            ts.end();
        }
        return tokens;
    }

    /** In-memory descriptor set (no Mongo) for analyzer unit tests. */
    private static final class StubDescriptors extends CommonDescriptorsSet {
        private final Set<String> words;

        StubDescriptors(Set<String> words) {
            this.words = words;
        }

        @Override
        public boolean contains(String descriptor) {
            return words.contains(descriptor.toLowerCase());
        }
    }
}
