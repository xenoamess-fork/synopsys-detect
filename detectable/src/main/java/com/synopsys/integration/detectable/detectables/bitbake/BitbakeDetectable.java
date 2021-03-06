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
package com.synopsys.integration.detectable.detectables.bitbake;

import java.io.File;

import com.synopsys.integration.detectable.Detectable;
import com.synopsys.integration.detectable.DetectableEnvironment;
import com.synopsys.integration.detectable.ExecutableTarget;
import com.synopsys.integration.detectable.detectable.Requirements;
import com.synopsys.integration.detectable.detectable.annotation.DetectableInfo;
import com.synopsys.integration.detectable.detectable.exception.DetectableException;
import com.synopsys.integration.detectable.detectable.executable.resolver.BashResolver;
import com.synopsys.integration.detectable.detectable.explanation.PropertyProvided;
import com.synopsys.integration.detectable.detectable.file.FileFinder;
import com.synopsys.integration.detectable.detectable.result.DetectableResult;
import com.synopsys.integration.detectable.detectable.result.PropertyInsufficientDetectableResult;
import com.synopsys.integration.detectable.extraction.Extraction;
import com.synopsys.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(language = "various", forge = "YOCTO", requirementsMarkdown = "Properties: Package names <br /><br /> File: build env script.<br /><br /> Executable: bash")
public class BitbakeDetectable extends Detectable {
    private final BitbakeDetectableOptions bitbakeDetectableOptions;
    private final FileFinder fileFinder;
    private final BitbakeExtractor bitbakeExtractor;
    private final BashResolver bashResolver;

    private File foundBuildEnvScript;
    private ExecutableTarget bashExe;

    public BitbakeDetectable(DetectableEnvironment detectableEnvironment, FileFinder fileFinder, BitbakeDetectableOptions bitbakeDetectableOptions, BitbakeExtractor bitbakeExtractor,
        BashResolver bashResolver) {
        super(detectableEnvironment);
        this.fileFinder = fileFinder;
        this.bitbakeDetectableOptions = bitbakeDetectableOptions;
        this.bitbakeExtractor = bitbakeExtractor;
        this.bashResolver = bashResolver;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        foundBuildEnvScript = requirements.file(bitbakeDetectableOptions.getBuildEnvName());

        if (bitbakeDetectableOptions.getPackageNames() == null || bitbakeDetectableOptions.getPackageNames().isEmpty()) {
            return new PropertyInsufficientDetectableResult("Bitbake requires that at least one package name is provided.");
        } else {
            requirements.explain(new PropertyProvided("Bitbake Package Names"));
        }

        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);
        bashExe = requirements.executable(bashResolver::resolveBash, "bash");
        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return bitbakeExtractor.extract(environment.getDirectory(), foundBuildEnvScript, bitbakeDetectableOptions.getSourceArguments(), bitbakeDetectableOptions.getPackageNames(), bitbakeDetectableOptions.getSearchDepth(), bashExe);
    }
}
