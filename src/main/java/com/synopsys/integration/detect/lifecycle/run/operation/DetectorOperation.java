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
package com.synopsys.integration.detect.lifecycle.run.operation;

import java.nio.file.Path;
import java.util.List;

import com.synopsys.integration.configuration.config.PropertyConfiguration;
import com.synopsys.integration.detect.configuration.DetectConfigurationFactory;
import com.synopsys.integration.detect.configuration.DetectProperties;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.DetectTool;
import com.synopsys.integration.detect.configuration.enumeration.ExitCodeType;
import com.synopsys.integration.detect.lifecycle.run.RunResult;
import com.synopsys.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.synopsys.integration.detect.tool.detector.CodeLocationConverter;
import com.synopsys.integration.detect.tool.detector.DetectDetectableFactory;
import com.synopsys.integration.detect.tool.detector.DetectorIssuePublisher;
import com.synopsys.integration.detect.tool.detector.DetectorRuleFactory;
import com.synopsys.integration.detect.tool.detector.DetectorTool;
import com.synopsys.integration.detect.tool.detector.DetectorToolResult;
import com.synopsys.integration.detect.tool.detector.extraction.ExtractionEnvironmentProvider;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.file.DirectoryManager;
import com.synopsys.integration.detector.base.DetectorType;
import com.synopsys.integration.detector.evaluation.DetectorEvaluationOptions;
import com.synopsys.integration.detector.finder.DetectorFinder;
import com.synopsys.integration.detector.finder.DetectorFinderOptions;
import com.synopsys.integration.detector.rule.DetectorRuleSet;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.NameVersion;

public class DetectorOperation {
    private PropertyConfiguration detectConfiguration;
    private DetectConfigurationFactory detectConfigurationFactory;
    private DirectoryManager directoryManager;
    private EventSystem eventSystem;
    private DetectDetectableFactory detectDetectableFactory;
    private ExtractionEnvironmentProvider extractionEnvironmentProvider;
    private CodeLocationConverter codeLocationConverter;

    public DetectorOperation(PropertyConfiguration detectConfiguration, DetectConfigurationFactory detectConfigurationFactory, DirectoryManager directoryManager, EventSystem eventSystem,
        DetectDetectableFactory detectDetectableFactory, ExtractionEnvironmentProvider extractionEnvironmentProvider, CodeLocationConverter codeLocationConverter) {
        this.detectConfiguration = detectConfiguration;
        this.detectConfigurationFactory = detectConfigurationFactory;
        this.directoryManager = directoryManager;
        this.eventSystem = eventSystem;
        this.detectDetectableFactory = detectDetectableFactory;
        this.extractionEnvironmentProvider = extractionEnvironmentProvider;
        this.codeLocationConverter = codeLocationConverter;
    }

    public boolean execute(RunResult runResult) throws DetectUserFriendlyException, IntegrationException {
        String projectBomTool = detectConfiguration.getValueOrEmpty(DetectProperties.DETECT_PROJECT_DETECTOR.getProperty()).orElse(null);
        List<DetectorType> requiredDetectors = detectConfiguration.getValueOrDefault(DetectProperties.DETECT_REQUIRED_DETECTOR_TYPES.getProperty());
        boolean buildless = detectConfiguration.getValueOrDefault(DetectProperties.DETECT_BUILDLESS.getProperty());

        DetectorRuleFactory detectorRuleFactory = new DetectorRuleFactory();
        DetectorRuleSet detectRuleSet = detectorRuleFactory.createRules(detectDetectableFactory, buildless);

        Path sourcePath = directoryManager.getSourceDirectory().toPath();
        DetectorFinderOptions finderOptions = detectConfigurationFactory.createSearchOptions(sourcePath);
        DetectorEvaluationOptions detectorEvaluationOptions = detectConfigurationFactory.createDetectorEvaluationOptions();

        DetectorIssuePublisher detectorIssuePublisher = new DetectorIssuePublisher();
        DetectorTool detectorTool = new DetectorTool(new DetectorFinder(), extractionEnvironmentProvider, eventSystem, codeLocationConverter, detectorIssuePublisher);
        DetectorToolResult detectorToolResult = detectorTool.performDetectors(directoryManager.getSourceDirectory(), detectRuleSet, finderOptions, detectorEvaluationOptions, projectBomTool, requiredDetectors);

        detectorToolResult.getBomToolProjectNameVersion().ifPresent(it -> runResult.addToolNameVersion(DetectTool.DETECTOR, new NameVersion(it.getName(), it.getVersion())));
        runResult.addDetectCodeLocations(detectorToolResult.getBomToolCodeLocations());
        boolean anythingFailed = false;
        if (!detectorToolResult.getFailedDetectorTypes().isEmpty()) {
            eventSystem.publishEvent(Event.ExitCode, new ExitCodeRequest(ExitCodeType.FAILURE_DETECTOR, "A detector failed."));
            anythingFailed = true;
        }
        return anythingFailed;
    }
}
