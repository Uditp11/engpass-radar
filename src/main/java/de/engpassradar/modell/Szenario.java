package de.engpassradar.modell;

/**
 * Ein Stoerungsszenario: Ein Lieferant liefert ab einer Woche fuer eine bestimmte Dauer
 * nur noch einen Teil seiner Kapazitaet.
 *
 * @param lieferantId           betroffener Lieferant, null fuer das Basisszenario ohne Stoerung
 * @param startWoche            Index der ersten betroffenen Woche (0 = erste Planungswoche)
 * @param dauer                 Anzahl betroffener Wochen
 * @param restkapazitaetProzent verbleibende Kapazitaet in Prozent (0 = Totalausfall)
 */
public record Szenario(String id, String titel, String lieferantId, int startWoche, int dauer,
                       int restkapazitaetProzent) {

    public static Szenario basis() {
        return new Szenario("basis", "Basisplan ohne Stoerung", null, 0, 0, 100);
    }

    public Szenario {
        if (restkapazitaetProzent < 0 || restkapazitaetProzent > 100) {
            throw new IllegalArgumentException("Restkapazitaet muss zwischen 0 und 100 Prozent liegen");
        }
        if (startWoche < 0 || dauer < 0) {
            throw new IllegalArgumentException("Startwoche und Dauer duerfen nicht negativ sein");
        }
    }

    /** true, wenn dieser Lieferant in dieser Woche gestoert ist. */
    public boolean betrifft(String lieferant, int woche) {
        return lieferantId != null && lieferantId.equals(lieferant)
                && woche >= startWoche && woche < startWoche + dauer;
    }
}
