package com.gstextract.orchestrator;

import com.gstextract.exporter.XlsxExporter;
import com.gstextract.model.SourceType;
import com.gstextract.parser.Gstr2aParser;
import com.gstextract.storage.OutputStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ProcessOrchestrator {

    private final Gstr2aParser gstr2aParser;
    private final XlsxExporter xlsxExporter;
    private final OutputStorageService storageService;

    @Autowired
    public ProcessOrchestrator(Gstr2aParser gstr2aParser, XlsxExporter xlsxExporter, OutputStorageService storageService) {
        this.gstr2aParser = gstr2aParser;
        this.xlsxExporter = xlsxExporter;
        this.storageService = storageService;
    }

    public byte[] processGstr2a(InputStream inputStream, String originalFilename, SourceType sourceType) throws Exception {
        // Parse
        Gstr2aParser.ParsingResult result = gstr2aParser.parse(inputStream, sourceType);

        // Export
        byte[] exportedBytes = xlsxExporter.exportInvoicesAndErrors(result.invoices(), result.errors());

        // Save
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputFileName = "Processed_" + originalFilename.replace(".xlsx", "") + "_" + timestamp + ".xlsx";
        
        storageService.saveFile(exportedBytes, outputFileName);

        return exportedBytes;
    }
}
