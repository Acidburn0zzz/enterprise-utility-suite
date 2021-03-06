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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/

package com.blackducksoftware.tools.appuseradjuster.appidentifiersperuser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.tools.appuseradjuster.AppUserAdjusterConfig;
import com.blackducksoftware.tools.appuseradjuster.AppUserAdjusterType;
import com.blackducksoftware.tools.appuseradjuster.UserAdjustmentReport;
import com.blackducksoftware.tools.appuseradjuster.add.lobuseradjust.SimpleUserSet;
import com.blackducksoftware.tools.appuseradjuster.add.lobuseradjust.applist.AppListProcessor;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.codecenter.application.ApplicationPojo;
import com.blackducksoftware.tools.connector.codecenter.user.UserStatus;

public class AppListProcessorAppIdentifiersPerUser implements AppListProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass()
            .getName());

    private final AppUserAdjusterConfig config;

    private ICodeCenterServerWrapper codeCenterServerWrapper;

    private final AppIdentifierUserListMap appIdentifierUserListMap;

    private final AppUserAdjuster appUserAdjuster;

    public AppListProcessorAppIdentifiersPerUser(
            ICodeCenterServerWrapper codeCenterServerWrapper,
            AppUserAdjusterConfig config,
            AppIdentifierUserListMap appIdentifierUserListMap,
            AppUserAdjuster appUserAdjuster) {
        this.codeCenterServerWrapper = codeCenterServerWrapper;
        this.config = config;
        this.appIdentifierUserListMap = appIdentifierUserListMap;
        this.appUserAdjuster = appUserAdjuster;
    }

    /**
     * Load the applications to be processed from Code Center.
     *
     * @return
     * @throws CommonFrameworkException
     * @throws Exception
     */
    @Override
    public List<ApplicationPojo> loadApplications() throws CommonFrameworkException {

        List<ApplicationPojo> apps = new ArrayList<>();

        for (String appIdentifier : appIdentifierUserListMap) {

            ApplicationPageFilter filter = new ApplicationPageFilter();
            filter.setFirstRowIndex(0);
            filter.setLastRowIndex(Integer.MAX_VALUE);
            String startsWithString = appIdentifier
                    + config.getAppNameSeparator();
            List<ApplicationPojo> appIdentifierApps = codeCenterServerWrapper.getApplicationManager().getApplications(0, Integer.MAX_VALUE, startsWithString);

            logger.info("Loaded " + appIdentifierApps.size()
                    + " apps for AppIdentifier: " + appIdentifier);
            AppListFilter appListFilter = new AppListFilter(config,
                    appIdentifierApps, appIdentifier);
            List<ApplicationPojo> appIdentifierFilteredApps = appListFilter
                    .getFilteredList();
            logger.info("Filtered that down to "
                    + appIdentifierFilteredApps.size()
                    + " apps to process for AppIdentifier: " + appIdentifier);
            AppIdentifierAddUserDetails details = appIdentifierUserListMap
                    .getAppIdentifierUsernameListMap().get(appIdentifier);
            details.setApplications(appIdentifierFilteredApps); // Record the
            // apps for
            // this
            // AppIdentifier
            apps.addAll(appIdentifierFilteredApps);
        }

        return apps;
    }

    /**
     * Add users to applications for the given list of applications.
     *
     * @param appList
     * @param newUsers
     * @param report
     * @throws Exception
     */
    @Override
    public void processAppList(List<ApplicationPojo> apps, SimpleUserSet newUsers,
            UserAdjustmentReport report) throws Exception {

        boolean circumventLocks = config.isCircumventLocks();

        int matchingAppCount = 0;
        for (String appIdentifier : appIdentifierUserListMap) {
            logger.info("Processing AppIdentifier: " + appIdentifier);

            AppIdentifierAddUserDetails details = appIdentifierUserListMap
                    .getAppIdentifierUsernameListMap().get(appIdentifier);
            List<String> userNamesList = details.getUsernames();
            Set<String> userSet = new HashSet<String>(userNamesList);

            for (ApplicationPojo app : details.getApplications()) {
                logger.info("Potentially processing app " + app.getName()
                        + " / " + app.getVersion());
                if (!apps.contains(app)) {
                    logger.info("Skipping app " + app.getName()
                            + "; it is not assigned to this thread.");
                    continue;
                }

                logger.info("Processing app " + app.getName() + " / "
                        + app.getVersion());
                matchingAppCount++;
                String appId = app.getId();
                String appName = app.getName();
                String appVersion = app.getVersion();
                AppUserAdjusterType adjusterType;

                List<UserStatus> results = appUserAdjuster.adjustAppUsers(appId, userSet, circumventLocks);

                adjusterType = appUserAdjuster.getType(); // what type of adjuster (add or remove users) were we passed?
                if (adjusterType == AppUserAdjusterType.ADD) {
                    report.addRecord(appName, appVersion, true, null,
                            userNamesList, null, null, null);
                } else { // Remove
                    report.addRecord(appName, appVersion, true, null,
                            null, results, null, null);
                }
            }
        }
        if (matchingAppCount == 0) {
            report.addRecord("<all>", "", false, null, null, null, null,
                    "No applications match the AppIdentifiers specified in the input file");
        }
    }

}
