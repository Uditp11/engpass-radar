package de.engpassradar.modell;

/**
 * Ein Teil mit Lieferant, Wochenkapazitaet und Sicherheitsbestand.
 *
 * @param kapazitaetProWoche maximale Liefermenge pro Woche
 * @param sicherheitsbestand Startbestand und Zielbestand des Puffers
 * @param vorprodukt         optional (null), wenn das Teil ein Vorprodukt einer zweiten Stufe braucht
 */
public record Teil(String id, String name, String lieferantId, int kapazitaetProWoche,
                   int sicherheitsbestand, Vorprodukt vorprodukt) {
}
