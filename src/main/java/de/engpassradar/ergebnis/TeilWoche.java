package de.engpassradar.ergebnis;

import de.engpassradar.modell.Ampel;

/**
 * Kennzahlen eines Teils in einer Woche.
 *
 * @param geliefert  Teil der laufenden Lieferung, der in den Bedarf dieser Woche geht (ohne Nachfuellen des Puffers)
 * @param auslastung Bedarf / Kapazitaet, null wenn die Kapazitaet 0 ist (Totalausfall)
 */
public record TeilWoche(int bedarf, int kapazitaet, int geliefert, int ausLager, int fehlmenge,
                        int bestandEnde, Double auslastung, Ampel ampel) {
}
