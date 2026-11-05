package de.engpassradar.ergebnis;

import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Zuteilungsregel;

import java.util.List;

/** Ergebnis einer Simulation fuer ein Szenario und eine Zuteilungsregel. */
public record Ergebnis(Szenario szenario, Zuteilungsregel regel, List<String> wochen,
                       List<TeilZeile> teile, List<VarianteZeile> varianten, Kennzahlen kennzahlen) {
}
