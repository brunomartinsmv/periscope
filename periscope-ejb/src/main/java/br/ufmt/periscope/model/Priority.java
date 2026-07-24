package br.ufmt.periscope.model;

import java.io.Serializable;

import java.util.Date;

import dev.morphia.annotations.Embedded;

@Embedded
public class Priority implements Serializable {

    private static final long serialVersionUID = 1L;

    private String value;
    private Country country;
    private Date date;

    public Priority() {
        country = new Country();
    }

    public String getValue() {
        if (value != null) {
            value = value.toUpperCase();
        }
        return value;
    }

    public void setValue(String value) {
        if (value != null) {
            value = value.toUpperCase();
        }
        this.value = value;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return value;
    }
}
