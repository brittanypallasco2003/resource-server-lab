package com.spring.resource.server.lab.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/// Turns the architecture rules into something the build defends on its own.
///
/// Until now, "Spring does not enter `domain/`" was a sentence in `CLAUDE.md` that depended on
/// nobody slipping up. Here it is a build condition: adding a `@Service` to `CreateUserService` or
/// an `@Entity` to `User` breaks the build immediately, with a message naming the class and the
/// import that caused it.
@AnalyzeClasses(
        packages = HexagonalArchitectureTest.BASE,
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    static final String BASE = "com.spring.resource.server.lab";

    private static final String DOMAIN = BASE + ".domain..";
    private static final String APPLICATION = BASE + ".application..";
    private static final String INFRASTRUCTURE = BASE + ".infrastructure..";
    private static final String INBOUND_ADAPTERS = BASE + ".infrastructure.adapters.in..";
    private static final String OUTBOUND_ADAPTERS = BASE + ".infrastructure.adapters.out..";

    /// The domain may not depend on any framework nor on any provider.
    @ArchTest
    static final ArchRule domainFreeOfTechnology = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation..",
                    "org.keycloak..",
                    "tools.jackson..",
                    "com.fasterxml.jackson..",
                    "jakarta.ws.rs..")
            .because("the domain must compile and be testable without any technology");

    /// Neither may the application layer. This is the rule that forces `CreateUserService` to be
    /// registered as a `@Bean` in the configuration instead of being annotated with `@Service`.
    @ArchTest
    static final ArchRule applicationFreeOfTechnology = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation..",
                    "org.keycloak..",
                    "tools.jackson..",
                    "com.fasterxml.jackson..",
                    "jakarta.ws.rs..")
            .because("use cases are orchestrated without a framework; the wiring is infrastructure");

    /// Dependencies always point inwards: the domain knows nobody.
    @ArchTest
    static final ArchRule domainDoesNotKnowOuterLayers = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE)
            .because("the domain is the centre of the hexagon and depends on nobody");

    /// The application may not jump to the technological detail.
    @ArchTest
    static final ArchRule applicationDoesNotKnowInfrastructure = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            .because("the application talks to the outside only through the domain's ports");

    /// The inbound REST adapter may not talk directly to the outbound ones: it has to go through
    /// the inbound port. This is exactly the coupling that existed before, when the Keycloak service
    /// imported the controller's DTO.
    @ArchTest
    static final ArchRule adaptersDoNotTalkToEachOther = noClasses()
            .that().resideInAPackage(INBOUND_ADAPTERS)
            .should().dependOnClassesThat().resideInAPackage(OUTBOUND_ADAPTERS)
            .because("the two ends of the hexagon communicate through the centre, not along the edge");

    /// Global check of the direction of the dependencies between layers.
    @ArchTest
    static final ArchRule layersInTheRightOrder = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    /// Sanity check: if the importer finds no classes, the rules above would pass empty and give a
    /// false sense of safety.
    @ArchTest
    static void thereAreClassesToAnalyse(JavaClasses classes) {
        if (classes.isEmpty()) {
            throw new AssertionError("ArchUnit no encontró clases en " + BASE + "; las reglas no estarían probando nada");
        }
    }

}
