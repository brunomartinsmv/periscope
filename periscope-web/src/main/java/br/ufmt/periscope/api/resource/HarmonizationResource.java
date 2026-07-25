package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.RuleDTO;
import br.ufmt.periscope.api.dto.RuleRequest;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.harmonization.Harmonization;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.RuleType;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.repository.ApplicantTypeRepository;
import br.ufmt.periscope.repository.CountryRepository;
import br.ufmt.periscope.repository.InventorRepository;
import br.ufmt.periscope.repository.RuleRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/projects/{projectId}/harmonization")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
public class HarmonizationResource {

    @Inject
    private ApiSecuritySupport securitySupport;

    @Inject
    private ApplicantRepository applicantRepository;

    @Inject
    private InventorRepository inventorRepository;

    @Inject
    private RuleRepository ruleRepository;

    @Inject
    private Harmonization harmonization;

    @Inject
    private CountryRepository countryRepository;

    @Inject
    private ApplicantTypeRepository applicantTypeRepository;

    @GET
    @Path("/suggestions")
    public Map<String, Object> suggestions(
            @PathParam("projectId") String projectId,
            @QueryParam("type") @DefaultValue("applicant") String type,
            @QueryParam("query") String query,
            @QueryParam("top") @DefaultValue("50") int top,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        if (query == null || query.isBlank()) {
            throw ApiException.badRequest("query is required");
        }
        if (top <= 0 || top > 500) {
            top = 50;
        }
        Set<String> suggestions;
        String kind = type.trim().toLowerCase();
        if ("inventor".equals(kind)) {
            suggestions = inventorRepository.getInventorSugestions(project, top, query.trim());
        } else if ("applicant".equals(kind)) {
            suggestions = applicantRepository.getApplicantSugestions(project, top, query.trim());
        } else {
            throw ApiException.badRequest("type must be applicant or inventor");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", kind);
        body.put("query", query.trim());
        body.put("suggestions", new ArrayList<>(suggestions));
        return body;
    }

    @GET
    @Path("/rules")
    public List<RuleDTO> listRules(
            @PathParam("projectId") String projectId,
            @QueryParam("type") String type,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        List<Rule> rules;
        if (type == null || type.isBlank()) {
            rules = ruleRepository.getAllRule(project);
        } else if ("applicant".equalsIgnoreCase(type)) {
            rules = ruleRepository.getApplicantRule(project);
        } else if ("inventor".equalsIgnoreCase(type)) {
            rules = ruleRepository.getInventorRule(project);
        } else {
            throw ApiException.badRequest("type must be applicant or inventor");
        }
        return rules.stream().map(RuleDTO::from).toList();
    }

    @POST
    @Path("/rules")
    public Response createRule(
            @PathParam("projectId") String projectId,
            RuleRequest request,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw ApiException.badRequest("name is required");
        }
        if (request.type() == null) {
            throw ApiException.badRequest("type is required (APPLICANT or INVENTOR)");
        }
        RuleType ruleType;
        try {
            ruleType = RuleType.valueOf(request.type().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("type must be APPLICANT or INVENTOR");
        }
        Rule rule = new Rule();
        rule.setName(request.name());
        rule.setAcronym(request.acronym());
        rule.setType(ruleType);
        rule.setProject(project);
        if (request.substitutions() != null) {
            rule.setSubstitutions(new HashSet<>(request.substitutions()));
        }
        if (request.countryAcronym() != null && !request.countryAcronym().isBlank()) {
            Country country = countryRepository.getCountryByAcronym(request.countryAcronym());
            if (country != null) {
                rule.setCountry(country);
            }
        }
        if (request.nature() != null && !request.nature().isBlank()) {
            for (ApplicantType at : applicantTypeRepository.getAll()) {
                if (request.nature().equalsIgnoreCase(at.getName())) {
                    rule.setNature(at);
                    break;
                }
            }
        }
        ruleRepository.setCurrentProject(project);
        ruleRepository.save(rule);
        return Response.status(Response.Status.CREATED).entity(RuleDTO.from(rule)).build();
    }

    @DELETE
    @Path("/rules/{ruleId}")
    public Response deleteRule(
            @PathParam("projectId") String projectId,
            @PathParam("ruleId") String ruleId,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        Rule rule = ruleRepository.findById(ruleId);
        if (rule == null || rule.getProject() == null
                || !rule.getProject().getId().equals(project.getId())) {
            throw ApiException.notFound("Rule not found");
        }
        ruleRepository.delete(ruleId);
        return Response.noContent().build();
    }

    @POST
    @Path("/apply")
    public Map<String, Object> applyAll(
            @PathParam("projectId") String projectId,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        List<Rule> rules = ruleRepository.getAllRule(project);
        int applied = 0;
        for (Rule rule : rules) {
            rule.setProject(project);
            harmonization.applyRule(rule);
            applied++;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applied", applied);
        body.put("message", "Rules applied successfully");
        return body;
    }

    @POST
    @Path("/rules/{ruleId}/apply")
    public Map<String, Object> applyOne(
            @PathParam("projectId") String projectId,
            @PathParam("ruleId") String ruleId,
            @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(projectId, user);
        Rule rule = ruleRepository.findById(ruleId);
        if (rule == null || rule.getProject() == null
                || !rule.getProject().getId().equals(project.getId())) {
            throw ApiException.notFound("Rule not found");
        }
        rule.setProject(project);
        harmonization.applyRule(rule);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applied", 1);
        body.put("ruleId", ruleId);
        body.put("message", "Rule applied successfully");
        return body;
    }
}
