/*
 * Copyright (c) UChicago Argonne, LLC. All rights reserved.
 * See LICENSE file.
 */
package gov.anl.aps.logr.portal.controllers;

import gov.anl.aps.logr.portal.model.db.beans.SystemPropertyFacade;
import gov.anl.aps.logr.portal.model.db.entities.SystemProperty;
import gov.anl.aps.logr.portal.utilities.SessionUtility;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Named("systemPropertyController")
@ApplicationScoped
public class SystemPropertyController implements Serializable {

    public static final String PERMALINK_BASE_URL_KEY = "permalink.baseUrl";

    private static final Logger logger = LogManager.getLogger(SystemPropertyController.class.getName());

    @EJB
    private SystemPropertyFacade systemPropertyFacade;

    private String editPermalinkBaseUrl;

    @PostConstruct
    public void init() {
        editPermalinkBaseUrl = getPermalinkBaseUrl();
    }

    public static SystemPropertyController getInstance() {
        return (SystemPropertyController) SessionUtility.findBean("systemPropertyController");
    }

    public String getPermalinkBaseUrl() {
        SystemProperty prop = systemPropertyFacade.findByName(PERMALINK_BASE_URL_KEY);
        if (prop != null && prop.getValue() != null) {
            return prop.getValue();
        }
        return "";
    }

    public String getEditPermalinkBaseUrl() {
        return editPermalinkBaseUrl;
    }

    public void setEditPermalinkBaseUrl(String editPermalinkBaseUrl) {
        this.editPermalinkBaseUrl = editPermalinkBaseUrl;
    }

    public void prepareEdit() {
        editPermalinkBaseUrl = getPermalinkBaseUrl();
    }

    public void save() {
        String url = editPermalinkBaseUrl != null ? editPermalinkBaseUrl.trim() : "";
        SystemProperty prop = systemPropertyFacade.findByName(PERMALINK_BASE_URL_KEY);
        if (prop == null) {
            prop = new SystemProperty(PERMALINK_BASE_URL_KEY, url);
            prop.setDescription("Base URL used to construct log entry permalinks (e.g. https://bely.example.com/bely)");
            systemPropertyFacade.create(prop);
        } else {
            prop.setValue(url);
            systemPropertyFacade.edit(prop);
        }
        logger.info("Permalink base URL updated to: {}", url);
        SessionUtility.addInfoMessage("Saved", "Permalink base URL updated.");
    }

    public String getPermalinkForLog(Integer logId) {
        if (logId == null) {
            return "";
        }
        String base = getPermalinkBaseUrl();
        if (base == null || base.isEmpty()) {
            return "";
        }
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return trimmed + "/views/log/view.xhtml?id=" + logId;
    }

    public boolean isPermalinkConfigured() {
        String base = getPermalinkBaseUrl();
        return base != null && !base.isEmpty();
    }

}
