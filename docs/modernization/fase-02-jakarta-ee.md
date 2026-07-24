# Fase 2 — Migração Jakarta EE

**PR:** `cursor/fase-2-jakarta-ee-8905`  
**Depende de:** Fase 1  
**Bloqueia:** Fases 3, 4, 5, 6, 7

## Objetivo

Migrar de **Java EE 6 (`javax.*`)** para **Jakarta EE 10 (`jakarta.*`)**, compatível com **WildFly 31+** e **PrimeFaces 14**.

---

## Tarefas

### 2.1 Substituição de imports (automática + revisão manual)

Executar substituição em todos os `.java`:

| De | Para |
|----|------|
| `javax.faces.` | `jakarta.faces.` |
| `javax.servlet.` | `jakarta.servlet.` |
| `javax.enterprise.` | `jakarta.enterprise.` |
| `javax.inject.` | `jakarta.inject.` |
| `javax.annotation.` | `jakarta.annotation.` |
| `javax.ejb.` | `jakarta.ejb.` |
| `javax.ws.rs.` | `jakarta.ws.rs.` |
| `javax.validation.` | `jakarta.validation.` |

**Cuidado:** não substituir `javax.swing`, `javax.crypto`, `javax.net`, etc.

**Comando sugerido:**

```bash
find . -name "*.java" -exec sed -i \
  -e 's/javax\.faces\./jakarta.faces./g' \
  -e 's/javax\.servlet\./jakarta.servlet./g' \
  -e 's/javax\.enterprise\./jakarta.enterprise./g' \
  -e 's/javax\.inject\./jakarta.inject./g' \
  -e 's/javax\.annotation\./jakarta.annotation./g' \
  -e 's/javax\.ejb\./jakarta.ejb./g' \
  -e 's/javax\.ws\.rs\./jakarta.ws.rs./g' \
  -e 's/javax\.validation\./jakarta.validation./g' \
  {} +
```

### 2.2 Migrar JSF Managed Beans → CDI

`javax.faces.bean.*` foi **removido** no Jakarta Faces 4.

| Legado | Moderno | Arquivos (~25) |
|--------|---------|----------------|
| `@ManagedBean` | `@Named` (jakarta.inject) | Controllers em `periscope-web/.../controller/` |
| `javax.faces.bean.ViewScoped` | `jakarta.faces.view.ViewScoped` | Controllers, repositórios com `@ViewScoped` |
| `javax.faces.bean.ApplicationScoped` | `jakarta.enterprise.context.ApplicationScoped` | `SeedBean` |
| `javax.faces.bean.SessionScoped` | `jakarta.enterprise.context.SessionScoped` | `LanguageBean` |
| `javax.faces.bean.RequestScoped` | `jakarta.enterprise.context.RequestScoped` | `SeedBean` (se usado) |

**Exemplo:**

```java
// Antes
@ManagedBean
@ViewScoped
public class PatentController { ... }

// Depois
@Named
@ViewScoped
public class PatentController { ... }
```

**Arquivos com `@ManagedBean` / `javax.faces.bean`:**

- `PatentController`, `ImportPatentController`, `ProjectController`, `UserController`
- Todos em `controller/harmonization/`
- Todos os controllers de relatório (`*Controller.java` em `periscope-web`)
- `LanguageBean`, `SidebarController`
- `ApplicantRepository`, `InventorRepository`, `LuceneIndexerResources` (escopo incorreto — revisar se `@ViewScoped` faz sentido em EJB/repositório)

### 2.3 Atualizar descriptors XML

#### `web.xml`

```xml
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         version="6.0">
    <!-- javax.faces.* → jakarta.faces.* nos context-param -->
    <context-param>
        <param-name>jakarta.faces.STATE_SAVING_METHOD</param-name>
        ...
    </context-param>
    <servlet-class>jakarta.faces.webapp.FacesServlet</servlet-class>
</web-app>
```

#### `faces-config.xml`

```xml
<faces-config version="4.0"
    xmlns="https://jakarta.ee/xml/ns/jakartaee"
    ...>
```

#### `beans.xml` (ambos módulos)

```xml
<beans version="4.0"
    xmlns="https://jakarta.ee/xml/ns/jakartaee"
    bean-discovery-mode="all">
</beans>
```

#### `jboss-web.xml`

Atualizar namespace se necessário; validar `context-root` `/periscope`.

### 2.4 PrimeFaces 14 (Jakarta)

**Dependência:**

```xml
<dependency>
    <groupId>org.primefaces</groupId>
    <artifactId>primefaces</artifactId>
    <version>${primefaces.version}</version>
    <classifier>jakarta</classifier>
</dependency>
```

**Mover PrimeFaces** de `periscope-ejb` para `periscope-web` (UI não pertence ao EJB).

### 2.5 Views XHTML

PrimeFaces 14 / Faces 4 — verificar namespaces nos templates:

```xml
xmlns="http://www.w3.org/1999/xhtml"
xmlns:h="jakarta.faces.html"
xmlns:f="jakarta.faces.core"
xmlns:ui="jakarta.faces.facelets"
xmlns:p="http://primefaces.org/ui"
```

**Arquivos:** ~70 XHTML em `periscope-web/src/main/webapp/`.

### 2.6 JAX-RS Activator

`JaxRsActivator.java` — `@ApplicationPath` com imports jakarta. Preparação para Fase 7 (SPA).

### 2.7 Compilação

```bash
mvn clean compile -DskipTests
```

Corrigir erros restantes de API (PrimeFaces upload/download fica para Fase 5).

---

## Arquivos afetados (resumo)

| Categoria | Quantidade | Local |
|-----------|------------|-------|
| Java (imports + beans) | ~70 | `periscope-web`, `periscope-ejb` |
| XHTML | ~70 | `periscope-web/src/main/webapp/` |
| XML config | 5 | `WEB-INF/`, `META-INF/` |
| POMs | 2 | parent, web, ejb |

## Critérios de aceite

- [x] Zero imports `javax.faces`, `javax.servlet`, `javax.enterprise`, `javax.ejb`
- [x] Zero `@ManagedBean` — todos usam `@Named`
- [x] `web.xml`, `faces-config.xml`, `beans.xml` com namespace Jakarta EE 10
- [x] XHTML com namespaces jakarta.faces
- [x] `mvn clean compile` passa (exceto erros de Mongo/Lucene/PDFBox — fases 3–5)
- [x] PrimeFaces 14 jakarta no classpath do módulo web

## Riscos

| Risco | Mitigação |
|-------|-----------|
| `@ViewScoped` em repositórios EJB | Remover escopo JSF; usar `@ApplicationScoped` ou `@RequestScoped` CDI |
| CDI discovery | `beans.xml` com `bean-discovery-mode="all"` |
| Navegação JSF implícita quebrada | Testar fluxos do `faces-config.xml` localmente |

## Validação local (sem homologação)

Checklist mínimo pós-Fase 2:

- [ ] Deploy no WildFly local (Fase 6 antecipada se necessário)
- [ ] Login funciona
- [ ] Lista de projetos carrega
- [ ] Navegação entre telas sem erro 500
