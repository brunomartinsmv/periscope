package br.ufmt.periscope.util;

import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.indexer.resources.analysis.CommonDescriptor;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers classpath YAML seed loading used by {@code SeedBean}.
 */
class YamlLoaderTest {

    @Test
    void loadsCountriesFromSeedYaml() {
        List<Country> countries = YamlLoader.loadList("country-inicial-data.yaml", Country.class);

        assertThat(countries).hasSize(248);
        assertThat(countries)
                .anySatisfy(c -> {
                    assertThat(c.getAcronym()).isEqualTo("BR");
                    assertThat(c.getName()).isEqualToIgnoringCase("Brasil");
                })
                .anySatisfy(c -> assertThat(c.getAcronym()).isEqualTo("US"));
    }

    @Test
    void loadsInitialAdminUser() {
        List<User> users = YamlLoader.loadList("user-inicial.yaml", User.class);

        assertThat(users).hasSize(1);
        User admin = users.get(0);
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getPassword()).isEqualTo("123456");
        assertThat(admin.getUserLevel()).isEqualTo(UserLevel.ADMIN);
        assertThat(admin.getEmail()).isEqualTo("admin@gmail.com");
    }

    @Test
    void loadsApplicantTypesAndDescriptors() {
        List<ApplicantType> types = YamlLoader.loadList("applicantType-inicial.yaml", ApplicantType.class);
        List<CommonDescriptor> descriptors = YamlLoader.loadList("descriptors.yaml", CommonDescriptor.class);

        assertThat(types).isNotEmpty();
        assertThat(descriptors).isNotEmpty();
        assertThat(descriptors).allSatisfy(d -> assertThat(d.getWord()).isNotBlank());
    }

    @Test
    void missingResourceThrows() {
        assertThatThrownBy(() -> YamlLoader.loadList("does-not-exist.yaml", Country.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does-not-exist.yaml");
    }
}
