package br.com.documentshield.config;

import br.com.documentshield.service.PolicyManagementService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PolicyBootstrap implements ApplicationRunner {
    private final PolicyManagementService policies;

    public PolicyBootstrap(PolicyManagementService policies) {
        this.policies = policies;
    }

    @Override
    public void run(ApplicationArguments args) {
        policies.ensureDefaultPolicy();
    }
}
