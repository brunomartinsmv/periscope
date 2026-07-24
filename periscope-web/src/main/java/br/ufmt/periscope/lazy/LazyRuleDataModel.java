package br.ufmt.periscope.lazy;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.InventorRepository;
import br.ufmt.periscope.repository.RuleRepository;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

@Named
public class LazyRuleDataModel extends LazyDataModel<Rule>{
    
    private @Inject
    RuleRepository ruleRepository;
    private List<Rule> rules;
    private Integer searchType;
    private @Inject
    ApplicantRepository applicantRepository;
    private @Inject
    InventorRepository inventorRepository;

    
    @Override
    public int count(Map<String, FilterMeta> filterBy) {
        return getRowCount();
    }

    @Override
    public int getRowCount() {
        return ruleRepository.getRowCount();
    }

    @Override
    public String getRowKey(Rule object) {
        return object.getId() == null ? null : object.getId().toString();
    }

    @Override
    public Rule getRowData(String rowkey) {
        for (Rule rule : rules) {
            if (rule.getId().toString().equals(rowkey)) {
                return rule;
            }
        }
        return null;
    }
    
    
    @Override
    public List<Rule> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
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

        
        ruleRepository.setSearchType(this.searchType);
        
        List<Rule> regras;
        rules = ruleRepository.load(first, pageSize, sortField, sortOrd, filters);
        regras = new ArrayList<Rule>();
        if (this.searchType == 1) {
            for (Rule rule : rules) {
                rule.setAppSugestions(loadApplicantSugestions(rule));
                regras.add(rule);
            }    
        }else{
            for (Rule rule : rules) {
                rule.setInvSugestions(loadInventorSugestions(rule));
                regras.add(rule);
            }
        }
        return regras;
    }
    
    public List<Applicant> loadApplicantSugestions(Rule rule) {
        String[] names = new String[rule.getSubstitutions().size()];
        int i = 0;
        for (Iterator<String> it = rule.getSubstitutions().iterator(); it.hasNext();) {
            names[i] = it.next();
            i++;
        }
        Set<String> sugestions = applicantRepository.getApplicantSugestions(ruleRepository.getCurrentProject(), 100, names);
        ArrayList<Applicant> aplicants = new ArrayList<Applicant>();
        for (String sugestion : sugestions) {
            aplicants.add(new Applicant(sugestion));
        }
        rule.setAppSugestions(aplicants);
        return aplicants;
    }
    
    public List<Inventor> loadInventorSugestions(Rule rule) {
        String[] names = new String[rule.getSubstitutions().size()];
        int i = 0;
        for (Iterator<String> it = rule.getSubstitutions().iterator(); it.hasNext();) {
            names[i] = it.next();
            i++;
        }
        Set<String> sugestions = inventorRepository.getInventorSugestions(ruleRepository.getCurrentProject(), 100, names);
        ArrayList<Inventor> inventors = new ArrayList<Inventor>();
        for (String sugestion : sugestions) {
            inventors.add(new Inventor(sugestion));
        }
        rule.setInvSugestions(inventors);
        return inventors;
    }

    public Integer getSearchType() {
        return searchType;
    }

    public void setSearchType(Integer searchType) {
        this.searchType = searchType;
    }

    public RuleRepository getRuleRepository() {
        return ruleRepository;
    }

    public void setRuleRepository(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
    
    
}
