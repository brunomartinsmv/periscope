package br.ufmt.periscope.importer.decorator;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import br.ufmt.periscope.importer.PatentImporter;
import br.ufmt.periscope.model.Patent;

@Decorator
public abstract class ValidatePatentDecorator implements PatentImporter {

    @Inject
    @Delegate
    @Any
    private PatentImporter patentImporter;
    @Inject
    private PatentValidator validator;

    @Override
    public Patent next() {
        Patent patent = patentImporter.next();
        if (patent == null) {
            return null;
        }
        validator.validate(patent);
        return patent;
    }
}
