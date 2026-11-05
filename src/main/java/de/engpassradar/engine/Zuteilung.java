package de.engpassradar.engine;

import de.engpassradar.modell.Variante;
import de.engpassradar.modell.Werk;
import de.engpassradar.modell.Zuteilungsregel;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verteilt die in einer Woche verfuegbaren Teile auf die Fahrzeugvarianten.
 * Beide Regeln garantieren: Es wird nie mehr verbaut als verfuegbar ist.
 */
public final class Zuteilung {

    private Zuteilung() {
    }

    /**
     * @param geplant    Variante -> geplante Stueckzahl in dieser Woche
     * @param bedarf     Teil -> Bruttobedarf in dieser Woche
     * @param verfuegbar Teil -> verfuegbare Menge in dieser Woche
     * @return Variante -> baubare Stueckzahl
     */
    public static Map<String, Integer> berechne(Werk werk, Map<String, Integer> geplant,
                                                Map<String, Integer> bedarf, Map<String, Integer> verfuegbar,
                                                Zuteilungsregel regel) {
        Map<String, Double> deckungsgrad = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : bedarf.entrySet()) {
            int b = e.getValue();
            deckungsgrad.put(e.getKey(), b == 0 ? 1.0 : Math.min(1.0, verfuegbar.get(e.getKey()) / (double) b));
        }
        boolean engpass = deckungsgrad.values().stream().anyMatch(d -> d < 1.0);
        if (!engpass) {
            return new LinkedHashMap<>(geplant);
        }
        return switch (regel) {
            case PROPORTIONAL -> proportional(werk, geplant, deckungsgrad);
            case RELATIVER_DECKUNGSBEITRAG -> nachRelativemDeckungsbeitrag(werk, geplant, verfuegbar, deckungsgrad);
        };
    }

    /** Jede Variante bekommt den Deckungsgrad ihres knappsten Teils. */
    private static Map<String, Integer> proportional(Werk werk, Map<String, Integer> geplant,
                                                     Map<String, Double> deckungsgrad) {
        Map<String, Integer> gebaut = new LinkedHashMap<>();
        for (Variante v : werk.varianten()) {
            double faktor = 1.0;
            for (Map.Entry<String, Double> e : deckungsgrad.entrySet()) {
                if (werk.mengeProFahrzeug(v.id(), e.getKey()) > 0) {
                    faktor = Math.min(faktor, e.getValue());
                }
            }
            gebaut.put(v.id(), (int) Math.floor(geplant.get(v.id()) * faktor));
        }
        return gebaut;
    }

    /**
     * Engpassorientierte Reihenfolge: Deckungsbeitrag geteilt durch den Verbrauch des knappsten Teils.
     * Varianten, die das Engpassteil nicht brauchen, kommen zuerst. Danach gierig auffuellen.
     */
    private static Map<String, Integer> nachRelativemDeckungsbeitrag(Werk werk, Map<String, Integer> geplant,
                                                                     Map<String, Integer> verfuegbar,
                                                                     Map<String, Double> deckungsgrad) {
        String engpassTeil = deckungsgrad.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow();

        List<Variante> reihenfolge = werk.varianten().stream()
                .sorted(Comparator.comparingDouble((Variante v) -> relativerDeckungsbeitrag(werk, v, engpassTeil))
                        .reversed())
                .toList();

        Map<String, Integer> rest = new LinkedHashMap<>(verfuegbar);
        Map<String, Integer> gebaut = new LinkedHashMap<>();
        werk.varianten().forEach(v -> gebaut.put(v.id(), 0));

        for (Variante v : reihenfolge) {
            int moeglich = geplant.get(v.id());
            for (String teilId : rest.keySet()) {
                int menge = werk.mengeProFahrzeug(v.id(), teilId);
                if (menge > 0) {
                    moeglich = Math.min(moeglich, rest.get(teilId) / menge);
                }
            }
            gebaut.put(v.id(), moeglich);
            for (String teilId : rest.keySet()) {
                int menge = werk.mengeProFahrzeug(v.id(), teilId);
                rest.put(teilId, rest.get(teilId) - menge * moeglich);
            }
        }
        return gebaut;
    }

    static double relativerDeckungsbeitrag(Werk werk, Variante v, String engpassTeil) {
        int verbrauch = werk.mengeProFahrzeug(v.id(), engpassTeil);
        return verbrauch == 0 ? Double.MAX_VALUE : v.deckungsbeitrag() / (double) verbrauch;
    }
}
