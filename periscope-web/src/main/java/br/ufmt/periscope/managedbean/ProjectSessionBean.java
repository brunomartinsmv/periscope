package br.ufmt.periscope.managedbean;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import dev.morphia.Datastore;
import static dev.morphia.query.filters.Filters.eq;
import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.bson.types.ObjectId;

@Named("projectSession")
@SessionScoped
public class ProjectSessionBean implements Serializable {

    private static final long serialVersionUID = -202445705543842694L;

    @Inject
    private Datastore ds;
    private Project currentProject;

    public String openProject(String idProject) {
        currentProject = ds.find(Project.class)
                .filter(eq("_id", new ObjectId(idProject)))
                .first();

        if (isProjectSelected()) {
            return "projectHome";
        } else {
            return null;
        }
    }

    public boolean isProjectSelected() {
        return currentProject != null;
    }

    @Named
    @Produces
    @CurrentProject
    public Project getCurrentProject() {
        return currentProject;
    }

}
