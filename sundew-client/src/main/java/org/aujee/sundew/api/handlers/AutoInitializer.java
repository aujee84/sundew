package org.aujee.sundew.api.handlers;

import org.aujee.sundew.spi.AutoInitializerProvider;


import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public interface AutoInitializer {

    class ConfigurationHandler {

        private ConfigurationHandler(){}

        public static void initialize() {
            ServiceLoader<AutoInitializerProvider> loader = ServiceLoader.load(AutoInitializerProvider.class);
            AutoInitializerProvider provider = loader.findFirst().orElseThrow(() -> new ServiceConfigurationError(
                    "Unable to load AutoInitializerProvider service."));
            provider.initialize();
        }
    }
}
