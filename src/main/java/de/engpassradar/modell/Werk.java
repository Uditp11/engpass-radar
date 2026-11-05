package de.engpassradar.modell;

import java.util.List;
import java.util.Map;

/**
 * Stammdaten eines Werks: Planungshorizont, Varianten, Teile, Lieferanten,
 * Stueckliste und Produktionsprogramm.
 *
 * @param wochen       Bezeichnungen der Planungswochen, z. B. "KW 47"
 * @param stueckliste  Variante -> (Teil -> Menge pro Fahrzeug)
 * @param programm     Variante -> geplante Stueckzahl je Woche (gleiche Laenge wie wochen)
 */
public record Werk(String name,
                   List<String> wochen,
                   List<Variante> varianten,
                   List<Teil> teile,
                   List<Lieferant> lieferanten,
                   Map<String, Map<String, Integer>> stueckliste,
                   Map<String, List<Integer>> programm) {

    public Werk {
        for (List<Integer> zeile : programm.values()) {
            if (zeile.size() != wochen.size()) {
                throw new IllegalArgumentException("Produktionsprogramm passt nicht zum Planungshorizont");
            }
        }
    }

    public Teil findeTeil(String id) {
        return teile.stream().filter(t -> t.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekanntes Teil: " + id));
    }

    public Lieferant findeLieferant(String id) {
        return lieferanten.stream().filter(l -> l.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Lieferant: " + id));
    }

    /** Menge eines Teils pro Fahrzeug der Variante, 0 wenn nicht verbaut. */
    public int mengeProFahrzeug(String varianteId, String teilId) {
        return stueckliste.getOrDefault(varianteId, Map.of()).getOrDefault(teilId, 0);
    }

    /** true, wenn das Teil nur als Vorprodukt eines anderen Teils gebraucht wird. */
    public boolean istVorprodukt(String teilId) {
        return teile.stream().anyMatch(t -> t.vorprodukt() != null && t.vorprodukt().teilId().equals(teilId));
    }
}
