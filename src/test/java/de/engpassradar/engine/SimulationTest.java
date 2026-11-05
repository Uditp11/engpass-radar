package de.engpassradar.engine;

import de.engpassradar.daten.BeispielWerk;
import de.engpassradar.ergebnis.Ergebnis;
import de.engpassradar.ergebnis.TeilWoche;
import de.engpassradar.ergebnis.TeilZeile;
import de.engpassradar.modell.Ampel;
import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Teil;
import de.engpassradar.modell.Werk;
import de.engpassradar.modell.Zuteilungsregel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationTest {

    private final Werk werk = BeispielWerk.erstellen();

    private static Szenario ausfall(String lieferant, int start, int dauer, int rest) {
        return new Szenario("test", "Test", lieferant, start, dauer, rest);
    }

    private static TeilZeile zeile(Ergebnis e, String teilId) {
        return e.teile().stream().filter(t -> t.teilId().equals(teilId)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Stuecklistenaufloesung: Bruttobedarf = Summe aus Stueckzahl x Menge pro Fahrzeug")
    void bruttobedarfAusStueckliste() {
        Map<String, Integer> geplant = new LinkedHashMap<>();
        geplant.put(BeispielWerk.KOMPAKT_BENZIN, 400);
        geplant.put(BeispielWerk.KOMPAKT_ELEKTRO, 300);
        geplant.put(BeispielWerk.SUV_ELEKTRO, 200);

        assertEquals(400 * 1 + 300 * 2 + 200 * 3, Simulation.bruttobedarf(werk, "T01", geplant));
        assertEquals(300 * 6 + 200 * 10, Simulation.bruttobedarf(werk, "T02", geplant));
        assertEquals(400, Simulation.bruttobedarf(werk, "T06", geplant));
    }

    @Test
    @DisplayName("Basisplan: keine Fehlmengen, alle Fahrzeuge baubar, kein Schaden")
    void basisplanOhneEngpass() {
        Ergebnis e = Simulation.berechne(werk, Szenario.basis(), Zuteilungsregel.PROPORTIONAL);

        assertEquals(0, e.kennzahlen().nichtGebaut());
        assertEquals(0L, e.kennzahlen().schadenspotenzial());
        assertNull(e.kennzahlen().ersteEngpassWoche());
        e.teile().forEach(t -> t.wochen().forEach(w -> assertEquals(0, w.fehlmenge())));
    }

    @Test
    @DisplayName("Hohe Auslastung ohne Fehlmenge wird gelb, nicht rot")
    void hoheAuslastungIstGelb() {
        Ergebnis e = Simulation.berechne(werk, Szenario.basis(), Zuteilungsregel.PROPORTIONAL);
        TeilWoche batterieKw03 = zeile(e, "T02").wochen().get(8);

        assertTrue(batterieKw03.auslastung() > 0.85);
        assertEquals(Ampel.GELB, batterieKw03.ampel());
    }

    @Test
    @DisplayName("Kurzer Ausfall wird vom Sicherheitsbestand abgefangen: gelb, kein Schaden")
    void sicherheitsbestandFaengtKurzenAusfallAb() {
        Ergebnis e = Simulation.berechne(werk, ausfall("L3", 3, 1, 0), Zuteilungsregel.PROPORTIONAL);
        TeilWoche kabelKw50 = zeile(e, "T03").wochen().get(3);

        assertEquals(0, kabelKw50.kapazitaet());
        assertNull(kabelKw50.auslastung());
        assertTrue(kabelKw50.ausLager() > 0);
        assertEquals(Ampel.GELB, kabelKw50.ampel());
        assertEquals(0L, e.kennzahlen().schadenspotenzial());
    }

    @Test
    @DisplayName("Mehrstufig: Ausfall beim Tier-2-Chiplieferanten erzeugt verzoegert einen Engpass beim Steuergeraet")
    void tier2AusfallSchlaegtAufTier1Durch() {
        Ergebnis e = Simulation.berechne(werk, ausfall("L5", 2, 3, 0), Zuteilungsregel.PROPORTIONAL);
        List<TeilWoche> chip = zeile(e, "T07").wochen();
        List<TeilWoche> steuergeraet = zeile(e, "T01").wochen();

        assertEquals(0, chip.get(2).fehlmenge(), "erste Ausfallwoche: Chip-Puffer reicht noch");
        assertTrue(chip.get(3).fehlmenge() > 0, "zweite Ausfallwoche: Chips fehlen");
        assertTrue(steuergeraet.get(4).fehlmenge() > 0, "Werk spuert den Engpass beim Steuergeraet");
        assertTrue(e.kennzahlen().engpassTeile().contains("Steuergeraet"));
        assertTrue(e.kennzahlen().schadenspotenzial() > 0);
    }

    @Test
    @DisplayName("100 Prozent Restkapazitaet entspricht dem Basisplan")
    void volleRestkapazitaetIstBasis() {
        Ergebnis basis = Simulation.berechne(werk, Szenario.basis(), Zuteilungsregel.PROPORTIONAL);
        Ergebnis voll = Simulation.berechne(werk, ausfall("L2", 0, 12, 100), Zuteilungsregel.PROPORTIONAL);

        assertEquals(basis.kennzahlen(), voll.kennzahlen());
        assertEquals(basis.teile(), voll.teile());
    }

    @ParameterizedTest
    @EnumSource(Zuteilungsregel.class)
    @DisplayName("Nie mehr verbaut als verfuegbar, nie mehr gebaut als geplant (alle Szenarien)")
    void zuteilungIstImmerZulaessig(Zuteilungsregel regel) {
        for (Szenario s : BeispielWerk.szenarien()) {
            Ergebnis e = Simulation.berechne(werk, s, regel);
            for (int w = 0; w < werk.wochen().size(); w++) {
                for (Teil t : werk.teile()) {
                    if (werk.istVorprodukt(t.id())) {
                        continue;
                    }
                    TeilWoche tw = zeile(e, t.id()).wochen().get(w);
                    int verfuegbar = tw.geliefert() + tw.ausLager();
                    int verbaut = 0;
                    for (var v : e.varianten()) {
                        verbaut += werk.mengeProFahrzeug(v.varianteId(), t.id()) * v.gebaut().get(w);
                    }
                    assertTrue(verbaut <= verfuegbar, s.id() + " " + t.id() + " Woche " + w);
                }
                for (var v : e.varianten()) {
                    assertTrue(v.gebaut().get(w) <= v.geplant().get(w));
                    assertTrue(v.gebaut().get(w) >= 0);
                }
            }
        }
    }

    @Test
    @DisplayName("Schadenspotenzial = nicht gebaute Fahrzeuge x Deckungsbeitrag")
    void schadenspotenzialRechnung() {
        Ergebnis e = Simulation.berechne(werk, ausfall("L2", 7, 2, 30), Zuteilungsregel.PROPORTIONAL);
        long erwartet = 0;
        for (var v : e.varianten()) {
            int db = werk.varianten().stream().filter(x -> x.id().equals(v.varianteId())).findFirst()
                    .orElseThrow().deckungsbeitrag();
            for (int w = 0; w < v.geplant().size(); w++) {
                erwartet += (long) (v.geplant().get(w) - v.gebaut().get(w)) * db;
            }
        }
        assertEquals(erwartet, e.kennzahlen().schadenspotenzial());
        assertTrue(erwartet > 0);
    }
}
