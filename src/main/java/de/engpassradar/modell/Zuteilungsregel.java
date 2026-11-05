package de.engpassradar.modell;

/** Wie knappe Teile auf die Fahrzeugvarianten verteilt werden. */
public enum Zuteilungsregel {
    /** Jede Variante wird im gleichen Verhaeltnis gekuerzt. */
    PROPORTIONAL,
    /** Varianten mit dem hoechsten Deckungsbeitrag pro Einheit des Engpassteils zuerst. */
    RELATIVER_DECKUNGSBEITRAG
}
