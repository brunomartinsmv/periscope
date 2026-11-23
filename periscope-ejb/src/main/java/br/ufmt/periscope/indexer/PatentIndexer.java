package br.ufmt.periscope.indexer;

import br.ufmt.periscope.indexer.resources.search.FuzzyTokenSimilaritySearch;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.InventorRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

@Named
@RequestScoped
public class PatentIndexer {

    private @Inject
    LuceneIndexerResources resources;
    private IndexWriter writer;
    private @Inject
    ApplicantRepository paRepo;
    private @Inject
    InventorRepository inRepo;
    private @Inject
    Logger log;
    private @Inject
    FuzzyTokenSimilaritySearch fs;

    public void indexPatents(List<Patent> patents, Project project) {
        Set<String> applicants = new HashSet<String>();
        Set<String> inventors = new HashSet<String>();
        for (Patent patent : patents) {
            for (Applicant pa : patent.getApplicants()) {
                applicants.add(pa.getName());
            }
            for (Inventor inv : patent.getInventors()) {
                inventors.add(inv.getName());
            }
        }
        index(new ArrayList<String>(applicants), new ArrayList<String>(inventors), project);
    }

    public void deleteIndexesForProject(Project project) {
        if (project == null || project.getId() == null) {
            return;
        }
        IndexWriter localWriter = resources.getIndexWriter();
        if (localWriter == null) {
            return;
        }
        try {
            localWriter.deleteDocuments(new Term("project", project.getId().toString()));
            if (log != null) {
                log.info("Indices deletados com sucesso");
            }
        } catch (IOException e) {
            if (log != null) {
                log.log(Level.SEVERE, "Ocorreu algum erro deletando os indices.", e);
            }
        } finally {
            resources.closeWriter(localWriter);
        }
    }

    public void index(List<String> pas, List<String> invs, Project project) {
        if (project == null || project.getId() == null) {
            return;
        }
        IndexWriter localWriter = resources.getIndexWriter();
        if (localWriter == null) {
            return;
        }
        try {
            if (pas != null) {
                for (String a : pas) {
                    if (a == null || a.isBlank()) {
                        continue;
                    }
                    Document doc = new Document();
                    doc.add(new TextField("applicant", a, Field.Store.YES));
                    doc.add(new StringField("tokens", fs.getAnalyzed("applicant", a), Field.Store.YES));
                    doc.add(new StringField("id", project.getId().toString() + "app" + String.valueOf(a.hashCode()), Field.Store.YES));
                    doc.add(new StringField("project", project.getId().toString(), Field.Store.YES));
                    localWriter.deleteDocuments(new Term("id", doc.get("id")));
                    localWriter.addDocument(doc);
                }
            }
            if (invs != null) {
                for (String a : invs) {
                    if (a == null || a.isBlank()) {
                        continue;
                    }
                    Document doc = new Document();
                    doc.add(new TextField("inventor", a, Field.Store.YES));
                    doc.add(new StringField("tokens", fs.getAnalyzed("inventor", a), Field.Store.YES));
                    doc.add(new StringField("id", project.getId().toString() + "inv" + String.valueOf(a.hashCode()), Field.Store.YES));
                    doc.add(new StringField("project", project.getId().toString(), Field.Store.YES));
                    localWriter.deleteDocuments(new Term("id", doc.get("id")));
                    localWriter.addDocument(doc);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PatentIndexer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            resources.closeWriter(localWriter);
        }
    }

    public void reindex(Project project) {
        deleteIndexesForProject(project);
        List<String> pas = paRepo.getApplicants(project, "");
        List<String> invs = inRepo.getInventors(project, "");
        index(pas, invs, project);
    }

    public void indexRule(List<String> pas, String pa, List<String> invs, String inv, Project project) {
        if (project == null || project.getId() == null) {
            return;
        }
        IndexWriter localWriter = resources.getIndexWriter();
        if (localWriter == null) {
            return;
        }
        try {
            if (pas != null) {
                for (String a : pas) {
                    if (a == null) {
                        continue;
                    }
                    // Must use StringField for "id" — same index options as index()
                    localWriter.deleteDocuments(new Term("id",
                            project.getId().toString() + "app" + String.valueOf(a.hashCode())));
                }
                if (pa != null && !pa.isBlank()) {
                    Document doc = new Document();
                    doc.add(new StringField("id", project.getId().toString() + "app" + String.valueOf(pa.hashCode()), Field.Store.YES));
                    doc.add(new TextField("applicant", pa, Field.Store.YES));
                    doc.add(new StringField("tokens", fs.getAnalyzed("applicant", pa), Field.Store.YES));
                    doc.add(new StringField("project", project.getId().toString(), Field.Store.YES));
                    localWriter.deleteDocuments(new Term("id", doc.get("id")));
                    localWriter.addDocument(doc);
                }
            }
            if (invs != null) {
                for (String a : invs) {
                    if (a == null) {
                        continue;
                    }
                    localWriter.deleteDocuments(new Term("id",
                            project.getId().toString() + "inv" + String.valueOf(a.hashCode())));
                }
                if (inv != null && !inv.isBlank()) {
                    Document doc = new Document();
                    doc.add(new StringField("id", project.getId().toString() + "inv" + String.valueOf(inv.hashCode()), Field.Store.YES));
                    doc.add(new TextField("inventor", inv, Field.Store.YES));
                    doc.add(new StringField("tokens", fs.getAnalyzed("inventor", inv), Field.Store.YES));
                    doc.add(new StringField("project", project.getId().toString(), Field.Store.YES));
                    localWriter.deleteDocuments(new Term("id", doc.get("id")));
                    localWriter.addDocument(doc);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PatentIndexer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            resources.closeWriter(localWriter);
        }
    }

    public void indexPatent(Patent p) {
        Long in = System.currentTimeMillis();
        List<String> pas = new ArrayList<String>();
        List<String> invs = new ArrayList<String>();
        if (p.getApplicants() != null) {
            for (Applicant a : p.getApplicants()) {
                pas.add(a.getName());
            }
        }
        in = System.currentTimeMillis();
        if (p.getInventors() != null) {
            for (Inventor a : p.getInventors()) {
                invs.add(a.getName());
            }
        }
        index(pas, invs, p.getProject());
    }
}
