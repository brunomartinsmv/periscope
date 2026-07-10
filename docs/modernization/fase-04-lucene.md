# Fase 4 — Migração Apache Lucene

**PR:** `cursor/fase-4-lucene-8905`  
**Depende de:** Fase 2  
**Bloqueia:** Fase 5 (parcial)  
**Paralelo com:** Fase 3

## Objetivo

Atualizar **Lucene 6.0.0 → 9.12.0**, preservando o algoritmo **Fast-Join** (WANG et al., 2011) usado na harmonização de depositantes e inventores.

---

## Contexto crítico

A harmonização é funcionalidade core. O código customizado em `periscope-ejb/.../indexer/` implementa:

- Analyzers proprietários (`FastJoinAnalyzer`, `DataSignaturesAnalyzer`, `QuerySignaturesAnalyzer`)
- Token filters (`CommonDescriptorsTokenFilter`, `CondenseTokenFilter`, etc.)
- Query customizada (`FastJoinQuery`, `FastJoinTermEnum`)
- Busca fuzzy (`FuzzyTokenSimilaritySearch`)

**Qualquer regressão aqui invalida a modernização.**

---

## Stack alvo

```xml
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-core</artifactId>
    <version>9.12.0</version>
</dependency>
<!-- + lucene-analyzers-common, lucene-queryparser, lucene-queries -->
```

---

## Tarefas

### 4.1 Breaking changes Lucene 6 → 9

| Lucene 6 | Lucene 9 | Arquivos afetados |
|----------|----------|-------------------|
| `org.apache.lucene.util.Version` | **Removido** | Todos os analyzers |
| `new StopFilter(version, stream, set)` | `new StopFilter(stream, set)` | `FastJoinAnalyzer`, `DataSignaturesAnalyzer`, `QuerySignaturesAnalyzer` |
| `new LowerCaseFilter(version, source)` | `new LowerCaseFilter(source)` | Idem |
| `FilteredTermsEnum.AcceptStatus.NO_AND_SEEK` | **Removido** — usar `NO` | `FastJoinTermEnum.java` |
| `CorruptIndexException` | Consolidado em `IOException` | `PatentIndexer`, `LuceneIndexerResources` |
| `IndexWriter(dir, config)` | Similar; validar construtor | `LuceneIndexerResources` |
| `MultiTermQuery` construtor | Validar API | `FastJoinQuery` |
| `TopTermsBoostOnlyBooleanQueryRewrite` | Verificar pacote | `FastJoinQuery` |
| `BytesRef.utf8ToString()` | Deprecated — usar `new String(bytes, off, len, UTF_8)` | `FastJoinTermEnum` |

### 4.2 Atualizar Analyzers

**Arquivos:**

```
indexer/resources/analysis/
├── FastJoinAnalyzer.java          ← remover Version, ajustar construtores de filters
├── DataSignaturesAnalyzer.java
├── QuerySignaturesAnalyzer.java
├── CommonDescriptorsTokenFilter.java
├── CommonDescriptorsSet.java
├── CondenseTokenFilter.java
├── DataSignaturesTokenFilter.java
└── QuerySignaturesTokenFilter.java
```

**Exemplo FastJoinAnalyzer:**

```java
// Antes
StopFilter stopFilter = new StopFilter(aSCIIFoldingFilter, StandardAnalyzer.STOP_WORDS_SET);

// Depois — sem Version
StopFilter stopFilter = new StopFilter(aSCIIFoldingFilter, StandardAnalyzer.STOP_WORDS_SET);
```

Remover campo `matchVersion` e imports de `Version`.

### 4.3 Atualizar FastJoinTermEnum

**Problema:** `NO_AND_SEEK` permitia pular termos eficientemente.

```java
// Antes
return AcceptStatus.NO_AND_SEEK;

// Depois
return AcceptStatus.NO;
// Comportamento: menos eficiente, funcionalmente equivalente para correção
```

