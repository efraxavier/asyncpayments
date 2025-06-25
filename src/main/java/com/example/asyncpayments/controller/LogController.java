package com.example.asyncpayments.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Configuration
@EnableScheduling
class LogCleanupConfig {
}

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Value("${logging.file.name:asyncpayments-validacao.log}")
    private String logFilePath;

    private static final String FRONT_LOG_PREFIX = "[FRONT]";

    // Limpa o arquivo de log a cada 10 minutos
    @Scheduled(fixedRate = 600_000)
    public void limparLogsPeriodicamente() {
        try {
            File logFile = new File(logFilePath);
            if (logFile.exists()) {
                new FileWriter(logFile, false).close();
            }
        } catch (IOException e) {
            // Loga erro de limpeza, se necessário
            System.err.println("Erro ao limpar arquivo de log: " + e.getMessage());
        }
    }

    // Recebe logs do front e salva no arquivo geral, prefixando como FRONT
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> receiveLogs(@RequestParam String logs) {
        try (FileWriter fw = new FileWriter(logFilePath, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            String[] lines = logs.split("\\r?\\n");
            for (String line : lines) {
                bw.write(LocalDateTime.now() + " " + FRONT_LOG_PREFIX + " " + line);
                bw.newLine();
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar logs.");
        }
        return ResponseEntity.ok().build();
    }

    // Endpoint para buscar todos os logs (apenas admin)
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getLogs() {
        try {
            File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Arquivo de log não encontrado.");
            }
            // Lê o conteúdo do arquivo de log
            String content = new String(Files.readAllBytes(Paths.get(logFilePath)));
            // Formata cada linha para visualização padronizada
            StringBuilder formatted = new StringBuilder();
            String[] lines = content.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains("[FRONT]")) {
                    formatted.append(" FRONT  | ").append(line).append(System.lineSeparator());
                } else if (line.contains("[JWT]")) {
                    formatted.append(" BACK   | ").append(line).append(System.lineSeparator());
                } else if (line.contains("[AUTH]")) {
                    formatted.append(" BACK   | ").append(line).append(System.lineSeparator());
                } else if (line.contains("[USER]")) {
                    formatted.append(" BACK   | ").append(line).append(System.lineSeparator());
                } else if (line.contains("[SINCRONIZACAO]")) {
                    formatted.append(" BACK   | ").append(line).append(System.lineSeparator());
                } else if (line.contains("[TRANSACAO]")) {
                    formatted.append(" BACK   | ").append(line).append(System.lineSeparator());
                } else if (line.contains("[API][RESPONSE]")) {
                    formatted.append(" API    | ").append(line).append(System.lineSeparator());
                } else {
                    formatted.append(" OUTRO  | ").append(line).append(System.lineSeparator());
                }
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", logFile.getName());
            return new ResponseEntity<>(formatted.toString(), headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao ler logs.");
        }
    }
}
