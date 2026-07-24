package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Collections;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.MainIPCReport;
import br.ufmt.periscope.report.Pair;
import jakarta.faces.event.ValueChangeEvent;

/**
 * Controller responsável por operações de visualização relacionadas à classificação IPC.
 */
@Named
@ViewScoped
public class MainIPCController extends GenericController {

    private @Inject
    MainIPCReport report;
    private boolean klass;
    private boolean subKlass;
    private boolean group;
    private boolean subGroup;
    private boolean description;
    private int classification;

    @PostConstruct
    @Override
    public void init() {
        klass = false;
        subKlass = false;
        group = false;
        subGroup = false;
        this.setClassification(1);
        setLimit(8);
        super.init();
    }

    public void update() {
        if (!klass) {
            subKlass = false;
            group = false;
            subGroup = false;
        } else if (!subKlass) {
            group = false;
            subGroup = false;
        } else if (!group) {
            subGroup = false;
        }

        refreshChart();
    }

    @Override
    public void refreshChart() {
        ChartSeries series = report.ipcCount(getCurrentProject(), klass, subKlass,
                group, subGroup, getLimit(), getFiltro(), this.getClassification());
        applyBarChart(series, series.getLabel(), true);

        setPairs(new ArrayList<Pair>());

        this.description = false;
        String descriptionText;
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            String ipc = (String) key;
            descriptionText = null;
            if (ipc != null && ipc.length() == 1) {
                this.description = true;
                switch (ipc.charAt(0)) {
                    case 'A':
                        descriptionText = "Necessidades Humanas";
                        break;
                    case 'B':
                        descriptionText = "Operações de Processamento; Transporte";
                        break;
                    case 'C':
                        descriptionText = "Química e Metalurgia";
                        break;
                    case 'D':
                        descriptionText = "Têxteis e Papel";
                        break;
                    case 'F':
                        descriptionText = "Engenharia Mecânica; Iluminação; Aquecimento; Arma";
                        break;
                    case 'E':
                        descriptionText = "Construções Fixas";
                        break;
                    case 'G':
                        descriptionText = "Física";
                        break;
                    case 'H':
                        descriptionText = "Eletricidade";
                        break;
                    case 'Y':
                        descriptionText = "";
                        break;
                    default:
                        break;
                }
            }
            getPairs().add(new Pair(key, value, descriptionText));
        }

        Collections.reverse(getPairs());
    }

    public boolean isKlass() {
        return klass;
    }

    public void classificationListener(ValueChangeEvent event) {
        int newVal = (Integer) event.getNewValue();
        this.setClassification(newVal);
    }

    public void setKlass(boolean klass) {
        this.klass = klass;
    }

    public boolean isSubKlass() {
        return subKlass;
    }

    public void setSubKlass(boolean subKlass) {
        this.subKlass = subKlass;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public boolean isSubGroup() {
        return subGroup;
    }

    public void setSubGroup(boolean subGroup) {
        this.subGroup = subGroup;
    }

    public boolean isDescription() {
        return description;
    }

    public void setDescription(boolean description) {
        this.description = description;
    }

    public int getClassification() {
        return classification;
    }

    public void setClassification(int classification) {
        this.classification = classification;
    }
}
