package br.ufmt.periscope.model;

import dev.morphia.annotations.Embedded;
import java.io.Serializable;

@Embedded
public class History implements Serializable{
    
    private String name;
    private Country country;

    public History() {
        this.country = new Country();
        this.country.setStates(null);
        this.country.setName("");
        this.country.setAcronym("");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return "History{" + "name=" + name + ", country=" + country + '}';
    }
    
}
