package com.gstextract.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private Tolerance tolerance;
    private Map<String, Map<String, Map<String, List<String>>>> columnMappings;
    private List<ItcBlockRule> itcBlockRules;

    public static class Tolerance {
        private BigDecimal taxAmount;

        public BigDecimal getTaxAmount() { return taxAmount; }
        public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    }

    public static class ItcBlockRule {
        private String field;
        private List<String> blocklist;
        private boolean rcmOnly;

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public List<String> getBlocklist() { return blocklist; }
        public void setBlocklist(List<String> blocklist) { this.blocklist = blocklist; }
        public boolean isRcmOnly() { return rcmOnly; }
        public void setRcmOnly(boolean rcmOnly) { this.rcmOnly = rcmOnly; }
    }

    public Tolerance getTolerance() { return tolerance; }
    public void setTolerance(Tolerance tolerance) { this.tolerance = tolerance; }
    public Map<String, Map<String, Map<String, List<String>>>> getColumnMappings() { return columnMappings; }
    public void setColumnMappings(Map<String, Map<String, Map<String, List<String>>>> columnMappings) { this.columnMappings = columnMappings; }
    public List<ItcBlockRule> getItcBlockRules() { return itcBlockRules; }
    public void setItcBlockRules(List<ItcBlockRule> itcBlockRules) { this.itcBlockRules = itcBlockRules; }
}
