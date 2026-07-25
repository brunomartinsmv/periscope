# Dataset de teste

Conjunto de referência para validação local (importação, harmonização, relatórios).

## Conteúdo

| Arquivo | Formato | Importador | Origem |
|---------|---------|------------|--------|
| `dpma-sample.csv` | CSV `; ` (DPMA) | `DPMAPatentImporter` | commitado aqui e em `periscope-ejb/src/test/resources/importer/` |
| Espacenet XLS | `.xls` HSSF | `ESPACENETPatentImporter` | gerado em tempo de teste por `ESPACENETPatentImporterTest` (não versionar binário) |
| Patentscope XLS | `.xls` HSSF | `PATENTSCOPEPatentImporter` | gerado em tempo de teste por `PATENTSCOPEPatentImporterTest` |
| PDF de amostra | PDFBox 3 | `PDFTextParser` | gerado em tempo de teste por `PDFTextParserTest` |

Fixtures de importação usados pelos testes unitários também ficam sob:

`periscope-ejb/src/test/resources/importer/`

## Critérios do dataset

- Poucos registros (2–3 linhas) — rápido de importar
- Casos com nomes similares para Fast-Join (ver `FastJoinRegressionTest`)
- Ao menos um caso multi-formato (DPMA texto + Espacenet/Patentscope XLS)

## Uso manual

1. Subir MongoDB e WildFly com `periscope.war`
2. Login `admin` / `123456` (seed)
3. Criar projeto de teste
4. Importar `dpma-sample.csv` (provedor DPMA) ou gerar XLS via testes e reutilizar
5. Executar harmonização e um relatório fixo

## Relação com a Fase 8a

Os fixtures acima sustentam os testes unitários da Fase 8a. Expansão E2E / OpenAPI fica para a Fase 8b.
