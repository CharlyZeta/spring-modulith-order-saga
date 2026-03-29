package com.showcase.ordersystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.io.IOException;

@SpringBootTest
class ModulithArchitectureTest {

    @Test
    void verifyModularStructure() {
        // Valida que no se violen los límites de los módulos según las reglas de Spring Modulith.
        ApplicationModules.of(ModularOrderSystemApplication.class).verify();
    }

    @Test
    void documentModules() throws IOException {
        // Genera documentación de los módulos (diagramas de componentes PlantUML) en target/spring-modulith-docs.
        ApplicationModules modules = ApplicationModules.of(ModularOrderSystemApplication.class);
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}