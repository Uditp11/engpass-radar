package de.engpassradar.engine;

import de.engpassradar.daten.BeispielWerk;
import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Werk;
import de.engpassradar.modell.Zuteilungsregel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZuteilungTest {

    private final Werk werk = BeispielWerk.erstellen();

    @Test
    @DisplayName("Relativer Deckungsbeitrag: beim Steuergeraete-Engpass lohnt sich der Benziner pro Steuergeraet am meisten")
    void reihenfolgeNachRelativemDeckungsbeitrag() {
        var benzin = werk.varianten().get(0);
        var elektro = werk.varianten().get(1);
        var suv = werk.varianten().get(2);

        double rBenzin = Zuteilung.relativerDeckungsbeitrag(werk, benzin, "T01");   // 2800 / 1
        double rElektro = Zuteilung.relativerDeckungsbeitrag(werk, elektro, "T01"); // 4200 / 2
        double rSuv = Zuteilung.relativerDeckungsbeitrag(werk, suv, "T01");         // 6500 / 3

        assertTrue(rBenzin > rSuv && rSuv > rElektro);
    }

    @Test
    @DisplayName("Ohne Engpass wird das ganze Programm gebaut")
    void ohneEngpassAllesBaubar() {
        Map<String, Integer> geplant = Map.of("KOMPAKT_B", 10, "KOMPAKT_E", 10, "SUV_E", 10);
        Map<String, Integer> bedarf = new LinkedHashMap<>();
        bedarf.put("T01", 60);
        Map<String, Integer> verfuegbar = Map.of("T01", 60);

        Map<String, Integer> gebaut = Zuteilung.berechne(werk, geplant, bedarf, verfuegbar,
                Zuteilungsregel.RELATIVER_DECKUNGSBEITRAG);
        assertEquals(geplant, gebaut);
    }

    @Test
    @DisplayName("Massnahme wirkt: Zuteilung nach relativem Deckungsbeitrag senkt den Schaden beim Chip-Ausfall")
    void massnahmeSenktSchaden() {
        Szenario chipAusfall = BeispielWerk.szenarien().stream()
                .filter(s -> s.id().equals("halbleiter")).findFirst().orElseThrow();

        long proportional = Simulation.berechne(werk, chipAusfall, Zuteilungsregel.PROPORTIONAL)
                .kennzahlen().schadenspotenzial();
        long mitMassnahme = Simulation.berechne(werk, chipAusfall, Zuteilungsregel.RELATIVER_DECKUNGSBEITRAG)
                .kennzahlen().schadenspotenzial();

        assertTrue(mitMassnahme < proportional);
    }
}
