/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.tools.appadjuster;

import java.util.Map;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;

/**
 * Sets values of custom attributes on an application. The custom attribute
 * names and their values are specified by the given configuration.
 *
 * @author sbillings
 *
 */
public class AppAdjuster {
    private AppAdjusterConfigManager config;

    public AppAdjuster(AppAdjusterConfigManager config) {
	this.config = config;
    }

    /**
     * Makes the custom attribute value changes on the given app.
     */
    public void adjustApp(String appName) throws CommonFrameworkException,
	    AppNotFoundException {

	CcApp app = new CcApp(config, appName, config.getAppVersion());

	// Set a custom attribute on the app
	Map<String, String> attrValues = config.getAttrValues();
	for (String attrName : attrValues.keySet()) {
	    attrValues.put(attrName, attrValues.get(attrName));
	}
	app.setCustomAttributes(attrValues);
    }
}
