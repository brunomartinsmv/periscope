package br.ufmt.periscope.indexer.resources.analysis;

import java.io.Serializable;

import dev.morphia.Datastore;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * O conjunto de descritores comuns. Gerencia as buscas a base de dados de
 * descritores comuns.
 *
 */
@Named
public class CommonDescriptorsSet implements Serializable {

    private static final long serialVersionUID = 1L;

    private @Inject
    Datastore ds;

    /**
     * Construtor simples da classe
     */
    public CommonDescriptorsSet() {
    }

    /**
     * Verifica se a palavra é um descritor comum
     * @param descriptor a palavra a ser buscada
     * @return true se a palavra existe na base de dados, false caso contrário.
     */
    public boolean contains(String descriptor) {
        List<CommonDescriptor> descriptorSet = ds
                .find(CommonDescriptor.class)
                .filter(dev.morphia.query.filters.Filters.eq("_id", descriptor))
                .iterator().toList();

        if (descriptorSet != null && !descriptorSet.isEmpty()) {
            return true;
        }
        return false;
    }

}
