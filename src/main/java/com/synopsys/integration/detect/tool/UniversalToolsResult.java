/**
 * synopsys-detect
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.detect.tool;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.detect.kotlin.nameversion.NameVersionDecision;
import com.synopsys.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.synopsys.integration.detect.workflow.project.DetectToolProjectInfo;
import com.synopsys.integration.util.NameVersion;

public class UniversalToolsResult {
    public UniversalToolsResultType getResultType() {
        return resultType;
    }

    public NameVersion getNameVersion() {
        return nameVersion;
    }

    private enum UniversalToolsResultType {
        FAILED,
        SUCCESS
    }

    private UniversalToolsResultType resultType;
    private NameVersion nameVersion;

    public UniversalToolsResult(UniversalToolsResultType resultType, NameVersion nameVersion) {
        this.resultType = resultType;
        this.nameVersion = nameVersion;
    }

    public static UniversalToolsResult failure(NameVersion nameVersion) {
        return new UniversalToolsResult(UniversalToolsResultType.FAILED, nameVersion);
    }

    public static UniversalToolsResult success(NameVersion nameVersion) {
        return new UniversalToolsResult(UniversalToolsResultType.SUCCESS, nameVersion);
    }

    public boolean anyFailed() {
        return resultType == UniversalToolsResultType.FAILED;
    }
}