package br.ufmt.periscope.controller;

import br.ufmt.periscope.lazy.LazyPatentBrazilianDataModel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.model.DataModel;
import jakarta.inject.Inject;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;

/**
 * - @Named<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações relacionadas às patentes brasileiras
 */
@Named
@ViewScoped
public class PatentBrazilianController {

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    LazyPatentBrazilianDataModel patents;

    /**
     * Método pós construtor do controller<BR/>
     * Carrega as patentes brasileiras de maneira Lazy
     */
    @PostConstruct
    public void init() {
        patents.getRepo().setBlacklisted(false);
        patents.getRepo().setCompleted(true);
        patents.getRepo().setCurrentProject(currentProject);
    }

    /**
     *
     * @return
     */
    public DataModel<Patent> getPatents() {
        return patents;
    }

    /**
     *
     * @param patents
     */
    public void setPatents(DataModel<Patent> patents) {
        this.patents = (LazyPatentBrazilianDataModel) patents;
    }

    /**
     *
     * @return
     */
    public int getPartialCount() {
        return patents.getRowCount();
    }
}
