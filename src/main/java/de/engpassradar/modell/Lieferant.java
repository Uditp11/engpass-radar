package de.engpassradar.modell;

/**
 * Ein Lieferant im Netzwerk.
 *
 * @param tier                       1 = liefert direkt ans Werk, 2 = liefert an einen Tier-1-Lieferanten
 * @param ausfallwahrscheinlichkeit  geschaetzte Wahrscheinlichkeit pro Jahr fuer einen mehrwoechigen Ausfall (fiktiv)
 */
public record Lieferant(String id, String name, int tier, double ausfallwahrscheinlichkeit) {
}
