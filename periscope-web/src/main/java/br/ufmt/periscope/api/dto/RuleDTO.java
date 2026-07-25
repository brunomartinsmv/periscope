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
    /**
     * Maps a rule using an explicit project id — preferred for list endpoints
     * so Morphia never has to resolve {@code Rule.project} (@Reference) when
     * list projections omit that field.
     */
    public static RuleDTO from(Rule rule, String projectId) {
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
                projectId
        );
    }

    /**
     * Prefer {@link #from(Rule, String)} when the project id is already known
     * (list/create paths). This overload only reads {@code project} if a stub
     * with id was already set — it is not safe on a Morphia-lazy reference
     * while another cursor is open / when projections omit {@code project}.
     */
    public static RuleDTO from(Rule rule) {
        if (rule == null) {
            return null;
        }
        String projectId = null;
        if (rule.getProject() != null && rule.getProject().getId() != null) {
            projectId = rule.getProject().getId().toString();
        }
        return from(rule, projectId);
    }
}
