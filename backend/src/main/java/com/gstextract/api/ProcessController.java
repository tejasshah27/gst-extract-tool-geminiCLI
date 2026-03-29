package com.gstextract.api;

import com.gstextract.model.SourceType;
import com.gstextract.orchestrator.ProcessOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProcessController {

    private final ProcessOrchestrator orchestrator;

    @Autowired
    public ProcessController(ProcessOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processFile(
            @RequestParam("returnFile") MultipartFile returnFile,
            @RequestParam(value = "purchaseRegisterFile", required = false) MultipartFile purchaseRegisterFile,
            @RequestParam("returnType") String returnType,
            @RequestParam("mode") String mode) {
        try {
            if (returnFile.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please upload a GST return file");
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(error);
            }

            // Phase 1: Defaulting to GSTR2A for now as other parsers/reconcilers are in later phases
            SourceType sourceType = SourceType.GSTR2A;
            if ("GSTR2B".equalsIgnoreCase(returnType)) {
                sourceType = SourceType.GSTR2B;
            }

            byte[] resultFile = orchestrator.processGstr2a(returnFile.getInputStream(), returnFile.getOriginalFilename(), sourceType);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result.xlsx\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resultFile);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error processing file: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);
        }
    }
}
