package lt.daiva.bankstatement.dto;

public record ExportResult(byte[] csv, int totalRecords) {}
