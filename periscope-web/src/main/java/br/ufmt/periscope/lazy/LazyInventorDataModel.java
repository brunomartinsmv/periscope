package br.ufmt.periscope.lazy;

import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.repository.InventorRepository;
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
public class LazyInventorDataModel extends LazyDataModel<Inventor> {

    private @Inject
    InventorRepository inventorRepository;
    private List<Inventor> datasource;
    private List<Inventor> selectedInventors;
    private Integer searchType;
    private Boolean harmonization = false;

    public LazyInventorDataModel() {
        System.out.println("Construtor");
    }
    
    

    @Override
    public int count(Map<String, FilterMeta> filterBy) {
        return getRowCount();
    }

    @Override
    public int getRowCount() {
        return super.getRowCount(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Inventor getRowData(String key) {
        for (Inventor inventor : datasource) {
            if (inventor.getName().equals(key)) {
                return inventor;
            }
        }
        return null;
    }

    @Override
    public String getRowKey(Inventor object) {
        return object.getName();
    }

    @Override
    public List<Inventor> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
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

        long inicio = System.currentTimeMillis();
        inventorRepository.setSearchType(searchType);
        

        if (harmonization) {
            datasource = inventorRepository.load(first, pageSize, sortField, sortOrd, filters);
        } else {
            datasource = inventorRepository.load(first, pageSize, sortField, sortOrd, filters, this.selectedInventors);
        }
        if (this.selectedInventors == null) {
            System.out.println("null");
            this.selectedInventors = new ArrayList<Inventor>();
        }
        setRowCount(inventorRepository.getCount());
        for (Inventor inventor : datasource) {
            if (this.selectedInventors != null && this.selectedInventors.contains(inventor)) {
                inventor.setSelected(true);
            }
        }
        try {
            return datasource;
        } finally {
//            System.out.println("Tempo de Load Inventor: " + (System.currentTimeMillis() - inicio) + " millis");
        }
    }

    public InventorRepository getInventorRepository() {
        return inventorRepository;
    }

    public void setInventorRepository(InventorRepository inventorRepository) {
        this.inventorRepository = inventorRepository;
    }

    public List<Inventor> getSelectedInventors() {
        return selectedInventors;
    }

    public void setSelectedInventors(List<Inventor> selectedInventors) {
        this.selectedInventors = selectedInventors;
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

    public boolean verify(Inventor newInventor) {
        return inventorRepository.exists(newInventor);
    }
}
