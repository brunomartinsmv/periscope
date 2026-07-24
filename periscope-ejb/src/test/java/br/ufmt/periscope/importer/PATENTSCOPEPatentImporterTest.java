package br.ufmt.periscope.importer;

import br.ufmt.periscope.importer.impl.PATENTSCOPEPatentImporter;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.repository.CountryRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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
 * Parses a minimal Patentscope-style XLS (blank + query rows, then data).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PATENTSCOPEPatentImporterTest {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private PATENTSCOPEPatentImporter importer;

    private final Country wo = country("WO", "WIPO");
    private final Country br = country("BR", "Brazil");

    @BeforeEach
    void stubCountries() {
        when(countryRepository.getCountryByAcronym(anyString())).thenAnswer(inv -> {
            String ac = inv.getArgument(0);
            if ("WO".equalsIgnoreCase(ac)) {
                return wo;
            }
            if ("BR".equalsIgnoreCase(ac)) {
                return br;
            }
            return null;
        });
    }

    @Test
    void importsPatentsFromMinimalXls() throws Exception {
        byte[] xls = buildPatentscopeXls();
        assertThat(importer.initWithStream(new ByteArrayInputStream(xls))).isTrue();

        List<Patent> patents = collect(importer);
        assertThat(patents).hasSize(2);

        Patent first = patents.get(0);
        assertThat(first.getPublicationNumber()).isEqualTo("WO2021000123");
        assertThat(first.getPublicationCountry().getAcronym()).isEqualTo("WO");
        assertThat(first.getPublicationDate())
                .isEqualTo(new SimpleDateFormat("dd.MM.yyyy").parse("15.03.2021"));
        assertThat(first.getTitleSelect()).isEqualTo("WATER PURIFICATION DEVICE");
        assertThat(first.getAbstractSelect()).contains("filtration membrane");
        assertThat(first.getClassifications()).extracting(c -> c.getValue())
                .containsExactly("C02F1/44", "B01D61/14");
        assertThat(first.getApplicants()).extracting(a -> a.getName())
                .containsExactly("CLEAN WATER SA", "UFMT");
        assertThat(first.getInventors()).extracting(i -> i.getName())
                .containsExactly("ALMEIDA PEDRO", "COSTA ANA");

        Patent second = patents.get(1);
        assertThat(second.getPublicationNumber()).isEqualTo("BR112021000999");
        assertThat(second.getTitleSelect()).isEqualTo("AGRICULTURAL DRONE");
        assertThat(second.getApplicants()).extracting(a -> a.getName())
                .containsExactly("AGRO TECH LTDA");
    }

    @Test
    void providerIsPatentscope() {
        assertThat(importer.provider()).isEqualTo("PATENTSCOPE");
    }

    private static byte[] buildPatentscopeXls() throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            HSSFSheet sheet = wb.createSheet("Results");
            sheet.createRow(0); // blank
            sheet.createRow(1).createCell(0).setCellValue("Consulta IPC=C02F");

            HSSFRow r1 = sheet.createRow(2);
            r1.createCell(0).setCellValue("WO2021000123");
            r1.createCell(1).setCellValue("15.03.2021");
            r1.createCell(2).setCellValue("Water Purification Device");
            r1.createCell(3).setCellValue("A portable filtration membrane unit");
            r1.createCell(4).setCellValue("C02F1/44; B01D61/14");
            r1.createCell(5).setCellValue("CLEAN WATER SA; UFMT");
            r1.createCell(6).setCellValue("ALMEIDA PEDRO; COSTA ANA");

            HSSFRow r2 = sheet.createRow(3);
            r2.createCell(0).setCellValue("BR112021000999");
            r2.createCell(1).setCellValue("01.07.2021");
            r2.createCell(2).setCellValue("Agricultural Drone");
            r2.createCell(3).setCellValue("UAV for crop spraying");
            r2.createCell(4).setCellValue("B64C39/02");
            r2.createCell(5).setCellValue("AGRO TECH LTDA");
            r2.createCell(6).setCellValue("SANTOS RUI");

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static List<Patent> collect(PatentImporter importer) {
        List<Patent> patents = new ArrayList<>();
        while (importer.hasNext()) {
            patents.add(importer.next());
        }
        return patents;
    }

    private static Country country(String acronym, String name) {
        Country c = new Country();
        c.setAcronym(acronym);
        c.setName(name);
        return c;
    }
}
