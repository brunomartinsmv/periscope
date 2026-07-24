package br.ufmt.periscope.lazy;

import java.io.Serializable;

import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.repository.PatentRepository;
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
public class LazyPatentBrazilianDataModel extends LazyDataModel<Patent> implements Serializable{

    private static final long serialVersionUID = 1L;
    private @Inject PatentRepository repo;
    private List<Patent> datasource;

    @Override
    public int count(Map<String, FilterMeta> filterBy) {
        return getRowCount();
    }

    @Override
    public int getRowCount() {
        return repo.getRowCount();
    }

    @Override
    public String getRowKey(Patent object) {
        return object.getId() == null ? null : object.getId().toString();
    }

    @Override
    public Patent getRowData(String rowkey) {
        for (Patent patent : datasource) {
            if (patent.getId().toString().equals(rowkey)) {
                return patent;
            }
        }
        return null;
    }
    
    
    @Override
    public List<Patent> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
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

        datasource = repo.loadBrazilian(first, pageSize, sortField, sortOrd, filters);
        return datasource;
        
    }

    public PatentRepository getRepo() {
        return repo;
    }

    public void setRepo(PatentRepository repo) {
        this.repo = repo;
    }

    
}
