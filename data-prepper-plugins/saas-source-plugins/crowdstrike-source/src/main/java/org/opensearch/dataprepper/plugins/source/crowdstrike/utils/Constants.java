package org.opensearch.dataprepper.plugins.source.crowdstrike.utils;

import java.util.List;

/**
 * The type Constants.
 */
public class Constants {

    public static final String PLUGIN_NAME = "crowdstrike";
    public static final int MAX_RETRIES = 6;
    public static final List<Integer> RETRY_ATTEMPT_SLEEP_TIME = List.of(1, 2, 5, 10, 20, 40);
    public static final int SLEEP_TIME_MULTIPLIER = 1000;

}