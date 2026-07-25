package br.ufmt.periscope.importer;

import br.ufmt.periscope.importer.impl.DPMAPatentImporter;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.repository.CountryRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * DPMA importer expects a semicolon-separated text export (not Excel).
 * Fixture format matches {@link DPMAPatentImporter} field layout.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DPMAPatentImporterTest {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private DPMAPatentImporter importer;

    private final Country de = country("DE", "Germany");

    @BeforeEach
    void stubCountries() {
        when(countryRepository.getCountryByAcronym(anyString())).thenAnswer(inv -> {
            String ac = inv.getArgument(0);
            if (ac != null && ac.toUpperCase().startsWith("DE")) {
                return de;
            }
            return de;
        });
    }

    @Test
    void importsPatentsFromMinimalCsvLikeExport() throws Exception {
        // Line 1: non-Title marker (consumed by init)
        // Line 2: header (skipped)
        // Line 3+: data — fields split on "; " (semicolon+space)
        // Title field: first 5 chars are stripped as a DPMA prefix
        String content = ""
                + "DPMA export v1\n"
                + "PubNo; AppDate; PubDate; IPC; Inventors; Applicants; Title\n"
                + "DE102020000001; 15.01.2020; 10.06.2021; A01B1/00; Schmidt Hans, DE; ACME GmbH, DE; XXXXXSolar Tracking Mount\n"
                + "DE102021000002; 01.03.2021; 20.09.2022; H02S20/10; Mueller Anna, DE; UFMT Partner GmbH, DE; XXXXXBiofuel Catalyst Bed\n";

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        assertThat(importer.initWithStream(new ByteArrayInputStream(bytes))).isTrue();

        List<Patent> patents = new ArrayList<>();
        while (importer.hasNext()) {
            patents.add(importer.next());
        }

        assertThat(patents).hasSize(2);

        Patent first = patents.get(0);
        assertThat(first.getTitleSelect()).isEqualTo("SOLAR TRACKING MOUNT");
        assertThat(first.getPublicationNumber()).isEqualTo("DE102020000001 20210610");
        assertThat(first.getPublicationDate())
                .isEqualTo(new SimpleDateFormat("dd.MM.yyyy").parse("10.06.2021"));
        assertThat(first.getApplicationDate())
                .isEqualTo(new SimpleDateFormat("dd.MM.yyyy").parse("15.01.2020"));
        assertThat(first.getClassifications()).isNotEmpty();
        assertThat(first.getMainClassification().getValue()).contains("A01B1/00");
        assertThat(first.getInventors()).extracting(i -> i.getName().trim())
                .contains("SCHMIDT HANS");
        assertThat(first.getApplicants()).extracting(a -> a.getName().trim())
                .contains("ACME GMBH");

        Patent second = patents.get(1);
        assertThat(second.getTitleSelect()).isEqualTo("BIOFUEL CATALYST BED");
        assertThat(second.getPublicationNumber()).startsWith("DE102021000002");
        assertThat(second.getApplicants()).extracting(a -> a.getName().trim())
                .contains("UFMT PARTNER GMBH");
    }

    @Test
    void rejectsTitlePrefixedEspacenetStyleCsv() {
        String content = "\"Title\",\"Publication number\"\n\"Something\",\"DE1\"\n";
        assertThat(importer.initWithStream(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))).isFalse();
    }

    @Test
    void providerIsDpma() {
        assertThat(importer.provider()).isEqualTo("DPMA");
    }

    private static Country country(String acronym, String name) {
        Country c = new Country();
        c.setAcronym(acronym);
        c.setName(name);
        return c;
    }
}
