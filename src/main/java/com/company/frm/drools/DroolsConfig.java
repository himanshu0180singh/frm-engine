package com.company.frm.drools;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.internal.io.ResourceFactory;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that builds the Drools {@link KieContainer} at
 * application startup by compiling {@code classpath:rules/fraud-rules.drl}.
 *
 * <p>The container is a singleton bean shared by {@link DroolsFraudEvaluator},
 * which creates a new stateful {@code KieSession} per transaction and disposes
 * it immediately after firing rules.
 */
@Slf4j
@Configuration
public class DroolsConfig {

    private static final String RULES_DRL = "rules/fraud-rules.drl";

    @Bean
    public KieContainer kieContainer() {
        KieServices ks = KieServices.Factory.get();

        // Register the default release with the repository so KieBuilder can
        // resolve the module without requiring a kmodule.xml.
        KieRepository kr = ks.getRepository();
        kr.addKieModule(new KieModule() {
            @Override
            public ReleaseId getReleaseId() {
                return kr.getDefaultReleaseId();
            }
        });

        // Load the DRL file from the classpath explicitly.
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write(ResourceFactory.newClassPathResource(RULES_DRL));

        // Compile all rules.
        KieBuilder builder = ks.newKieBuilder(kfs);
        builder.buildAll();

        // Fail fast at startup — a compilation error in a DRL file must never
        // reach production silently.
        if (builder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(
                    "Drools fraud rule compilation failed:\n"
                            + builder.getResults().getMessages(Message.Level.ERROR));
        }

        KieModule module = builder.getKieModule();
        KieContainer container = ks.newKieContainer(module.getReleaseId());
        log.info("Drools KieContainer ready — fraud rules loaded from {}", RULES_DRL);
        return container;
    }
}
