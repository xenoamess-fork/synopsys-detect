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

import java.util.Arrays;
import java.util.Optional;

import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.codelocation.Result;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchOutput;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.synopsys.integration.detect.lifecycle.run.operation.input.SignatureScanInput;
import com.synopsys.integration.detect.tool.signaturescanner.BlackDuckSignatureScannerTool;
import com.synopsys.integration.detect.tool.signaturescanner.SignatureScannerToolResult;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.status.DetectIssue;
import com.synopsys.integration.detect.workflow.status.DetectIssueType;
import com.synopsys.integration.detect.workflow.status.Status;
import com.synopsys.integration.detect.workflow.status.StatusType;
import com.synopsys.integration.exception.IntegrationException;

public class SignatureScanOperation {
    private final BlackDuckRunData blackDuckRunData;
    private final BlackDuckSignatureScannerTool signatureScannerTool;
    private final EventSystem eventSystem;

    public SignatureScanOperation(BlackDuckRunData blackDuckRunData, BlackDuckSignatureScannerTool signatureScannerTool,
        EventSystem eventSystem) {
        this.blackDuckRunData = blackDuckRunData;
        this.signatureScannerTool = signatureScannerTool;
        this.eventSystem = eventSystem;
    }

    public Optional<CodeLocationCreationData<ScanBatchOutput>> execute(SignatureScanInput signatureScanInput) throws DetectUserFriendlyException, IntegrationException {
        Optional<CodeLocationCreationData<ScanBatchOutput>> result = Optional.empty();
        BlackDuckServerConfig blackDuckServerConfig = null;
        CodeLocationCreationService codeLocationCreationService = null;
        if (null != blackDuckRunData && blackDuckRunData.isOnline()) {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            codeLocationCreationService = blackDuckServicesFactory.createCodeLocationCreationService();
            blackDuckServerConfig = blackDuckRunData.getBlackDuckServerConfig();
        }
        SignatureScannerToolResult signatureScannerToolResult = signatureScannerTool.runScanTool(codeLocationCreationService, blackDuckServerConfig, signatureScanInput.getProjectNameVersion(), signatureScanInput.getDockerTar());
        if (signatureScannerToolResult.getResult() == Result.SUCCESS && signatureScannerToolResult.getCreationData().isPresent()) {
            result = signatureScannerToolResult.getCreationData();
        } else if (signatureScannerToolResult.getResult() != Result.SUCCESS) {
            eventSystem.publishEvent(Event.StatusSummary, new Status("SIGNATURE_SCAN", StatusType.FAILURE));
            eventSystem.publishEvent(Event.Issue, new DetectIssue(DetectIssueType.SIGNATURE_SCANNER, Arrays.asList(signatureScannerToolResult.getResult().toString())));
        }
        return result;
    }
}
