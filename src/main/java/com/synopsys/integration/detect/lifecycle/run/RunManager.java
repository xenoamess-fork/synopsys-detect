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
package com.synopsys.integration.detect.lifecycle.run;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatchOutput;
import com.synopsys.integration.blackduck.codelocation.binaryscanner.BinaryScanBatchOutput;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchOutput;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.DetectTool;
import com.synopsys.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.synopsys.integration.detect.lifecycle.run.data.ProductRunData;
import com.synopsys.integration.detect.lifecycle.run.operation.OperationFactory;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.ImpactAnalysisOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.input.BdioInput;
import com.synopsys.integration.detect.lifecycle.run.operation.input.FullScanPostProcessingInput;
import com.synopsys.integration.detect.lifecycle.run.operation.input.ImpactAnalysisInput;
import com.synopsys.integration.detect.lifecycle.run.operation.input.RapidScanInput;
import com.synopsys.integration.detect.lifecycle.run.operation.input.SignatureScanInput;
import com.synopsys.integration.detect.tool.UniversalToolsResult;
import com.synopsys.integration.detect.tool.impactanalysis.ImpactAnalysisToolResult;
import com.synopsys.integration.detect.util.filter.DetectToolFilter;
import com.synopsys.integration.detect.workflow.bdio.AggregateOptions;
import com.synopsys.integration.detect.workflow.bdio.BdioResult;
import com.synopsys.integration.detect.workflow.blackduck.codelocation.CodeLocationAccumulator;
import com.synopsys.integration.detect.workflow.blackduck.codelocation.CodeLocationResults;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.phonehome.PhoneHomeManager;
import com.synopsys.integration.detect.workflow.report.util.ReportConstants;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.NameVersion;

public class RunManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public RunResult run(RunContext runContext) throws DetectUserFriendlyException, IntegrationException {
        RunResult runResult = new RunResult();
        ProductRunData productRunData = runContext.getProductRunData();
        OperationFactory operationFactory = new OperationFactory(runContext);
        RunOptions runOptions = runContext.createRunOptions();
        DetectToolFilter detectToolFilter = runOptions.getDetectToolFilter();
        EventSystem eventSystem = runContext.getEventSystem();

        logger.info(ReportConstants.RUN_SEPARATOR);
        if (runContext.getProductRunData().shouldUsePolarisProduct()) {
            runPolarisProduct(operationFactory, detectToolFilter, runOptions);
        } else {
            logger.info("Polaris tools will not be run.");
        }

        UniversalToolsResult universalToolsResult = runUniversalProjectTools(operationFactory, runOptions, detectToolFilter, eventSystem, runResult);

        if (productRunData.shouldUseBlackDuckProduct()) {
            AggregateOptions aggregateOptions = operationFactory.createAggregateOptionsOperation().execute(universalToolsResult.anyFailed());
            runBlackDuckProduct(productRunData.getBlackDuckRunData(), operationFactory, runOptions, detectToolFilter, runResult,
                universalToolsResult.getNameVersion(), aggregateOptions);
        } else {
            logger.info("Black Duck tools will not be run.");
        }

        logger.info("All tools have finished.");
        logger.info(ReportConstants.RUN_SEPARATOR);

