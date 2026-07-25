package br.ufmt.periscope.api.dto;

import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import java.util.Set;

public record RuleDTO(
        String id,
        String name,
        String acronym,
        String type,
        Set<String> substitutions,
        String countryAcronym,
        String nature,
        String projectId
) {
    public static RuleDTO from(Rule rule) {
        if (rule == null) {
            return null;
        }
        RuleType type = rule.getType();
        return new RuleDTO(
                rule.getId() != null ? rule.getId().toString() : null,
                rule.getName(),
                rule.getAcronym(),
                type != null ? type.name() : null,
                rule.getSubstitutions(),
                rule.getCountry() != null ? rule.getCountry().getAcronym() : null,
                rule.getNature() != null ? rule.getNature().getName() : null,
                rule.getProject() != null && rule.getProject().getId() != null
                        ? rule.getProject().getId().toString() : null
        );
    }
}
