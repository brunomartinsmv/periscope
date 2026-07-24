package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.dto.ReportDTO;
import br.ufmt.periscope.api.dto.ReportItemDTO;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.compat.chart.ChartSeries;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.report.ApplicationDateReport;
import br.ufmt.periscope.report.MainApplicantReport;
import br.ufmt.periscope.report.MainIPCReport;
import br.ufmt.periscope.report.MainInventorReport;
import br.ufmt.periscope.report.PublicationDateReport;
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.util.Filters;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/projects/{projectId}/reports")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
@Tag(name = "Reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportResource {

    @Inject
    private ApiSecuritySupport securitySupport;

    @Inject
    private PatentRepository patentRepository;

    @Inject
    private MainApplicantReport mainApplicantReport;

    @Inject
    private MainInventorReport mainInventorReport;

    @Inject
    private MainIPCReport mainIPCReport;

    @Inject
    private ApplicationDateReport applicationDateReport;

    @Inject
    private PublicationDateReport publicationDateReport;

    @GET
    @Path("/main-applicant")
    @Operation(summary = "Principais depositantes")
    public ReportDTO mainApplicant(
            @PathParam("projectId") String projectId,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        ChartSeries series = mainApplicantReport.mainApplicantSeries(project, clamp(limit), filtersFor(project, 1));
        return toReport("main-applicant", series);
    }

    @GET
    @Path("/main-inventor")
    @Operation(summary = "Principais inventores")
    public ReportDTO mainInventor(
            @PathParam("projectId") String projectId,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        ChartSeries series = mainInventorReport.InventorDateSeries(project, clamp(limit), filtersFor(project, 1));
        return toReport("main-inventor", series);
    }

    @GET
    @Path("/main-ipc")
    @Operation(summary = "Principais IPCs")
    public ReportDTO mainIpc(
            @PathParam("projectId") String projectId,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        ChartSeries series = mainIPCReport.ipcCount(
                project, true, false, false, false, clamp(limit), filtersFor(project, 1), 1);
        return toReport("main-ipc", series);
    }

    @GET
    @Path("/application-date")
    @Operation(summary = "Relatório por data de depósito")
    public ReportDTO applicationDate(
            @PathParam("projectId") String projectId,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        ChartSeries series = applicationDateReport.applicationDateSeries(project, filtersFor(project, 2));
        return toReport("application-date", series);
    }

    @GET
    @Path("/publication-date")
    @Operation(summary = "Relatório por data de publicação")
    public ReportDTO publicationDate(
            @PathParam("projectId") String projectId,
            @Context SecurityContext securityContext) {
        Project project = loadProject(projectId, securityContext);
        ChartSeries series = publicationDateReport.publicationDateSeries(project, filtersFor(project, 1));
        return toReport("publication-date", series);
    }

    private Project loadProject(String projectId, SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        return securitySupport.requireProject(projectId, user);
    }

    /** Mirrors GenericController date window. selecionaData: 1=publication, 2=application. */
    private Filters filtersFor(Project project, int selecionaData) {
        Filters f = new Filters();
        f.setComplete(false);
        f.setSelecionaData(selecionaData);
        Date min = patentRepository.getMinDate(project, selecionaData);
        Date max = patentRepository.getMaxDate(project, selecionaData);
        if (min == null) {
            min = new Date(0L);
        }
        if (max == null) {
            max = new Date();
        }
        f.setInicio(min);
        f.setFim(max);
        return f;
    }

    private static int clamp(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 100);
    }

    private static ReportDTO toReport(String name, ChartSeries series) {
        List<ReportItemDTO> items = new ArrayList<>();
        if (series != null && series.getData() != null) {
            for (Map.Entry<Object, Number> e : series.getData().entrySet()) {
                items.add(new ReportItemDTO(String.valueOf(e.getKey()), e.getValue()));
            }
        }
        String label = series != null ? series.getLabel() : name;
        return new ReportDTO(name, label, items);
    }
}
