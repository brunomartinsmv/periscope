package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.MainIPCReport;
import br.ufmt.periscope.report.Pair;
import java.util.ArrayList;
import java.util.Collections;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.tagcloud.DefaultTagCloudModel;
import org.primefaces.model.tagcloud.TagCloudModel;

/**
 * Tag-cloud report for main IPC classifications.
 */
@Named
@ViewScoped
public class MainClassificationController extends GenericController {

    private @Inject
    MainIPCReport report;

    private TagCloudModel tagCloudModel = new DefaultTagCloudModel();

    @Override
    public void refreshChart() {
        ChartSeries series = report.ipcCount(
                getCurrentProject(), false, false, false, false,
                getLimit() <= 0 ? 20 : Math.max(getLimit(), 20),
                getFiltro(), 1);
        tagCloudModel = ChartModels.tagCloud(series);
        setChartEmpty(ChartModels.isEmpty(series));

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }
        Collections.reverse(getPairs());
    }

    public TagCloudModel getTagCloudModel() {
        return tagCloudModel != null ? tagCloudModel : new DefaultTagCloudModel();
    }

    public void setTagCloudModel(TagCloudModel tagCloudModel) {
        this.tagCloudModel = tagCloudModel != null ? tagCloudModel : new DefaultTagCloudModel();
    }
}
