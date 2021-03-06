/**
 * configuration
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
package com.synopsys.integration.configuration.property;

import java.util.List;
import java.util.stream.Collectors;

public class Properties {
    private final List<Property> properties;

    public Properties(List<Property> properties) {
        this.properties = properties;
    }

    public List<String> getPropertyKeys() {
        return properties.stream()
                   .map(Property::getKey)
                   .collect(Collectors.toList());
    }

    public List<String> getSortedPropertyKeys() {
        return getPropertyKeys().stream()
                   .sorted()
                   .collect(Collectors.toList());
    }

    public List<Property> getProperties() {
        return properties;
    }
}
