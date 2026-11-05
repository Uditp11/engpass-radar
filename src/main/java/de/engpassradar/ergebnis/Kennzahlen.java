package de.engpassradar.ergebnis;

import java.util.List;

/**
 * Zusammenfassung eines Szenarios.
 *
 * @param schadenspotenzial entgangene Deckungsbeitraege in Euro
 * @param ersteEngpassWoche erste Woche mit Fehlmenge, null wenn es keine gibt
 */
public record Kennzahlen(int geplant, int gebaut, int nichtGebaut, long schadenspotenzial,
                         String ersteEngpassWoche, List<String> engpassTeile) {
}
