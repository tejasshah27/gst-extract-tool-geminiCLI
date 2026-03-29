package com.gstextract.api;

import com.gstextract.model.SourceType;
import com.gstextract.orchestrator.ProcessOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.io.InputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(ProcessController.class)
public class ProcessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessOrchestrator orchestrator;

    @Test
    public void testProcessFileSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "returnFile", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "test content".getBytes());

        when(orchestrator.processGstr2a(any(InputStream.class), anyString(), any(SourceType.class))).thenReturn("result".getBytes());

        mockMvc.perform(multipart("/api/process")
                        .file(file)
                        .param("returnType", "GSTR2A")
                        .param("mode", "validate"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result.xlsx\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes("result".getBytes()));
    }

    @Test
    public void testProcessEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "returnFile", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        mockMvc.perform(multipart("/api/process")
                        .file(file)
                        .param("returnType", "GSTR2A")
                        .param("mode", "validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Please upload a GST return file"));
    }

    @Test
    public void testProcessFileError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "returnFile", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "test content".getBytes());

        when(orchestrator.processGstr2a(any(InputStream.class), anyString(), any(SourceType.class))).thenThrow(new RuntimeException("Parsing error"));

        mockMvc.perform(multipart("/api/process")
                        .file(file)
                        .param("returnType", "GSTR2A")
                        .param("mode", "validate"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error processing file: Parsing error"));
    }
}
