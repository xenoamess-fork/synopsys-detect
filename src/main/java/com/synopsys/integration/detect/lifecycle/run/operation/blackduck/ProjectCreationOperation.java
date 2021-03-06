/**
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.detect.lifecycle.run.operation.blackduck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.lifecycle.run.RunOptions;
import com.synopsys.integration.detect.workflow.blackduck.DetectCodeLocationUnmapService;
import com.synopsys.integration.detect.workflow.blackduck.DetectCustomFieldService;
import com.synopsys.integration.detect.workflow.blackduck.DetectProjectService;
import com.synopsys.integration.detect.workflow.blackduck.DetectProjectServiceOptions;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.NameVersion;

public class ProjectCreationOperation {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RunOptions runOptions;
    private final DetectProjectServiceOptions detectProjectServiceOptions;
    private final DetectCustomFieldService detectCustomFieldService;

    public ProjectCreationOperation(RunOptions runOptions, DetectProjectServiceOptions detectProjectServiceOptions,
        DetectCustomFieldService detectCustomFieldService) {
        this.runOptions = runOptions;
        this.detectProjectServiceOptions = detectProjectServiceOptions;
        this.detectCustomFieldService = detectCustomFieldService;
    }

    public ProjectVersionWrapper execute(BlackDuckServicesFactory blackDuckServicesFactory, NameVersion projectNameVersion) throws DetectUserFriendlyException, IntegrationException {
        DetectProjectService detectProjectService = new DetectProjectService(blackDuckServicesFactory.getBlackDuckApiClient(), blackDuckServicesFactory.createProjectService(),
            blackDuckServicesFactory.createProjectBomService(), blackDuckServicesFactory.createProjectUsersService(), blackDuckServicesFactory.createTagService(), detectProjectServiceOptions,
            blackDuckServicesFactory.createProjectMappingService(), detectCustomFieldService);
        DetectCodeLocationUnmapService detectCodeLocationUnmapService = new DetectCodeLocationUnmapService(blackDuckServicesFactory.getBlackDuckApiClient(), blackDuckServicesFactory.createCodeLocationService());

        ProjectVersionWrapper projectVersionWrapper = detectProjectService.createOrUpdateBlackDuckProject(projectNameVersion);
        if (null != projectVersionWrapper && runOptions.shouldUnmapCodeLocations()) {
            logger.debug("Unmapping code locations.");
            detectCodeLocationUnmapService.unmapCodeLocations(projectVersionWrapper.getProjectVersionView());
        } else {
            logger.debug("Will not unmap code locations: Project view was not present, or should not unmap code locations.");
        }
        return projectVersionWrapper;
    }
}
