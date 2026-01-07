package lt.daiva.bankstatement.controller;

import lt.daiva.bankstatement.service.BankStatementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/statements")
public class BankStatementController {

    private final BankStatementService service;

    public BankStatementController(BankStatementService service) {
        this.service = service;
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Integer>> importCsv(@RequestPart("file") MultipartFile file) {
        int imported = service.importFromCsv(file);
        return ResponseEntity.ok(Map.of("imported", imported));
    }
}
