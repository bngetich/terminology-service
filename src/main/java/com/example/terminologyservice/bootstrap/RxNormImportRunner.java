package com.example.terminologyservice.bootstrap;

import com.example.terminologyservice.importer.RxNormImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("import-rxnorm")
public class RxNormImportRunner implements CommandLineRunner {

    private final RxNormImportService importService;

    public RxNormImportRunner(RxNormImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(String... args) throws Exception {

        importService.importRxNorm(
                "C:/Users/kimut/Desktop/omscs/Projects/RXNCONSO.RRF",
                "2025-12-01"
        );

        System.out.println("RxNorm import complete");
    }
}
