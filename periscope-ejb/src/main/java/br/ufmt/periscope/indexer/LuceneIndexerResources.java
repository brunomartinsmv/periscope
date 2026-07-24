package br.ufmt.periscope.indexer;

import br.ufmt.periscope.bean.SeedBean;
import br.ufmt.periscope.indexer.resources.analysis.DataSignaturesAnalyzer;
import br.ufmt.periscope.indexer.resources.analysis.FastJoinAnalyzer;
import java.io.IOException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.LockObtainFailedException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Lucene index access for harmonization.
 * <p>
 * Lucene 6 indexes are incompatible with Lucene 9. On open failure due to an
 * old format under {@link SeedBean#PERISCOPE_DIR}, the directory is wiped so a
 * fresh Lucene 9 index can be built (reindex on next write / seed).
 */
@Named
@ApplicationScoped
public class LuceneIndexerResources {

    private static final Logger LOG = Logger.getLogger(LuceneIndexerResources.class.getName());

    private @Inject FastJoinAnalyzer fastJoinAnalyzer;
    private @Inject DataSignaturesAnalyzer dataSignaturesAnalyzer;

    public IndexReader getReader() {
        Directory dir = this.getLocalLuceneDirectory();
        if (dir == null) {
            return null;
        }
        try {
            if (!DirectoryReader.indexExists(dir)) {
                return null;
            }
            return DirectoryReader.open(dir);
        } catch (IndexFormatTooOldException e) {
            LOG.log(Level.WARNING, "Incompatible Lucene index under {0}; wiping for Lucene 9",
                    SeedBean.PERISCOPE_DIR);
            wipeIndexDirectory();
            return null;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to open Lucene IndexReader", e);
            return null;
        }
    }

    public IndexWriter getIndexWriter() {
        Directory dir = this.getLocalLuceneDirectory();
        if (dir == null) {
            return null;
        }
        IndexWriterConfig config = this.getIndexConfig();
        try {
            return new IndexWriter(dir, config);
        } catch (IndexFormatTooOldException | LockObtainFailedException ex) {
            LOG.log(Level.WARNING, "Lucene writer open failed; wiping index and retrying", ex);
            wipeIndexDirectory();
            dir = this.getLocalLuceneDirectory();
            if (dir == null) {
                return null;
            }
            try {
                return new IndexWriter(dir, this.getIndexConfig());
            } catch (IOException retryEx) {
                LOG.log(Level.SEVERE, "Retry IndexWriter open failed", retryEx);
                return null;
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Failed to open Lucene IndexWriter", ex);
            return null;
        }
    }

    private IndexWriterConfig getIndexConfig() {
        return new IndexWriterConfig(dataSignaturesAnalyzer)
                .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    }

    public Directory getLocalLuceneDirectory() {
        try {
            File base = new File(SeedBean.PERISCOPE_DIR);
            if (!base.exists() && !base.mkdirs()) {
                LOG.warning("Could not create PERISCOPE_DIR: " + SeedBean.PERISCOPE_DIR);
            }
            return FSDirectory.open(base.toPath());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Failed to open Lucene directory", ex);
            return null;
        }
    }

    /**
     * Deletes all files under {@link SeedBean#PERISCOPE_DIR} so Lucene 9 can recreate the index.
     */
    public void wipeIndexDirectory() {
        Path root = new File(SeedBean.PERISCOPE_DIR).toPath();
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(root))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            LOG.log(Level.WARNING, "Could not delete " + p, e);
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed wiping Lucene index directory", e);
        }
    }

    public void closeWriter(IndexWriter writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.deleteUnusedFiles();
            writer.commit();
            writer.close();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public void closeReader(IndexReader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
