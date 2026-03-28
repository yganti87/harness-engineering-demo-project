package com.library.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;

/**
 * Architecture tests enforcing the layer dependency model.
 *
 * <p>Layer model: types → config → repository → service → controller
 *
 * <p>These tests run as part of the standard Maven test phase (no Docker required).
 * Violations fail the build with a message that includes remediation instructions.
 *
 * <p>See docs/ARCHITECTURE.md for the full layer model and rationale.
 */
@AnalyzeClasses(packages = "com.library")
class LayerDependencyTest {

    /**
     * Controllers must NOT import from repository layer.
     * REMEDIATION: Inject BookService (interface), not BookRepository.
     * See docs/ARCHITECTURE.md and docs/PATTERNS.md section "4. Controller Pattern".
     */
    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repositories =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because(
                "Controllers must only call services. "
                + "REMEDIATION: Inject the service interface (BookService), not the repository. "
                + "See docs/ARCHITECTURE.md for the layer model."
            );

    /**
     * Service layer must NOT import from controller layer.
     * REMEDIATION: Services are independent of HTTP. Remove the controller import.
     */
    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers =
        noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because(
                "Services must not depend on controllers. "
                + "REMEDIATION: Remove the import of any controller class from the service. "
                + "Services are HTTP-agnostic. See docs/ARCHITECTURE.md."
            );

    /**
     * Repository layer must NOT import from service layer.
     * REMEDIATION: Repositories are passive data-access objects. Remove the service import.
     */
    @ArchTest
    static final ArchRule repositories_should_not_depend_on_services =
        noClasses().that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAPackage("..service..")
            .because(
                "Repositories must not depend on services. "
                + "REMEDIATION: Repositories are passive data-access objects. "
                + "Remove any service import from the repository layer. See docs/ARCHITECTURE.md."
            );

    /**
     * Types layer must NOT import from any other application layer.
     * REMEDIATION: DTOs and enums are the foundation — they must have no dependencies.
     */
    @ArchTest
    static final ArchRule types_should_not_depend_on_application_layers =
        noClasses().that().resideInAPackage("..types..")
            .should().dependOnClassesThat().resideInAPackage("..service..")
            .orShould().dependOnClassesThat().resideInAPackage("..controller..")
            .orShould().dependOnClassesThat().resideInAPackage("..repository..")
            .because(
                "Types (DTOs, enums) are the foundation layer and must not depend on other layers. "
                + "REMEDIATION: Remove the import from the types class. "
                + "If you need business logic in a DTO, move it to the service layer. "
                + "See docs/ARCHITECTURE.md."
            );

    /**
     * Config layer must NOT import from service or controller layers.
     */
    @ArchTest
    static final ArchRule config_should_not_depend_on_service_or_controller =
        noClasses().that().resideInAPackage("..config..")
            .should().dependOnClassesThat().resideInAPackage("..service..")
            .orShould().dependOnClassesThat().resideInAPackage("..controller..")
            .because(
                "Config classes must only import types. "
                + "REMEDIATION: Remove service or controller imports from the config class. "
                + "Configuration should depend only on types and external libraries. "
                + "See docs/ARCHITECTURE.md."
            );

    /**
     * Services must NOT declare Counter/Timer/Gauge fields.
     * REMEDIATION: Use inline meterRegistry.counter() calls instead.
     * See docs/PATTERNS.md section "8. Metrics Pattern".
     */
    @ArchTest
    static final ArchRule services_should_not_have_metric_fields =
        noFields().that().haveRawType(Counter.class)
            .or().haveRawType(Timer.class)
            .or().haveRawType(Gauge.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..service..")
            .allowEmptyShould(true)
            .because(
                "Services must use inline meterRegistry.counter() calls, not pre-registered "
                + "Counter/Timer/Gauge fields. "
                + "REMEDIATION: Replace field + @PostConstruct with "
                + "meterRegistry.counter(name, tags).increment(). "
                + "See docs/PATTERNS.md section '8. Metrics Pattern'."
            );

    /**
     * Services must NOT use {@code @PostConstruct} for metric initialization.
     * REMEDIATION: Remove @PostConstruct initMetrics() and use inline meterRegistry.counter()
     * calls instead.
     * See docs/PATTERNS.md section "8. Metrics Pattern".
     */
    @ArchTest
    static final ArchRule services_should_not_use_postconstruct =
        noMethods().that().areAnnotatedWith(PostConstruct.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..service..")
            .allowEmptyShould(true)
            .because(
                "Services must not use @PostConstruct for metric initialization. "
                + "REMEDIATION: Remove @PostConstruct initMetrics() and use inline "
                + "meterRegistry.counter() calls instead. "
                + "See docs/PATTERNS.md section '8. Metrics Pattern'."
            );

}
