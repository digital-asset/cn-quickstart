// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.utility;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfiguration {
    @Autowired
    void configureObjectMapper(final ObjectMapper mapper) {
        // enable JsonNullable serialization
        mapper.registerModule(new JsonNullableModule());
        // do not exclude fields with null values from serialization
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }
}
