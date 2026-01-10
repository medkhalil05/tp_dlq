package com.example.tpdlq.controller;

import com.example.tpdlq.consumer.DlqConsumer;
import com.example.tpdlq.model.DlqMessage;
import com.example.tpdlq.service.MessageProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Autowired
    private DlqConsumer dlqConsumer;

    @Autowired
    private MessageProducerService messageProducerService;

    @GetMapping("/")
    public String index(Model model) {
        List<DlqMessage> dlqMessages = dlqConsumer.getDlqMessages();
        model.addAttribute("dlqMessages", dlqMessages);
        return "index";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, 
                                   RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".jsonl")) {
            redirectAttributes.addFlashAttribute("message", "Only .jsonl files are allowed");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/";
        }

        try {
            int totalLines = 0;
            int successfulLines = 0;
            int skippedLines = 0;

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
            );
            
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
                    // Send to Kafka topic
                    messageProducerService.sendToInputTopic(line);
                    successfulLines++;
                    logger.debug("Successfully sent line {} to Kafka", totalLines);
                } catch (Exception e) {
                    logger.error("Error sending line {} to Kafka: {}. Skipping.", totalLines, line, e);
                    skippedLines++;
                }
            }
            reader.close();

            String message = String.format(
                "File uploaded successfully! Total lines: %d, Successful: %d, Skipped: %d",
                totalLines, successfulLines, skippedLines
            );
            redirectAttributes.addFlashAttribute("message", message);
            redirectAttributes.addFlashAttribute("messageType", "success");
            logger.info("Finished processing uploaded file. {}", message);
            
        } catch (Exception e) {
            logger.error("Error processing uploaded file", e);
            redirectAttributes.addFlashAttribute("message", "Error processing file: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/";
    }
}
