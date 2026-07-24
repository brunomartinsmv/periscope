package br.ufmt.periscope.lazy;

import java.io.Serializable;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.repository.ApplicantRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

@Named
public class LazyApplicantDataModel extends LazyDataModel<Applicant>  implements Serializable{

    private static final long serialVersionUID = 1L;

    private @Inject
    ApplicantRepository applicantRepository;
    private List<Applicant> datasource;
    private List<Applicant> selectedApplicants;
    private Integer searchType;
    private Boolean harmonization = false;

    public LazyApplicantDataModel() {
//        System.out.println("Lazy Applicant Data Model");
    }

    
    
    
    @Override
    public int count(Map<String, FilterMeta> filterBy) {
        return getRowCount();
    }

    @Override
    public int getRowCount() {
        return super.getRowCount();
    }

    @Override
    public Applicant getRowData(String key) {
        for (Applicant applicant : datasource) {
            if (applicant.getName().equals(key)) {
                return applicant;
            }
        }
        return null;
    }

    @Override
    public String getRowKey(Applicant object) {
        return object.getName();
    }

    @Override
    public List<Applicant> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        String sortField = null;
        int sortOrd = 0;
        if (sortBy != null && !sortBy.isEmpty()) {
            SortMeta meta = sortBy.values().iterator().next();
            sortField = meta.getField();
            if (meta.getOrder() == SortOrder.DESCENDING) {
                sortOrd = 1;
            }
        }
        Map<String, String> filters = new HashMap<String, String>();
        if (filterBy != null) {
            for (Map.Entry<String, FilterMeta> e : filterBy.entrySet()) {
                if (e.getValue() != null && e.getValue().getFilterValue() != null) {
                    filters.put(e.getKey(), e.getValue().getFilterValue().toString());
                }
            }
        }

        applicantRepository.setSearchType(searchType);
        if (harmonization) {
            datasource = applicantRepository.load(first, pageSize, sortField, sortOrd, filters);
        } else {
            datasource = applicantRepository.load(first, pageSize, sortField, sortOrd, filters, this.selectedApplicants);
        }
        setRowCount(applicantRepository.getCount());
        for (Applicant applicant : datasource) {
            if (this.selectedApplicants != null && this.selectedApplicants.contains(applicant)) {

                applicant.setSelected(true);
            }
        }
        return datasource;
    }

    public ApplicantRepository getApplicantRepository() {
        return applicantRepository;
    }

    public void setApplicantRepository(ApplicantRepository applicantRepository) {
        this.applicantRepository = applicantRepository;
    }

    public List<Applicant> getSelectedApplicants() {
        return selectedApplicants;
    }

    public void setSelectedApplicants(List<Applicant> selectedApplicants) {
        this.selectedApplicants = selectedApplicants;
    }

    public Integer getSearchType() {
        return searchType;
    }

    public void setSearchType(Integer searchType) {
        this.searchType = searchType;
    }

    public Boolean getHarmonization() {
        return harmonization;
    }

    public void setHarmonization(Boolean harmonization) {
        this.harmonization = harmonization;
    }

    public boolean verify(Applicant newApplicant) {
        return applicantRepository.exists(newApplicant);
    }
}
