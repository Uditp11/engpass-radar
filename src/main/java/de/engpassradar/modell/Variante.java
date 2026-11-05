package de.engpassradar.modell;

/**
 * Eine Fahrzeugvariante im Produktionsprogramm.
 *
 * @param deckungsbeitrag Deckungsbeitrag pro Fahrzeug in Euro (fiktiv)
 */
public record Variante(String id, String name, int deckungsbeitrag) {
}
