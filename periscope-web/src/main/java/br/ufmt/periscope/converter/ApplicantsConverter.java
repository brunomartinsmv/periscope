package br.ufmt.periscope.converter;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.repository.ApplicantRepository;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

@FacesConverter(value = "applicantConverter", forClass = Applicant.class)
public class ApplicantsConverter implements Converter {

    @Inject
    private ApplicantRepository applicantRepository;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String name) {
        Applicant applicant;
        applicant = applicantRepository.getApplicantByName(name);
        return applicant;

    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        Applicant applicant = (Applicant) value;
        return applicant.getName() + "";
    }
}
