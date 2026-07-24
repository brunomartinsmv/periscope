package br.ufmt.periscope.repository;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import br.ufmt.periscope.model.Country;

import com.github.jmkgreen.morphia.Datastore;

@Named
public class CountryRepository {

    @Inject
    private Datastore ds;

    public List<Country> getAll() {
        return ds.createQuery(Country.class).order("name").asList();
    }

    public Country getCountryByAcronym(String acronym) {
        return ds.createQuery(Country.class).field("acronym").equal(acronym).get();
    }
}
