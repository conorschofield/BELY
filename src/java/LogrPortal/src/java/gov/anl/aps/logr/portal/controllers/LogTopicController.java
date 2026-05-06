/*
 * Copyright (c) UChicago Argonne, LLC. All rights reserved.
 * See LICENSE file.
 */
package gov.anl.aps.logr.portal.controllers;

import gov.anl.aps.logr.portal.controllers.settings.LogTopicSettings;
import gov.anl.aps.logr.portal.controllers.utilities.LogTopicControllerUtility;
import gov.anl.aps.logr.portal.model.db.entities.LogTopic;
import gov.anl.aps.logr.portal.model.db.beans.LogTopicFacade;

import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Named("logTopicController")
@SessionScoped
public class LogTopicController extends CdbEntityController<LogTopicControllerUtility, LogTopic, LogTopicFacade, LogTopicSettings> implements Serializable {

    private static final Logger logger = LogManager.getLogger(LogTopicController.class.getName());

    @EJB
    private LogTopicFacade logTopicFacade;

    public LogTopicController() {
        super();
    }

    @Override
    protected LogTopicFacade getEntityDbFacade() {
        return logTopicFacade;
    }

    @Override
    public List<LogTopic> getAvailableItems() {
        return super.getAvailableItems();
    }

    @Override
    protected LogTopicSettings createNewSettingObject() {
        return new LogTopicSettings(this);
    }

    @Override
    protected LogTopicControllerUtility createControllerUtilityInstance() {
        return new LogTopicControllerUtility(); 
    }

    /**
     * JSF converter for LogTopic entities.
     *
     * Registered both as forClass (for single-value bindings JSF can type-infer)
     * AND under the explicit name "logTopicConverter" (for multi-value bindings
     * like selectCheckboxMenu/List, where generic erasure prevents JSF from
     * discovering the element type — those bindings must reference this
     * converter via converter="logTopicConverter").
     */
    @FacesConverter(value = "logTopicConverter", forClass = LogTopic.class)
    public static class LogTopicControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            // NOTE: This converter intentionally returns null on failure rather
            // than throwing ConverterException. Throwing here causes silent
            // form-validation rejection on save (no <p:messages> on the add-log
            // dialog to surface the error). Returning null is safe because
            // Log.setLogTopicList strips null elements defensively, so a failed
            // conversion at worst loses a category instead of corrupting the
            // persistence context with a null collection element. Any null
            // return is logged at WARN below for diagnosability.
            try {
                if (value == null || value.length() == 0) {
                    return null;
                }
                LogTopicController controller = (LogTopicController) facesContext.getApplication().getELResolver().
                        getValue(facesContext.getELContext(), null, "logTopicController");
                LogTopic resolved = controller.getEntity(getIntegerKey(value));
                if (resolved == null) {
                    logger.warn("No LogTopic found for id '{}' — dropping from selection", value);
                }
                return resolved;
            } catch (Exception ex) {
                logger.warn("Value '{}' cannot be converted to LogTopic — dropping from selection", value, ex);
                return null;
            }
        }

        Integer getIntegerKey(String value) {
            return Integer.valueOf(value);
        }

        String getStringKey(Integer value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof LogTopic) {
                LogTopic o = (LogTopic) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + LogTopic.class.getName());
            }
        }

    }

}
