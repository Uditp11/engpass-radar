package de.engpassradar.modell;

/** Bewertung eines Teils in einer Woche. */
public enum Ampel {
    /** Bedarf gedeckt, Auslastung bis 85 Prozent. */
    GRUEN,
    /** Bedarf gedeckt, aber Auslastung ueber 85 Prozent, Sicherheitsbestand angegriffen oder Kapazitaet 0. */
    GELB,
    /** Fehlmenge: Bedarf nicht gedeckt. */
    ROT
}
