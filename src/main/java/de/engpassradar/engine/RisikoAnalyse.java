package de.engpassradar.engine;

import de.engpassradar.ergebnis.Ergebnis;
import de.engpassradar.ergebnis.RisikoEintrag;
import de.engpassradar.modell.Lieferant;
import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Teil;
import de.engpassradar.modell.Werk;
import de.engpassradar.modell.Zuteilungsregel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bewertet jeden Lieferanten mit einem Totalausfall fester Dauer: Der Ausfall wird in jeder
 * moeglichen Startwoche simuliert, der schlimmste Zeitpunkt zaehlt.
 */
public final class RisikoAnalyse {

    private RisikoAnalyse() {
    }

    public static List<RisikoEintrag> bewerte(Werk werk, int dauer) {
        List<RisikoEintrag> eintraege = new ArrayList<>();
        int letzteStartwoche = werk.wochen().size() - dauer;

        for (Lieferant l : werk.lieferanten()) {
            long maxSchaden = -1;
            long mitMassnahme = 0;
            int kritisch = 0;
            for (int start = 0; start <= letzteStartwoche; start++) {
                Szenario s = new Szenario("risiko-" + l.id(), "Ausfall " + l.name(), l.id(), start, dauer, 0);
                long schaden = Simulation.berechne(werk, s, Zuteilungsregel.PROPORTIONAL)
                        .kennzahlen().schadenspotenzial();
                if (schaden > maxSchaden) {
                    maxSchaden = schaden;
                    kritisch = start;
                    Ergebnis besser = Simulation.berechne(werk, s, Zuteilungsregel.RELATIVER_DECKUNGSBEITRAG);
                    mitMassnahme = besser.kennzahlen().schadenspotenzial();
                }
            }
            List<String> teile = werk.teile().stream()
                    .filter(t -> t.lieferantId().equals(l.id()))
                    .map(Teil::name)
                    .toList();
            long erwartet = Math.round(l.ausfallwahrscheinlichkeit() * maxSchaden);
            eintraege.add(new RisikoEintrag(l.id(), l.name(), l.tier(), teile, l.ausfallwahrscheinlichkeit(),
                    werk.wochen().get(kritisch), maxSchaden, mitMassnahme, erwartet));
        }
        eintraege.sort(Comparator.comparingLong(RisikoEintrag::erwarteterSchaden).reversed());
        return eintraege;
    }
}
