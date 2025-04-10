/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */

package org.opensearch.dataprepper.plugins.source.crowdstrike.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.opensearch.dataprepper.model.plugin.PluginConfigVariable;

@Getter
public class Oauth2Config {
    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    @Setter
    private String bearerToken;

    @AssertTrue(message = "Client ID, Client Secret are both required for Oauth2")
    private boolean isOauth2ConfigValid() {
        return clientId != null && clientSecret != null;
    }
}
