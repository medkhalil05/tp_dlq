package com.example.tpdlq.controller;

import com.example.tpdlq.consumer.DlqConsumer;
import com.example.tpdlq.model.DlqMessage;
import com.example.tpdlq.service.MessageProducerService;
import com.example.tpdlq.service.ValidMessageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import io.micrometer.core.instrument.MeterRegistry;

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

    @Autowired
    private ValidMessageStore validMessageStore;

    @Autowired
    private MeterRegistry meterRegistry;

    @GetMapping("/")
    public String index(Model model) {
        List<DlqMessage> dlqMessages = dlqConsumer.getDlqMessages();
        model.addAttribute("dlqMessages", dlqMessages);
        model.addAttribute("validMessages", validMessageStore.getAll());

        // Metrics cards
        double processed = getCounter("tpdlq_messages_processed_total");
        double valid = getCounter("tpdlq_messages_valid_total");
        double invalid = getCounter("tpdlq_messages_invalid_total");
        double malformed = getCounter("tpdlq_messages_malformed_total");
        model.addAttribute("processedCount", (long) processed);
        model.addAttribute("validCount", (long) valid);
        model.addAttribute("invalidCount", (long) invalid);
        model.addAttribute("malformedCount", (long) malformed);
        return "index";
    }

    @PostMapping("/dlq/reprocess/{id}")
    public String reprocessMessage(@PathVariable String id, RedirectAttributes redirectAttributes) {
        return dlqConsumer.findById(id)
                .map(msg -> {
                    messageProducerService.sendToInputTopic(msg.getOriginalMessage());
                    dlqConsumer.removeById(id);
                    redirectAttributes.addFlashAttribute("message", "Reprocessed message " + id + " back to input topic.");
                    redirectAttributes.addFlashAttribute("messageType", "success");
                    logger.info("Reprocessed DLQ message {}", id);
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("message", "Message not found for reprocessing.");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/";
                });
    }

    @PostMapping("/dlq/clear")
    public String clearDlq(RedirectAttributes redirectAttributes) {
        dlqConsumer.clearDlqMessages();
        redirectAttributes.addFlashAttribute("message", "Cleared DLQ messages.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        logger.info("Cleared DLQ messages");
        return "redirect:/";
    }

    @PostMapping("/valid/clear")
    public String clearValid(RedirectAttributes redirectAttributes) {
        validMessageStore.clear();
        redirectAttributes.addFlashAttribute("message", "Cleared valid messages.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        logger.info("Cleared valid messages");
        return "redirect:/";
    }

    private double getCounter(String name) {
        var c = meterRegistry.find(name).counter();
        return c != null ? c.count() : 0.0;
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
