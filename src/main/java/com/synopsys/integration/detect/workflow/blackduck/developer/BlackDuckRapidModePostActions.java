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
package com.synopsys.integration.detect.workflow.blackduck.developer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.api.manual.view.DeveloperScanComponentResultView;
import com.synopsys.integration.blackduck.api.manual.view.PolicyViolationLicenseView;
import com.synopsys.integration.blackduck.api.manual.view.PolicyViolationVulnerabilityView;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.ExitCodeType;
import com.synopsys.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.file.DetectFileUtils;
import com.synopsys.integration.detect.workflow.file.DirectoryManager;
import com.synopsys.integration.util.IntegrationEscapeUtil;
import com.synopsys.integration.util.NameVersion;

public class BlackDuckRapidModePostActions {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Gson gson;
    private final EventSystem eventSystem;
    private final DirectoryManager directoryManager;

    public BlackDuckRapidModePostActions(Gson gson, EventSystem eventSystem, DirectoryManager directoryManager) {
        this.gson = gson;
        this.eventSystem = eventSystem;
        this.directoryManager = directoryManager;
    }

    public void perform(NameVersion projectNameVersion, List<DeveloperScanComponentResultView> results) throws DetectUserFriendlyException {
        Set<String> violatedPolicyComponentNames = new LinkedHashSet<>();
        generateJSONScanOutput(projectNameVersion, results);
        for (DeveloperScanComponentResultView resultView : results) {
            String componentName = resultView.getComponentName();
            String componentVersion = resultView.getVersionName();
            Set<String> policyNames = new LinkedHashSet<>(resultView.getViolatingPolicyNames());
            Set<PolicyViolationVulnerabilityView> vulnerabilityViolations = resultView.getPolicyViolationVulnerabilities();
            Set<PolicyViolationLicenseView> licenseViolations = resultView.getPolicyViolationLicenses();
            Set<String> vulnerabilityPolicyNames = vulnerabilityViolations.stream()
                                                       .map(PolicyViolationVulnerabilityView::getViolatingPolicyNames)
                                                       .flatMap(Collection::stream)
                                                       .collect(Collectors.toSet());

            Set<String> licensePolicyNames = licenseViolations.stream()
                                                 .map(PolicyViolationLicenseView::getViolatingPolicyNames)
                                                 .flatMap(Collection::stream)
                                                 .collect(Collectors.toSet());
            policyNames.removeAll(vulnerabilityPolicyNames);
            policyNames.removeAll(licensePolicyNames);
            boolean hasVulnerabilityErrors = false;
            boolean hasLicenseErrors = false;
            if (!policyNames.isEmpty()) {
                printViolatedPolicyNames(componentName, componentVersion, policyNames);
            }

            if (!vulnerabilityPolicyNames.isEmpty()) {
                hasVulnerabilityErrors = checkVulnerabilityErrorsAndLog(vulnerabilityViolations);
            }

            if (!licensePolicyNames.isEmpty()) {
                hasLicenseErrors = checkLicenseErrorsAndLog(licenseViolations);
            }

            if (hasVulnerabilityErrors || hasLicenseErrors) {
                violatedPolicyComponentNames.add(componentName);
            }
        }

        if (!violatedPolicyComponentNames.isEmpty()) {
            eventSystem.publishEvent(Event.ExitCode, new ExitCodeRequest(ExitCodeType.FAILURE_POLICY_VIOLATION, createViolationMessage(violatedPolicyComponentNames)));
        }
    }

    private void generateJSONScanOutput(NameVersion projectNameVersion, List<DeveloperScanComponentResultView> results) throws DetectUserFriendlyException {
        IntegrationEscapeUtil escapeUtil = new IntegrationEscapeUtil();
        String escapedProjectName = escapeUtil.replaceWithUnderscore(projectNameVersion.getName());
        String escapedProjectVersionName = escapeUtil.replaceWithUnderscore(projectNameVersion.getVersion());
        File jsonScanFile = new File(directoryManager.getScanOutputDirectory(), escapedProjectName + "_" + escapedProjectVersionName + "_BlackDuck_DeveloperMode_Result.json");
        if (jsonScanFile.exists()) {
            try {
                Files.delete(jsonScanFile.toPath());
            } catch (IOException ex) {
                logger.warn("Unable to delete an already-existing Black Duck Rapid Scan Result file: {}", jsonScanFile.getAbsoluteFile(), ex);
            }
        }

        String jsonString = gson.toJson(results);
        logger.trace("Rapid Scan JSON result output: ");
        logger.trace("{}", jsonString);
        try {
            DetectFileUtils.writeToFile(jsonScanFile, jsonString);
        } catch (IOException ex) {
            throw new DetectUserFriendlyException("Cannot create rapid scan output file", ex, ExitCodeType.FAILURE_UNKNOWN_ERROR);
        }
    }

    private void printViolatedPolicyNames(String componentName, String componentVersion, Set<String> policyNames) {
        for (String policyName : policyNames) {
            logger.info("Policy rule \"{}\" was violated by component \"{}\" ({}).",
                policyName,
                componentName,
                componentVersion
            );
        }
    }

    private boolean checkVulnerabilityErrorsAndLog(Set<PolicyViolationVulnerabilityView> vulnerabilites) {
        boolean hasErrors = false;
        for (PolicyViolationVulnerabilityView vulnerabilityPolicyViolation : vulnerabilites) {
            boolean hasError = StringUtils.isNotBlank(vulnerabilityPolicyViolation.getErrorMessage());
            boolean hasWarning = StringUtils.isNotBlank(vulnerabilityPolicyViolation.getWarningMessage());
            if (hasError) {
                logger.error(vulnerabilityPolicyViolation.getErrorMessage());
                hasErrors = true;
            }

            if (hasWarning) {
                logger.warn(vulnerabilityPolicyViolation.getWarningMessage());
            }
        }
        return hasErrors;
    }

    private boolean checkLicenseErrorsAndLog(Set<PolicyViolationLicenseView> licenses) {
        boolean hasErrors = false;
        for (PolicyViolationLicenseView licensePolicyViolation : licenses) {
            boolean hasError = StringUtils.isNotBlank(licensePolicyViolation.getErrorMessage());
            boolean hasWarning = StringUtils.isNotBlank(licensePolicyViolation.getWarningMessage());
            if (hasError) {
                logger.error(licensePolicyViolation.getErrorMessage());
                hasErrors = true;
            }

            if (hasWarning) {
                logger.warn(licensePolicyViolation.getWarningMessage());
            }
        }
        return hasErrors;
    }

    private String createViolationMessage(Set<String> violatedPolicyNames) {
        StringBuilder stringBuilder = new StringBuilder(200);
        stringBuilder.append("Black Duck found:");
        stringBuilder.append(fixComponentPlural(" %d %s in violation", violatedPolicyNames.size()));
        return stringBuilder.toString();
    }

    private String fixComponentPlural(String formatString, int count) {
        String label = "components";
        if (count == 1)
            label = "component";
        return String.format(formatString, count, label);
    }
}
