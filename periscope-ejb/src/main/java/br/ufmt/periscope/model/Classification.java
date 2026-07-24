package br.ufmt.periscope.model;

import java.io.Serializable;

import br.ufmt.periscope.enumerated.ClassificationType;

import dev.morphia.annotations.Embedded;

@Embedded
public class Classification implements Serializable {

    private static final long serialVersionUID = 1L;

    private String value;
    private String klass;
    private String group;
    private String subgroup;
    private ClassificationType type;

    public Classification() {
    }

    public Classification(String value, ClassificationType type) {
        this.value = value;
        this.setType(type);
        updateClassGroupSubGroup(value);
    }

    private void updateClassGroupSubGroup(String val) {
        String vet[] = null;
        if (val != null) {
            vet = val.trim().split("/");
            if (vet.length > 0) {
                if (vet[0].length() > 4) {
                    klass = vet[0].substring(0, 4);
                    group = vet[0].substring(4).trim();
                } else {
                    klass = vet[0];
                    group = "";
                }
                if (vet.length > 1) {
                    subgroup = vet[1].trim();
                } else {
                    subgroup = "";
                }
            } else {
                klass = "";
                group = "";
                subgroup = "";
            }
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (value != null) {
            value = value.toUpperCase();
        }
        this.value = value;
        updateClassGroupSubGroup(value);
    }

    public String getKlass() {
        return klass;
    }

    public String getGroup() {
        return group;
    }

    public String getSubgroup() {
        return subgroup;
    }

    public ClassificationType getType() {
        return type;
    }

    public void setType(ClassificationType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return value; //To change body of generated methods, choose Tools | Templates.
    }
}
