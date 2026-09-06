package com.estudos.ganhodecapital.cli;

import com.estudos.ganhodecapital.domain.CalculadoraDeImposto;
import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.domain.ResultadoOperacao;
import com.estudos.ganhodecapital.formato.ImpostoJson;
import com.estudos.ganhodecapital.formato.OperacaoJson;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Modo linha de comando (perfil {@code cli}), no formato original do desafio:
 * cada linha da entrada padrao e um array JSON de operacoes; para cada linha
 * escreve na saida um array JSON com o imposto de cada operacao.
 *
 * <pre>
 * java -jar target/ganho-de-capital-0.0.1-SNAPSHOT.jar --spring.profiles.active=cli &lt; exemplos/entrada.txt
 * </pre>
 *
 * <p>Fail-fast: le e valida <b>todas</b> as linhas, calcula <b>tudo</b>, e so
 * entao imprime. Uma linha invalida aborta antes de qualquer saida parcial.</p>
 *
 * <p>Reaproveita o nucleo {@link CalculadoraDeImposto} e o formato compartilhado
 * ({@code formato.*}); nao conhece a camada web nem grava no historico.</p>
 */
@Component
@Profile("cli")
public class CliRunner implements ApplicationRunner {

    private static final String BOM = "﻿";
    private static final CalculadoraDeImposto CALCULADORA = new CalculadoraDeImposto();

    private final ObjectMapper mapper;

    public CliRunner(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        processar(System.in, System.out);
    }

    void processar(InputStream entrada, PrintStream saida) throws IOException {
        List<List<Operacao>> simulacoes = lerSimulacoes(entrada);

        List<List<ResultadoOperacao>> resultados = simulacoes.stream()
                .map(CALCULADORA::calcular)
                .toList();

        for (List<ResultadoOperacao> simulacao : resultados) {
            saida.println(mapper.writeValueAsString(ImpostoJson.deResultados(simulacao)));
        }
    }

    private List<List<Operacao>> lerSimulacoes(InputStream entrada) throws IOException {
        List<List<Operacao>> simulacoes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(entrada, StandardCharsets.UTF_8))) {

            String linha;
            boolean primeira = true;
            while ((linha = reader.readLine()) != null) {
                if (primeira) {
                    linha = linha.startsWith(BOM) ? linha.substring(BOM.length()) : linha;
                    primeira = false;
                }
                if (linha.isBlank()) {
                    continue;
                }
                List<OperacaoJson> pedido = mapper.readValue(linha, new TypeReference<>() {});
                simulacoes.add(OperacaoJson.paraDominio(pedido));
            }
        }
        return simulacoes;
    }
}
