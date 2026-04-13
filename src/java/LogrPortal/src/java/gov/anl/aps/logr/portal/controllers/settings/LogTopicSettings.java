/*
 * Copyright (c) UChicago Argonne, LLC. All rights reserved.
 * See LICENSE file.
 */
package gov.anl.aps.logr.portal.controllers.settings;

import gov.anl.aps.logr.portal.controllers.LogTopicController;
import gov.anl.aps.logr.portal.model.db.entities.SettingEntity;
import gov.anl.aps.logr.portal.model.db.entities.SettingType;
import java.util.Map;

/**
 *
 * @author djarosz
 */
public class LogTopicSettings extends CdbEntitySettingsBase<LogTopicController> {

    public LogTopicSettings(LogTopicController parentController) {
        super(parentController);
    }

    @Override
    protected void updateSettingsFromSettingTypeDefaults(Map<String, SettingType> settingTypeMap) {
    }

    @Override
    protected void updateSettingsFromSessionSettingEntity(SettingEntity settingEntity) {
    }

    @Override
    protected void saveSettingsForSessionSettingEntity(SettingEntity settingEntity) {
    }

    @Override
    protected void settingsAreReloaded() {
    }

}
