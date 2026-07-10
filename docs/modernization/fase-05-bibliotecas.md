# Fase 5 — Bibliotecas Auxiliares

**PR:** `cursor/fase-5-bibliotecas-8905`  
**Depende de:** Fases 2, 3, 4  
**Bloqueia:** Fases 6, 7

## Objetivo

Atualizar **PDFBox**, **Apache POI**, **PrimeFaces upload/download** e demais bibliotecas auxiliares para versões compatíveis com Java 21 e Jakarta EE 10.

---

## Tarefas

### 5.1 Apache PDFBox 1.8 → 3.x

**Arquivo:** `periscope-ejb/src/main/java/br/ufmt/periscope/util/PDFTextParser.java`

**Antes (PDFBox 1.x):**

```java
import org.apache.pdfbox.util.PDFTextStripper;
parser = new PDFParser(new FileInputStream(file));
parser.parse();
cosDoc = parser.getDocument();
pdDoc = new PDDocument(cosDoc);
parsedText = pdfStripper.getText(pdDoc);
```

**Depois (PDFBox 3.x):**

```java
import org.apache.pdfbox.text.PDFTextStripper;
try (PDDocument pdDoc = Loader.loadPDF(file)) {
    PDFTextStripper stripper = new PDFTextStripper();
    return stripper.getText(pdDoc);
}
```

**Dependências:**

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

**Remover:** `fontbox`, `jempbox` separados (inclusos no pdfbox 3.x).

### 5.2 Apache POI 3.10-beta2 → 5.x

**Arquivos (importadores de patentes):**

```
periscope-ejb/src/main/java/br/ufmt/periscope/importer/
├── impl/ESPACENETPatentImporter.java
├── impl/PATENTSCOPEPatentImporter.java
├── impl/DPMAPatentImporter.java
└── decorator/PatentValidator.java
```

**Mudanças comuns POI 3 → 5:**

| POI 3 | POI 5 |
|-------|-------|
| `HSSFWorkbook` / `XSSFWorkbook` | API similar; validar construtores |
| `Cell.CELL_TYPE_*` | `CellType.*` enum |
| `getCellType()` retorna int | Retorna `CellType` |

**Dependência:**

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.3.0</version>
</dependency>
```

### 5.3 PrimeFaces 14 — Upload/Download

**Arquivos:** `PatentController.java`, `ImportPatentController.java`

**UploadedFile — pacote mudou:**

```java
// Antes
import org.primefaces.model.UploadedFile;

// Depois
import org.primefaces.model.file.UploadedFile;
```

**DefaultStreamedContent — builder pattern:**

```java
// Antes
new DefaultStreamedContent(stream, contentType, name)

// Depois
DefaultStreamedContent.builder()
    .stream(() -> stream)
    .contentType(contentType)
    .name(name)
    .build()
```

**FileUploadFilter** — verificar se ainda necessário no `web.xml` ou se servlet 6 trata nativamente.

### 5.4 Commons FileUpload

`commons-fileupload:1.3` — **remover** se PrimeFaces 14 + Servlet 6 gerenciam multipart nativamente.

### 5.5 Commons IO

Garantir versão única `commons-io:2.16.1` (corrigido na Fase 1).

### 5.6 Google Guava

`ApplicantRepository` usa `HashMultiset` do Guava — adicionar dependência explícita se transitiva não resolver:

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.3.0-jre</version>
</dependency>
```

### 5.7 Compilação e testes de importação

```bash
mvn clean package -DskipTests
```

Testar manualmente importação de arquivos:
- ESPACENET (Excel/XML)
- PATENTSCOPE
- DPMA

---

## Arquivos afetados

| Biblioteca | Arquivos |
|------------|----------|
| PDFBox | `PDFTextParser.java` |
| POI | 3 importadores + validator |
| PrimeFaces | `PatentController`, `ImportPatentController` |
| Guava | `ApplicantRepository` (dependência) |
| POMs | `periscope-ejb/pom.xml`, `periscope-web/pom.xml` |

## Critérios de aceite

- [ ] PDFBox 3.x — extração de texto de PDF funciona
- [ ] POI 5.x — importação de patentes de todos os formatos suportados
- [ ] PrimeFaces 14 — upload e download de arquivos funciona
- [ ] `mvn clean package` passa sem erros
- [ ] Zero dependências com CVE crítico conhecido (verificar via `mvn dependency-check:check` — Fase 8)

## Riscos

| Risco | Mitigação |
|-------|-----------|
| Formato de importação ESPACENET mudou | Manter arquivos de teste reais |
| StreamedContent builder exige Supplier | Garantir que stream não é consumido antes do download |

## Validação local

- [ ] Importar arquivo de patente de cada fonte
- [ ] Fazer upload de PDF em patente existente
- [ ] Download de arquivo anexado
