package com.example.terminologyservice.bootstrap;

import com.example.terminologyservice.importer.LoincImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("import-loinc")
public class LoincImportRunner implements CommandLineRunner {

    private final LoincImportService importer;

    public LoincImportRunner(LoincImportService importer) {
        this.importer = importer;
    }

    @Override
    public void run(String... args) throws Exception {

        importer.importLoinc(
                "C:/Users/kimut/Desktop/omscs/Projects/LoincTableCore.csv",
                "2.81"
        );

        System.out.println("LOINC import complete");
    }
}
