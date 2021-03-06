/**
 * detectable
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
package com.synopsys.integration.detectable.detectables.rebar.model;

import java.util.Optional;

import com.synopsys.integration.detectable.detectable.codelocation.CodeLocation;
import com.synopsys.integration.util.NameVersion;

public class RebarParseResult {
    private final Optional<NameVersion> projectNameVersion;
    private final CodeLocation codeLocation;

    public RebarParseResult(final NameVersion projectNameVersion, final CodeLocation codeLocation) {
        this.projectNameVersion = Optional.of(projectNameVersion);
        this.codeLocation = codeLocation;
    }

    public RebarParseResult(final CodeLocation codeLocation) {
        this.projectNameVersion = Optional.empty();
        this.codeLocation = codeLocation;
    }

    public Optional<NameVersion> getProjectNameVersion() {
        return projectNameVersion;
    }

    public CodeLocation getCodeLocation() {
        return codeLocation;
    }
}
