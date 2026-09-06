package com.estudos.ganhodecapital.arquitetura;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guarda-corpos da arquitetura. O objetivo do projeto e estudar clean
 * architecture, entao as regras que a sustentam sao testadas, nao so descritas.
 */
@AnalyzeClasses(packages = "com.estudos.ganhodecapital", importOptions = com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    @ArchTest
    static final ArchRule dominio_nao_depende_de_frameworks =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.validation..",
                            "com.fasterxml.jackson..",
                            "org.hibernate..")
                    .because("o dominio deve ser Java puro, testavel e reaproveitavel sem Spring/JPA");

    @ArchTest
    static final ArchRule dominio_nao_depende_das_camadas_externas =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..web..", "..cli..", "..historico..", "..application..", "..formato..", "..config..")
                    .because("o dominio fica no centro: nada de fora entra nele");

    @ArchTest
    static final ArchRule formato_nao_depende_de_adapters =
            noClasses().that().resideInAPackage("..formato..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..web..", "..cli..", "..historico..", "..application..")
                    .because("o formato do desafio e compartilhado; nao conhece quem o usa");

    @ArchTest
    static final ArchRule cli_nao_depende_de_web =
            noClasses().that().resideInAPackage("..cli..")
                    .should().dependOnClassesThat().resideInAPackage("..web..")
                    .because("web e cli sao adapters de entrada independentes");

    @ArchTest
    static final ArchRule web_nao_depende_de_cli =
            noClasses().that().resideInAPackage("..web..")
                    .should().dependOnClassesThat().resideInAPackage("..cli..");

    @ArchTest
    static final ArchRule persistencia_nao_depende_dos_servicos_nem_da_web =
            noClasses().that().resideInAPackage("..historico..")
                    .should().dependOnClassesThat().resideInAnyPackage("..application..", "..web..", "..cli..")
                    .because("a persistencia e infraestrutura: quem depende dela e a aplicacao, nao o contrario");
}
