package com.estudos.ganhodecapital.cli;

import com.estudos.ganhodecapital.domain.CalculadoraDeImposto;
import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.web.dto.ImpostoResponse;
import com.estudos.ganhodecapital.web.dto.OperacaoRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
 * Reaproveita o mesmo nucleo {@link CalculadoraDeImposto} da API; nao grava no historico.
 */
@Component
@Profile("cli")
public class CliRunner implements ApplicationRunner {

    private static final String BOM = "﻿";

    private final ObjectMapper mapper;
    private final CalculadoraDeImposto calculadora = new CalculadoraDeImposto();

    public CliRunner(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        processar(System.in.readAllBytes(), System.out);
    }

    void processar(byte[] entrada, PrintStream saida) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(entrada), StandardCharsets.UTF_8))) {

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
                List<OperacaoRequest> pedido = mapper.readValue(linha, new TypeReference<>() {});
                List<Operacao> operacoes = pedido.stream().map(OperacaoRequest::paraDominio).toList();
                List<ImpostoResponse> resposta = calculadora.calcular(operacoes).stream()
                        .map(ImpostoResponse::de)
                        .toList();
                saida.println(mapper.writeValueAsString(resposta));
            }
        }
    }
}
