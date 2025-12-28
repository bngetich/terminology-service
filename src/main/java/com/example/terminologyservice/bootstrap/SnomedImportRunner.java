package com.example.terminologyservice.bootstrap;

import com.example.terminologyservice.importer.SnomedImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("import-snomed")
public class SnomedImportRunner implements CommandLineRunner {

    private final SnomedImportService importService;

    public SnomedImportRunner(SnomedImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(String... args) throws Exception {

        importService.importSnomed(
                "C:/Users/kimut/Desktop/omscs/Projects/sct2_Concept_Snapshot_US1000124_20250901.txt",
                "C:/Users/kimut/Desktop/omscs/Projects/sct2_Description_Snapshot-en_US1000124_20250901.txt",
                "2025-09-01"
        );

        System.out.println("SNOMED import complete");
    }
}
