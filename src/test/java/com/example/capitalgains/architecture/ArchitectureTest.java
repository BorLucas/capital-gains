package com.example.capitalgains.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture guardrails. The project's goal is to study clean architecture,
 * so the rules that hold it up are tested, not just described.
 */
@AnalyzeClasses(packages = "com.example.capitalgains", importOptions = com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_frameworks =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.validation..",
                            "com.fasterxml.jackson..",
                            "org.hibernate..")
                    .because("the domain must be plain Java, testable and reusable without Spring/JPA");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..web..", "..cli..", "..history..", "..orders..", "..application..", "..format..", "..config..")
                    .because("the domain sits at the center: nothing from outside reaches into it");

    @ArchTest
    static final ArchRule format_does_not_depend_on_adapters =
            noClasses().that().resideInAPackage("..format..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..web..", "..cli..", "..history..", "..orders..", "..application..")
                    .because("the challenge format is shared; it does not know who uses it");

    @ArchTest
    static final ArchRule cli_does_not_depend_on_web =
            noClasses().that().resideInAPackage("..cli..")
                    .should().dependOnClassesThat().resideInAPackage("..web..")
                    .because("web and cli are independent inbound adapters");

    @ArchTest
    static final ArchRule web_does_not_depend_on_cli =
            noClasses().that().resideInAPackage("..web..")
                    .should().dependOnClassesThat().resideInAPackage("..cli..");

    @ArchTest
    static final ArchRule persistence_does_not_depend_on_services_or_web =
            noClasses().that().resideInAnyPackage("..history..", "..orders..")
                    .should().dependOnClassesThat().resideInAnyPackage("..application..", "..web..", "..cli..")
                    .because("persistence is infrastructure: the application depends on it, not the other way around");
}
