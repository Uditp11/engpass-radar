package de.engpassradar.export;

import de.engpassradar.daten.BeispielWerk;
import de.engpassradar.engine.RisikoAnalyse;
import de.engpassradar.engine.Simulation;
import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Werk;
import de.engpassradar.modell.Zuteilungsregel;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Schreibt alle vordefinierten Szenarien als JSON-Dateien, damit die Oberflaeche auch ohne Server
 * (GitHub Pages) funktioniert. Gleiche Engine, gleiche JSON-B-Abbildung wie die REST-API.
 * Aufruf: mvn compile exec:java
 */
public final class DemoExport {

    private DemoExport() {
    }

    public static void main(String[] args) throws Exception {
        Path ziel = Path.of(args.length > 0 ? args[0] : "docs/data");
        Files.createDirectories(ziel.resolve("analyse"));
        Werk werk = BeispielWerk.erstellen();

        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            schreibe(ziel.resolve("stammdaten.json"), jsonb.toJson(werk));
            schreibe(ziel.resolve("szenarien.json"), jsonb.toJson(BeispielWerk.szenarien()));
            schreibe(ziel.resolve("risiko.json"), jsonb.toJson(RisikoAnalyse.bewerte(werk, 3)));
            for (Szenario s : BeispielWerk.szenarien()) {
                for (Zuteilungsregel r : Zuteilungsregel.values()) {
                    String datei = s.id() + "_" + r.name() + ".json";
                    schreibe(ziel.resolve("analyse").resolve(datei), jsonb.toJson(Simulation.berechne(werk, s, r)));
                }
            }
        }
        System.out.println("Demo-Daten geschrieben nach " + ziel.toAbsolutePath());
    }

    private static void schreibe(Path datei, String json) throws IOException {
        Files.writeString(datei, json + "\n", StandardCharsets.UTF_8);
    }
}