Se performance degradar significativamente, considerar reimplementar com `AutomatonTermsEnum` (otimização futura).

### 4.4 Atualizar LuceneIndexerResources

```java
// FSDirectory.open — OK com Path (já usa toPath())
dir = FSDirectory.open(new File(SeedBean.PERISCOPE_DIR).toPath());

// IndexWriterConfig — remover referências a Version
IndexWriterConfig config = new IndexWriterConfig(dataSignaturesAnalyzer)
    .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

// DirectoryReader.open — OK
reader = DirectoryReader.open(dir);

// Remover catch CorruptIndexException — capturar IOException
```

**Remover:** import `javax.faces.bean.ViewScoped` → `@ApplicationScoped` CDI (corrigir escopo).

### 4.5 Atualizar PatentIndexer e busca

**Arquivos:**

```
indexer/
├── PatentIndexer.java
├── resources/search/
│   ├── FastJoinQuery.java
│   ├── FastJoinTermEnum.java
│   ├── FuzzyTokenSimilarity.java
│   ├── FuzzyTokenSimilaritySearch.java
│   └── DamerauLevenshteinAlgorithm.java
```

**FuzzyTokenSimilaritySearch** — validar:
- `BooleanQuery` API (max clause count)
- `IndexSearcher` constructor
- `ScoreDoc` iteration

### 4.6 Diretório de índice

`SeedBean.PERISCOPE_DIR` — configurável via env:

| Variável | Default Linux | Default Windows |
|----------|---------------|-----------------|
| `PERISCOPE_DIR` | `/opt/periscope` | `C:\ProgramData\Periscope` |

Garantir que índices Lucene 6 **não são compatíveis** com Lucene 9 — reindexar na primeira execução pós-migração.

**Ação:** adicionar lógica de reindex ou documentar limpeza de `PERISCOPE_DIR/lucene/` no deploy.

### 4.7 Testes de regressão Fast-Join

**Criar:** `periscope-ejb/src/test/java/.../FastJoinRegressionTest.java`

Casos mínimos:

| Entrada A | Entrada B | Esperado |
|-----------|-----------|----------|
| "IBM CORP" | "INTERNATIONAL BUSINESS MACHINES" | Match fuzzy |
| "UNIV FEDERAL MATO GROSSO" | "UFMT" | Match por sigla |
| "EMPRESA XYZ LTDA" | "EMPRESA ABC LTDA" | No match |

Usar dataset de descritores comuns do `descriptors.yaml`.

---

## Arquivos afetados (resumo)

| Arquivo | Tipo de mudança |
|---------|-----------------|
| 3 Analyzers | Remover Version, ajustar filters |
| 5 TokenFilters | Validar API TokenStream |
| FastJoinQuery/TermEnum | NO_AND_SEEK, MultiTermQuery |
| FuzzyTokenSimilaritySearch | BooleanQuery, IndexSearcher |
| LuceneIndexerResources | IOException, escopo CDI |
| PatentIndexer | Field API, Term API |
| SeedBean | Reindex na startup se necessário |
| pom.xml | Lucene 9.12.0 |

## Critérios de aceite

- [ ] Lucene 9.12.0 no classpath; zero referências a `org.apache.lucene.util.Version`
- [ ] Índice criado e consultado sem erro
- [ ] Harmonização de depositantes retorna sugestões corretas
- [ ] Harmonização de inventores retorna sugestões corretas
- [ ] Testes de regressão Fast-Join passam
- [ ] Reindex documentado/automatizado na primeira execução

## Riscos

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| Fast-Join deixa de funcionar | Crítico | Testes de regressão + dataset conhecido |
| Performance degradada (NO vs NO_AND_SEEK) | Médio | Benchmark; otimizar depois |
| Índice corrompido na migração | Alto | Limpar e reindexar |

## Validação local

- [ ] Executar harmonização com projeto de teste
- [ ] Comparar sugestões antes/depois com mesmo dataset
- [ ] Verificar tempo de resposta aceitável
