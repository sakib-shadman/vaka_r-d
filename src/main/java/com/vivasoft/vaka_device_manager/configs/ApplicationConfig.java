package com.vivasoft.vaka_device_manager.configs;

import com.vivasoft.vaka_device_manager.vaka_services.VakaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ApplicationConfig {
    @Value("${vaka.connection.close.delay}")
    private long connectionCloseDelay;
    @Value("${show.vaka.verbose.log}")
    private boolean showVakaVerboseLog;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public VakaService vakaService() {
        return new VakaService(connectionCloseDelay, showVakaVerboseLog);
    }
}
