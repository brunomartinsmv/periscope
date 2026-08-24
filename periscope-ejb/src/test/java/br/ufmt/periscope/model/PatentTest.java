package br.ufmt.periscope.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Domain accessors normalise identifiers used in harmonisation.
 */
class PatentTest {

    @Test
    void titleSelectIsStoredAndReadUppercase() {
        Patent patent = new Patent();
        patent.setTitleSelect("Integration Test Patent");

        assertThat(patent.getTitleSelect()).isEqualTo("INTEGRATION TEST PATENT");
    }

    @Test
    void titleSelectGetterLeavesNullUnchanged() {
        Patent patent = new Patent();

        assertThat(patent.getTitleSelect()).isNull();
    }
}
