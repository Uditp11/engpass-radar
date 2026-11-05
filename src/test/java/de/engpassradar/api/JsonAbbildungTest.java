package de.engpassradar.api;

import de.engpassradar.daten.BeispielWerk;
import de.engpassradar.engine.Simulation;
import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Zuteilungsregel;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonAbbildungTest {

    @Test
    @DisplayName("JSON-B bildet Ergebnis und Totalausfall (auslastung = null) ab, wie die Oberflaeche es erwartet")
    void ergebnisAlsJson() throws Exception {
        var werk = BeispielWerk.erstellen();
        var ergebnis = Simulation.berechne(werk, new Szenario("t", "t", "L3", 3, 1, 0),
                Zuteilungsregel.PROPORTIONAL);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            String json = jsonb.toJson(ergebnis);
            assertTrue(json.contains("\"kennzahlen\""));
            assertTrue(json.contains("\"ampel\":\"GELB\""));
            assertTrue(json.contains("\"schadenspotenzial\""));
            assertTrue(!json.contains("Infinity") && !json.contains("NaN"));
        }
    }
}
