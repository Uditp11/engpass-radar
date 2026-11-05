package de.engpassradar.engine;

import de.engpassradar.daten.BeispielWerk;
import de.engpassradar.ergebnis.RisikoEintrag;
import de.engpassradar.modell.Werk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RisikoAnalyseTest {

    private final Werk werk = BeispielWerk.erstellen();

    @Test
    @DisplayName("Jeder Lieferant wird bewertet, sortiert nach erwartetem Schaden")
    void rankingVollstaendigUndSortiert() {
        List<RisikoEintrag> ranking = RisikoAnalyse.bewerte(werk, 3);

        assertEquals(werk.lieferanten().size(), ranking.size());
        for (int i = 1; i < ranking.size(); i++) {
            assertTrue(ranking.get(i - 1).erwarteterSchaden() >= ranking.get(i).erwarteterSchaden());
        }
    }

    @Test
    @DisplayName("Erwarteter Schaden = Ausfallwahrscheinlichkeit x Schaden im schlimmsten Zeitpunkt")
    void erwarteterSchaden() {
        for (RisikoEintrag r : RisikoAnalyse.bewerte(werk, 3)) {
            assertEquals(Math.round(r.ausfallwahrscheinlichkeit() * r.maxSchaden()), r.erwarteterSchaden());
            assertTrue(r.maxSchadenMitMassnahme() <= r.maxSchaden(), r.name());
        }
    }

    @Test
    @DisplayName("Ein Lieferant fuer zwei Teile (Klumpenrisiko) hat den hoechsten Schaden")
    void klumpenrisiko() {
        RisikoEintrag hoechster = RisikoAnalyse.bewerte(werk, 3).stream()
                .max((a, b) -> Long.compare(a.maxSchaden(), b.maxSchaden())).orElseThrow();
        assertEquals("L6", hoechster.lieferantId());
        assertEquals(2, hoechster.teile().size());
    }
}
