package br.ufmt.periscope.repository;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import br.ufmt.periscope.model.Country;

import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;

import static dev.morphia.query.filters.Filters.eq;

@ApplicationScoped
@Named
public class CountryRepository {

    @Inject
    private Datastore ds;

    public List<Country> getAll() {
        return ds.find(Country.class)
                .iterator(new FindOptions().sort(Sort.ascending("name")))
                .toList();
    }

    public Country getCountryByAcronym(String acronym) {
        return ds.find(Country.class)
                .filter(eq("acronym", acronym))
                .first();
    }
}
