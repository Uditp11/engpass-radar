package de.engpassradar.ergebnis;

import java.util.List;

/** Geplante und tatsaechlich baubare Fahrzeuge einer Variante je Woche. */
public record VarianteZeile(String varianteId, String name, List<Integer> geplant, List<Integer> gebaut) {
}
