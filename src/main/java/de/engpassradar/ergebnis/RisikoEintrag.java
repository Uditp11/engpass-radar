package de.engpassradar.ergebnis;

import java.util.List;

/**
 * Bewertung eines Lieferanten: Was kostet sein Ausfall im schlimmsten Zeitpunkt?
 *
 * @param maxSchaden              Schadenspotenzial im schlimmsten Startzeitpunkt, proportionale Zuteilung
 * @param maxSchadenMitMassnahme  derselbe Ausfall mit Zuteilung nach relativem Deckungsbeitrag
 * @param erwarteterSchaden       Ausfallwahrscheinlichkeit x maxSchaden
 */
public record RisikoEintrag(String lieferantId, String name, int tier, List<String> teile,
                            double ausfallwahrscheinlichkeit, String kritischeStartwoche,
                            long maxSchaden, long maxSchadenMitMassnahme, long erwarteterSchaden) {
}
