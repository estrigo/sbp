package kz.spt.api.plugin;

public interface CustomPlugin {

    Boolean hasTemplates();

    String getMenuLabel();

    String getMenuUrl();

    String getMenuCssClass();

    String getRole();
}
