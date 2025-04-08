package org.opensearch.dataprepper.plugins.source.crowdstrike.utils;

import org.opensearch.dataprepper.plugins.source.crowdstrike.CrowdStrikeSourceConfig;

public class CrowdStrikeConfigHelper {

    public static boolean validateConfig(CrowdStrikeSourceConfig config) {
        if (config.getOauth2Config().getClientId() == null || config.getOauth2Config().getClientSecret() == null) {
            throw new RuntimeException("CrowdStrike Client ID or Credential are required for Oauth2.0 Auth Type");
        }
        return true;
    }
}