        return runResult;
    }

    private UniversalToolsResult runUniversalProjectTools(
        OperationFactory operationFactory,
        RunOptions runOptions,
        DetectToolFilter detectToolFilter,
        EventSystem eventSystem,
        RunResult runResult
    ) throws DetectUserFriendlyException, IntegrationException {
        boolean anythingFailed = false;

        logger.info(ReportConstants.RUN_SEPARATOR);
        if (!runOptions.shouldPerformRapidModeScan() && detectToolFilter.shouldInclude(DetectTool.DOCKER)) {
            logger.info("Will include the Docker tool.");
            anythingFailed = anythingFailed || operationFactory.createDockerOperation().execute(runResult);
            logger.info("Docker actions finished.");
        } else {
            logger.info("Docker tool will not be run.");
        }

        logger.info(ReportConstants.RUN_SEPARATOR);
        if (!runOptions.shouldPerformRapidModeScan() && detectToolFilter.shouldInclude(DetectTool.BAZEL)) {
            logger.info("Will include the Bazel tool.");
            anythingFailed = anythingFailed || operationFactory.createBazelOperation().execute(runResult);
            logger.info("Bazel actions finished.");
        } else {
            logger.info("Bazel tool will not be run.");
        }

        logger.info(ReportConstants.RUN_SEPARATOR);
        if (detectToolFilter.shouldInclude(DetectTool.DETECTOR)) {
            logger.info("Will include the detector tool.");
            anythingFailed = anythingFailed || operationFactory.createDetectorOperation().execute(runResult);
            logger.info("Detector actions finished.");
        } else {
            logger.info("Detector tool will not be run.");
        }

        logger.info(ReportConstants.RUN_SEPARATOR);
        logger.debug("Completed code location tools.");

        logger.debug("Determining project info.");

        NameVersion projectNameVersion = operationFactory.createProjectDecisionOperation().execute(runResult.getDetectToolProjectInfo());

        logger.info(String.format("Project name: %s", projectNameVersion.getName()));
        logger.info(String.format("Project version: %s", projectNameVersion.getVersion()));

        eventSystem.publishEvent(Event.ProjectNameVersionChosen, projectNameVersion);

        if (anythingFailed) {
            return UniversalToolsResult.failure(projectNameVersion);
        } else {
            return UniversalToolsResult.success(projectNameVersion);
        }
    }

    private void runPolarisProduct(OperationFactory operationFactory, DetectToolFilter detectToolFilter, RunOptions runOptions) {
        logger.info(ReportConstants.RUN_SEPARATOR);
        if (detectToolFilter.shouldInclude(DetectTool.POLARIS) && !runOptions.shouldPerformRapidModeScan()) {
            logger.info("Will include the Polaris tool.");
            operationFactory.createPolarisOperation().execute();
            logger.info("Polaris actions finished.");
        } else {
            logger.info("Polaris CLI tool will not be run.");
        }
    }

    private void runBlackDuckProduct(BlackDuckRunData blackDuckRunData, OperationFactory operationFactory, RunOptions runOptions, DetectToolFilter detectToolFilter, RunResult runResult, NameVersion projectNameVersion,
        AggregateOptions aggregateOptions)
        throws IntegrationException, DetectUserFriendlyException {

        logger.debug("Black Duck tools will run.");

        ProjectVersionWrapper projectVersionWrapper = null;

        BdioInput bdioInput = new BdioInput(aggregateOptions, projectNameVersion, runResult.getDetectCodeLocations());
        BdioResult bdioResult = operationFactory.createBdioFileGenerationOperation().execute(bdioInput);
        if (runOptions.shouldPerformRapidModeScan() && blackDuckRunData.isOnline()) {
            logger.info(ReportConstants.RUN_SEPARATOR);
            RapidScanInput rapidScanInput = new RapidScanInput(projectNameVersion, bdioResult);
            operationFactory.createRapidScanOperation().execute(blackDuckRunData, blackDuckRunData.getBlackDuckServicesFactory(), rapidScanInput);
        } else {
            if (blackDuckRunData.isOnline()) {
                blackDuckRunData.getPhoneHomeManager().ifPresent(PhoneHomeManager::startPhoneHome);
                BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
                logger.debug("Getting or creating project.");
                projectVersionWrapper = operationFactory.createProjectCreationOperation().execute(blackDuckServicesFactory, projectNameVersion);
            } else {
                logger.debug("Detect is not online, and will not create the project.");
            }

            logger.debug("Completed project and version actions.");
            logger.debug("Processing Detect Code Locations.");

            CodeLocationAccumulator codeLocationAccumulator = new CodeLocationAccumulator<>();
            Optional<CodeLocationCreationData<UploadBatchOutput>> uploadResult = operationFactory.createBdioUploadOperation().execute(blackDuckRunData, bdioResult);
            uploadResult.ifPresent(codeLocationAccumulator::addWaitableCodeLocation);

            logger.debug("Completed Detect Code Location processing.");

            logger.info(ReportConstants.RUN_SEPARATOR);
            if (detectToolFilter.shouldInclude(DetectTool.SIGNATURE_SCAN)) {
                logger.info("Will include the signature scanner tool.");
                SignatureScanInput signatureScanInput = new SignatureScanInput(projectNameVersion, runResult.getDockerTar().orElse(null));
                Optional<CodeLocationCreationData<ScanBatchOutput>> signatureScanResult = operationFactory.createSignatureScanOperation().execute(signatureScanInput);
                signatureScanResult.ifPresent(codeLocationAccumulator::addWaitableCodeLocation);
                logger.info("Signature scanner actions finished.");
            } else {
                logger.info("Signature scan tool will not be run.");
            }

            logger.info(ReportConstants.RUN_SEPARATOR);
            if (detectToolFilter.shouldInclude(DetectTool.BINARY_SCAN)) {
                logger.info("Will include the binary scanner tool.");
                if (blackDuckRunData.isOnline()) {
                    Optional<CodeLocationCreationData<BinaryScanBatchOutput>> binaryScanResult = operationFactory.createBinaryScanOperation().execute(projectNameVersion);
                    binaryScanResult.ifPresent(codeLocationAccumulator::addWaitableCodeLocation);
                }
                logger.info("Binary scanner actions finished.");
            } else {
                logger.info("Binary scan tool will not be run.");
            }
            ImpactAnalysisOperation impactAnalysisOperation = operationFactory.createImpactAnalysisOperation();
            logger.info(ReportConstants.RUN_SEPARATOR);
            if (detectToolFilter.shouldInclude(DetectTool.IMPACT_ANALYSIS) && impactAnalysisOperation.shouldImpactAnalysisToolRun()) {
                logger.info("Will include the Vulnerability Impact Analysis tool.");
                ImpactAnalysisInput impactAnalysisInput = new ImpactAnalysisInput(projectNameVersion, projectVersionWrapper);
                ImpactAnalysisToolResult impactAnalysisToolResult = impactAnalysisOperation.execute(impactAnalysisInput);
                /* TODO: There is currently no mechanism within Black Duck for checking the completion status of an Impact Analysis code location. Waiting should happen here when such a mechanism exists. See HUB-25142. JM - 08/2020 */
                codeLocationAccumulator.addNonWaitableCodeLocation(impactAnalysisToolResult.getCodeLocationNames());
                logger.info("Vulnerability Impact Analysis tool actions finished.");
            } else if (impactAnalysisOperation.shouldImpactAnalysisToolRun()) {
                logger.info("Vulnerability Impact Analysis tool is enabled but will not run due to tool configuration.");
            } else {
                logger.info("Vulnerability Impact Analysis tool will not be run.");
            }

            logger.info(ReportConstants.RUN_SEPARATOR);
            //We have finished code locations.
            CodeLocationResults codeLocationResults = operationFactory.createCodeLocationResultCalculationOperation().execute(codeLocationAccumulator);

            if (blackDuckRunData.isOnline()) {
                logger.info("Will perform Black Duck post actions.");
                BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
                FullScanPostProcessingInput fullScanPostProcessingInput = new FullScanPostProcessingInput(projectNameVersion, bdioResult, codeLocationResults, projectVersionWrapper);
                operationFactory.createFullScanPostProcessingOperation().execute(blackDuckServicesFactory, fullScanPostProcessingInput);
                logger.info("Black Duck actions have finished.");
            } else {
                logger.debug("Will not perform Black Duck post actions: Detect is not online.");
            }
        }
    }
}
