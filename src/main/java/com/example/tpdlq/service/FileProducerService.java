package com.example.tpdlq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Service
public class FileProducerService {

    private static final Logger logger = LoggerFactory.getLogger(FileProducerService.class);

    @Autowired
    private MessageProducerService messageProducerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void processJsonlFile(String filePath) {
        logger.info("Starting to process .jsonl file: {}", filePath);
        int totalLines = 0;
        int successfulLines = 0;
        int skippedLines = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                line = line.trim();
                
                if (line.isEmpty()) {
                    logger.debug("Skipping empty line at line {}", totalLines);
                    skippedLines++;
                    continue;
                }

                try {
                    // Validate that it's valid JSON by parsing it
                    objectMapper.readTree(line);
                    
                    // Send to Kafka topic
                    messageProducerService.sendToInputTopic(line);
                    successfulLines++;
                    logger.debug("Successfully sent line {} to Kafka", totalLines);
                } catch (Exception e) {
                    logger.error("Malformed JSON at line {}: {}. Skipping.", totalLines, line, e);
                    skippedLines++;
                }
            }

            logger.info("Finished processing file. Total lines: {}, Successful: {}, Skipped: {}", 
                    totalLines, successfulLines, skippedLines);
        } catch (IOException e) {
            logger.error("Error reading file: {}", filePath, e);
            throw new RuntimeException("Failed to process file: " + filePath, e);
        }
    }
}
