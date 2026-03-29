package com.gstextract.storage;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class OutputStorageService {

    public String saveFile(byte[] content, String fileName) throws IOException {
        Path outputDir = Paths.get("output");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        Path filePath = outputDir.resolve(fileName);
        Files.write(filePath, content);
        return filePath.toAbsolutePath().toString();
    }
}
