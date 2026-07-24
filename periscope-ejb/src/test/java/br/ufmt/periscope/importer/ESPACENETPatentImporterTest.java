package br.ufmt.periscope.importer;

import br.ufmt.periscope.importer.impl.ESPACENETPatentImporter;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.repository.CountryRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * Parses a minimal Espacenet-style XLS (5 preamble rows + header + data).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ESPACENETPatentImporterTest {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private ESPACENETPatentImporter importer;

    private final Country br = country("BR", "Brazil");
    private final Country us = country("US", "United States");

    @BeforeEach
    void stubCountries() {
        when(countryRepository.getCountryByAcronym(anyString())).thenAnswer(inv -> {
            String ac = inv.getArgument(0);
            if ("BR".equalsIgnoreCase(ac)) {
                return br;
            }
            if ("US".equalsIgnoreCase(ac)) {
                return us;
            }
            return null;
        });
    }

    @Test
    void importsPatentsFromMinimalXls() throws Exception {
        byte[] xls = buildEspacenetXls();
        assertThat(importer.initWithStream(new ByteArrayInputStream(xls))).isTrue();

        List<Patent> patents = collect(importer);
        assertThat(patents).hasSize(2);

        Patent first = patents.get(0);
        assertThat(first.getTitleSelect()).isEqualTo("SOLAR PANEL MOUNTING SYSTEM");
        assertThat(first.getPublicationNumber()).isEqualTo("BR102020000001A2");
        assertThat(first.getPublicationDate())
                .isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2021-05-10"));
        assertThat(first.getApplicationNumber()).isEqualTo("BR2020000001");
        assertThat(first.getApplicationDate())
                .isEqualTo(new SimpleDateFormat("yyyyMMdd").parse("20200115"));
        assertThat(first.getInventors()).extracting(i -> i.getName().trim())
                .containsExactly("SILVA JOAO");
        assertThat(first.getApplicants()).extracting(a -> a.getName().trim())
                .containsExactly("UFMT");
        assertThat(first.getClassifications()).extracting(c -> c.getValue())
                .contains("H02S20/00");
        assertThat(first.getMainClassification().getValue()).isEqualTo("H02S20/00");
        assertThat(first.getCpcClassifications()).extracting(c -> c.getValue())
                .contains("Y02E10/50");

        Patent second = patents.get(1);
        assertThat(second.getTitleSelect()).isEqualTo("BIOFUEL REACTOR");
        assertThat(second.getPublicationNumber()).isEqualTo("US20210001234A1");
        assertThat(second.getApplicants()).extracting(a -> a.getName().trim())
                .contains("ACME CORP");
    }

    @Test
    void providerIsEspacenet() {
        assertThat(importer.provider()).isEqualTo("ESPACENET");
    }

    private static byte[] buildEspacenetXls() throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            HSSFSheet sheet = wb.createSheet("Results");
            // 5 preamble rows skipped by importer
            sheet.createRow(0).createCell(0).setCellValue("Espacenet logo");
            sheet.createRow(1).createCell(0).setCellValue("2 results");
            sheet.createRow(2).createCell(0).setCellValue("Search title");
            sheet.createRow(3).createCell(0).setCellValue("2 publications");
            HSSFRow header = sheet.createRow(4);
            header.createCell(0).setCellValue("Title");
            header.createCell(1).setCellValue("Publication number");
            header.createCell(2).setCellValue("Publication date");
            header.createCell(3).setCellValue("Inventors");
            header.createCell(4).setCellValue("Applicants");
            header.createCell(5).setCellValue("IPC");
            header.createCell(6).setCellValue("CPC");
            header.createCell(7).setCellValue("Application number");
            header.createCell(8).setCellValue("Application date");
            header.createCell(9).setCellValue("Priority");

            fillDataRow(sheet.createRow(5),
                    "Solar Panel Mounting System",
                    "BR102020000001A2",
                    "2021-05-10",
                    "SILVA JOAO [BR]",
                    "UFMT [BR]",
                    "H02S20/00",
                    "Y02E10/50",
                    "BR2020000001",
                    "20200115",
                    "BR2020000001 20200115");

            fillDataRow(sheet.createRow(6),
                    "Biofuel Reactor",
                    "US20210001234A1",
                    "2021-01-07",
                    "DOE JANE [US]",
                    "ACME CORP [US]",
                    "C12M1/00",
                    "Y02E50/10",
                    "US2020123456",
                    "20200601",
                    "US2020123456 20200601");

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void fillDataRow(HSSFRow row, String... values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
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
