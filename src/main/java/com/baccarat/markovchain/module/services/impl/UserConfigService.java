package com.baccarat.markovchain.module.services.impl;




import com.baccarat.markovchain.module.data.Config;
import com.baccarat.markovchain.module.repository.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserConfigService {

    private final ConfigRepository configRepository;

    @Autowired
    public UserConfigService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    // Get all configurations for a user
    public List<Config> getConfigsByUserUuid(String userUuid) {
        return configRepository.findByUserUuid(userUuid);
    }

    // Get a configuration by ID
    public Optional<Config> getConfigById(int configId) {
        return configRepository.findById(configId);
    }

    // Add or update a configuration
    public Config saveOrUpdateConfig(Config config) {
        return configRepository.save(config);
    }
    public Optional<Config> findByName(String name) {
        return Optional.ofNullable(configRepository.findByName(name));
    }

    public Optional<Config> findByUserUuidAndName(String userUuid,String name) {
        return Optional.ofNullable(configRepository.findByUserUuidAndName(userUuid,name));
    }



    // Delete a configuration by ID
    public void deleteConfigById(int configId) {
        configRepository.deleteById(configId);
    }
}
