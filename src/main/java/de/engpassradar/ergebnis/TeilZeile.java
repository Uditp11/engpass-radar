package de.engpassradar.ergebnis;

import java.util.List;

/** Ein Teil mit seinem Verlauf ueber den Planungshorizont. */
public record TeilZeile(String teilId, String name, String lieferantId, String lieferantName, int tier,
                        List<TeilWoche> wochen) {
}
