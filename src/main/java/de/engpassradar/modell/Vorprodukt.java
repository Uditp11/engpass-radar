package de.engpassradar.modell;

/**
 * Ein Teil, das ein Lieferant selbst braucht, um sein Teil zu fertigen (zweite Stufe im Netzwerk).
 *
 * @param teilId ID des Vorprodukts
 * @param menge  benoetigte Menge pro gefertigter Einheit
 */
public record Vorprodukt(String teilId, int menge) {
}
