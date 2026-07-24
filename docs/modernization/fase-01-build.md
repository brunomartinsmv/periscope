# Fase 1 — Build e Dependências

**PR:** `cursor/fase-1-build-8905`  
**Depende de:** Fase 0  
**Bloqueia:** Fases 2–8

## Objetivo

Fazer o projeto compilar com **Java 21** e **Maven moderno**, eliminando dependências irrecuperáveis (fixjures, repositórios HTTP).

---

## Tarefas

### 1.1 Atualizar POM pai (`pom.xml`)

```xml
<properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <!-- Versões centralizadas -->
    <jakartaee.version>10.0.0</jakartaee.version>
    <primefaces.version>14.0.0</primefaces.version>
    <morphia.version>2.4.14</morphia.version>
    <mongodb.version>5.2.1</mongodb.version>
    <lucene.version>9.12.0</lucene.version>
    <pdfbox.version>3.0.3</pdfbox.version>
    <poi.version>5.3.0</poi.version>
    <junit.version>5.11.0</junit.version>
</properties>
```

**Plugins a atualizar:**

| Plugin | Versão atual | Versão alvo |
|--------|--------------|-------------|
| maven-compiler-plugin | 2.3.2 | 3.13.0 |
| maven-war-plugin | 2.1.1 | 3.4.0 |
| maven-ejb-plugin | 2.3 | 3.2.1 |
| maven-ear-plugin | 2.6 | 3.3.0 (temporário, removido na Fase 6) |
| buildnumber-maven-plugin | 1.3 | 3.2.1 |

**Remover:** `jboss-javaee-web-6.0` BOM, `javaee-endorsed-api`, plugin `jboss-as-maven-plugin`.

**Adicionar:** `jakarta.jakartaee-bom:10.0.0` (scope import) — preparação para Fase 2.

### 1.2 Substituir fixjures por Jackson YAML

**Problema:** `com.bigfatgun:fixjures:2.0-SNAPSHOT` vem de repositório HTTP morto.

**Solução:** Jackson YAML + helper simples.

**Arquivo a criar:** `periscope-ejb/src/main/java/br/ufmt/periscope/util/YamlLoader.java`

```java
// Pseudocódigo — implementar na fase
public final class YamlLoader {
    public static <T> List<T> loadList(InputStream is, Class<T> type) { ... }
}
```

**Dependência:**

```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
    <version>2.18.0</version>
</dependency>
```

**Arquivo a alterar:** `SeedBean.java` — trocar chamadas `Fixjure.listOf(...).from(YamlSource...)` por `YamlLoader.loadList(...)`.

**Remover:** repositório `fixjures` do `periscope-ejb/pom.xml`.

### 1.3 Corrigir dependências quebradas

| Dependência | Problema | Correção |
|-------------|----------|----------|
| `org.apache.commons:commons-io:1.3.2` | groupId errado | `commons-io:commons-io:2.16.1` |
| `org.primefaces:primefaces:3.4.2` | repo HTTP | `14.0.0` via Maven Central (classifier `jakarta`) |
| Repositório `prime-repo` HTTP | bloqueado | Remover — PrimeFaces está no Central |

### 1.4 Atualizar `periscope-ejb/pom.xml`

- Remover `javaee-api:6.0` e `javaee-endorsed-api`
- Remover bloco `endorsed.dir` e `maven-dependency-plugin` de endorsed
- Centralizar versões via parent POM
- Manter dependências legadas temporariamente com versões compatíveis Java 21 até Fases 3–5

### 1.5 Atualizar `periscope-web/pom.xml`

- Corrigir `commons-io`
- PrimeFaces 14 (`classifier: jakarta`)
- Remover repositório HTTP

### 1.6 JUnit 4 → 5

Substituir `junit:junit:4.10` por `org.junit.jupiter:junit-jupiter`.

### 1.7 Primeiro build

```bash
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
mvn clean compile -DskipTests
```

**Expectativa:** compilação pode falhar em APIs `javax.*` e bibliotecas legadas — isso é resolvido nas Fases 2–5. Meta desta fase: POMs válidos e fixjures substituído.

---

## Arquivos afetados

| Arquivo | Ação |
|---------|------|
| `pom.xml` | Reescrever properties e pluginManagement |
| `periscope-ejb/pom.xml` | Dependências, remover repos HTTP |
| `periscope-web/pom.xml` | Dependências, remover repos HTTP |
| `periscope-ear/pom.xml` | Atualizar plugins (temporário) |
| `SeedBean.java` | Substituir fixjures |
| `YamlLoader.java` | Criar |

## Critérios de aceite

- [x] `mvn validate` passa em todos os módulos
- [x] Nenhum repositório HTTP nos POMs
- [x] fixjures removido; seed YAML carrega via Jackson
- [x] Java 21 configurado (`maven.compiler.release=21`)
- [x] JUnit 5 configurado
- [x] Versões centralizadas no POM pai

## Riscos

| Risco | Mitigação |
|-------|-----------|
| Enums em YAML (ex.: `UserLevel: ADMIN`) | Configurar Jackson para enums ou usar `@JsonProperty` |
| PrimeFaces jakarta vs javax | Classifier `jakarta` só após Fase 2 |

## Notas

- Nesta fase **não** migrar `javax` → `jakarta` ainda.
- Compilação completa é meta da Fase 2; aqui o foco é infraestrutura de build.
