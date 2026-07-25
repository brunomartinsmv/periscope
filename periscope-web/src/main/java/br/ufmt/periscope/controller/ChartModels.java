package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.ChartSeries;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.primefaces.model.tagcloud.DefaultTagCloudItem;
import org.primefaces.model.tagcloud.DefaultTagCloudModel;
import org.primefaces.model.tagcloud.TagCloudModel;

/**
 * Builds PrimeFaces 14 / Chart.js models from report {@link ChartSeries}.
 */
public final class ChartModels {

    private static final String BAR_BG = "rgba(33, 150, 243, 0.65)";
    private static final String BAR_BORDER = "rgb(25, 118, 210)";
    private static final String LINE_BORDER = "rgb(25, 118, 210)";
    private static final String LINE_BG = "rgba(33, 150, 243, 0.15)";

    private ChartModels() {
    }

    public static BarChartModel bar(ChartSeries series, String title, boolean horizontal) {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();
        BarChartDataSet dataSet = new BarChartDataSet();
        dataSet.setLabel(labelOf(series));
        dataSet.setBackgroundColor(BAR_BG);
        dataSet.setBorderColor(BAR_BORDER);
        dataSet.setBorderWidth(1);

        List<Object> values = new ArrayList<Object>();
        List<String> labels = new ArrayList<String>();
        fill(series, labels, values);
        dataSet.setData(values);
        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        model.setData(data);

        BarChartOptions options = new BarChartOptions();
        if (horizontal) {
            options.setIndexAxis("y");
        }
        options.setTitle(titleOption(title));
        options.setLegend(legend("bottom"));
        options.setMaintainAspectRatio(false);
        model.setOptions(options);
        return model;
    }

    public static LineChartModel line(ChartSeries series, String title) {
        LineChartModel model = new LineChartModel();
        ChartData data = new ChartData();
        LineChartDataSet dataSet = new LineChartDataSet();
        dataSet.setLabel(labelOf(series));
        dataSet.setBorderColor(LINE_BORDER);
        dataSet.setBackgroundColor(LINE_BG);
        dataSet.setFill(true);
        dataSet.setTension(0.2);
        dataSet.setBorderWidth(2);

        List<Object> values = new ArrayList<Object>();
        List<String> labels = new ArrayList<String>();
        fill(series, labels, values);
        dataSet.setData(values);
        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        model.setData(data);

        LineChartOptions options = new LineChartOptions();
        options.setTitle(titleOption(title));
        options.setLegend(legend("bottom"));
        options.setMaintainAspectRatio(false);
        model.setOptions(options);
        return model;
    }

    public static TagCloudModel tagCloud(ChartSeries series) {
        DefaultTagCloudModel model = new DefaultTagCloudModel();
        if (series == null || series.isEmpty()) {
            return model;
        }
        int max = 1;
        for (Number value : series.getData().values()) {
            if (value != null && value.intValue() > max) {
                max = value.intValue();
            }
        }
        for (Map.Entry<Object, Number> entry : series.getData().entrySet()) {
            int count = entry.getValue() == null ? 0 : entry.getValue().intValue();
            int strength = Math.max(1, Math.min(5, (int) Math.ceil(count * 5.0 / max)));
            model.addTag(new DefaultTagCloudItem(String.valueOf(entry.getKey()), strength));
        }
        return model;
    }

    public static boolean isEmpty(ChartSeries series) {
        return series == null || series.isEmpty();
    }

    private static void fill(ChartSeries series, List<String> labels, List<Object> values) {
        if (series == null) {
            return;
        }
        for (Map.Entry<Object, Number> entry : series.getData().entrySet()) {
            labels.add(String.valueOf(entry.getKey()));
            values.add(entry.getValue());
        }
    }

    private static String labelOf(ChartSeries series) {
        if (series == null || series.getLabel() == null) {
            return "";
        }
        return series.getLabel();
    }

    private static Title titleOption(String title) {
        Title t = new Title();
        if (title != null && !title.isEmpty()) {
            t.setDisplay(true);
            t.setText(title);
        } else {
            t.setDisplay(false);
        }
        return t;
    }

    private static Legend legend(String position) {
        Legend legend = new Legend();
        legend.setDisplay(true);
        legend.setPosition(position);
        return legend;
    }
}
