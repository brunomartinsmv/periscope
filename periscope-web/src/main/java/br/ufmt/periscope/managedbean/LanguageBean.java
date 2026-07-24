package br.ufmt.periscope.managedbean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ValueChangeEvent;

@Named("language")
@SessionScoped
public class LanguageBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String localeCode = "pt_BR";
    private String localString = "Português";

    private static Map<String, Locale> locales;

    static {

        locales = new HashMap<String, Locale>();
        locales.put("Português", new Locale("pt", "BR"));
        locales.put("English", new Locale("en", "US"));

    }

    public LanguageBean() {

        Locale locale = locales.get("Português");
        FacesContext.getCurrentInstance().getViewRoot().setLocale(locale);
        setLocaleCode(locale.toString());
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public Set<String> getLocales() {
        return locales.keySet();
    }

    public String getLocalString() {
        return localString;
    }

    public void setLocalString(String localString) {
        this.localString = localString;
    }
public void localeCodeChanged(ValueChangeEvent evt) {
        String novoLocale = evt.getNewValue().toString();
        Locale locale = locales.get(novoLocale);
        FacesContext.getCurrentInstance().getViewRoot().setLocale(locale);
        setLocaleCode(locale.toString());
        
        
    }
    
}