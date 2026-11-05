package de.engpassradar.daten;

import de.engpassradar.modell.Lieferant;
import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Teil;
import de.engpassradar.modell.Variante;
import de.engpassradar.modell.Vorprodukt;
import de.engpassradar.modell.Werk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fiktives Beispielwerk: drei Varianten, sieben Teile (davon ein Tier-2-Vorprodukt), sechs Lieferanten,
 * zwoelf Planungswochen mit Werksferien in KW 52 und KW 01.
 * Alle Namen und Zahlen sind erfunden.
 */
public final class BeispielWerk {

    public static final String KOMPAKT_BENZIN = "KOMPAKT_B";
    public static final String KOMPAKT_ELEKTRO = "KOMPAKT_E";
    public static final String SUV_ELEKTRO = "SUV_E";

    private BeispielWerk() {
    }

    public static Werk erstellen() {
        List<String> wochen = List.of("KW 47", "KW 48", "KW 49", "KW 50", "KW 51", "KW 52",
                "KW 01", "KW 02", "KW 03", "KW 04", "KW 05", "KW 06");

        List<Variante> varianten = List.of(
                new Variante(KOMPAKT_BENZIN, "Kompakt Benzin", 2_800),
                new Variante(KOMPAKT_ELEKTRO, "Kompakt Elektro", 4_200),
                new Variante(SUV_ELEKTRO, "SUV Elektro", 6_500));

        List<Lieferant> lieferanten = List.of(
                new Lieferant("L1", "Elektronikwerk Nord", 1, 0.05),
                new Lieferant("L2", "Zellwerk Ost", 1, 0.08),
                new Lieferant("L3", "Kabeltechnik Sued", 1, 0.04),
                new Lieferant("L4", "Sitzsysteme West", 1, 0.03),
                new Lieferant("L5", "Halbleiter AG", 2, 0.15),
                new Lieferant("L6", "Antriebe Mitte", 1, 0.06));

        List<Teil> teile = List.of(
                new Teil("T01", "Steuergeraet", "L1", 1_900, 1_200, new Vorprodukt("T07", 4)),
                new Teil("T02", "Batteriemodul", "L2", 4_500, 3_000, null),
                new Teil("T03", "Kabelbaum", "L3", 1_100, 1_200, null),
                new Teil("T04", "Sitzsatz", "L4", 1_200, 800, null),
                new Teil("T05", "Elektromotor", "L6", 1_000, 500, null),
                new Teil("T06", "Getriebe", "L6", 500, 300, null),
                new Teil("T07", "Mikrochip", "L5", 8_000, 10_000, null));

        // Stueckliste: Menge je Teil pro Fahrzeug
        Map<String, Map<String, Integer>> stueckliste = new LinkedHashMap<>();
        stueckliste.put(KOMPAKT_BENZIN, positionen("T01", 1, "T03", 1, "T04", 1, "T06", 1));
        stueckliste.put(KOMPAKT_ELEKTRO, positionen("T01", 2, "T02", 6, "T03", 1, "T04", 1, "T05", 1));
        stueckliste.put(SUV_ELEKTRO, positionen("T01", 3, "T02", 10, "T03", 1, "T04", 1, "T05", 2));

        // Produktionsprogramm je Woche, Werksferien in KW 52 und KW 01, Hochlauf beim SUV
        Map<String, List<Integer>> programm = new LinkedHashMap<>();
        programm.put(KOMPAKT_BENZIN, List.of(400, 400, 400, 400, 400, 200, 200, 400, 400, 400, 400, 400));
        programm.put(KOMPAKT_ELEKTRO, List.of(300, 300, 300, 300, 300, 150, 150, 300, 300, 300, 300, 300));
        programm.put(SUV_ELEKTRO, List.of(200, 210, 220, 230, 240, 120, 125, 250, 260, 260, 260, 260));

        return new Werk("Beispielwerk (fiktiv)", wochen, varianten, teile, lieferanten, stueckliste, programm);
    }

    private static Map<String, Integer> positionen(Object... teilUndMenge) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < teilUndMenge.length; i += 2) {
            m.put((String) teilUndMenge[i], (Integer) teilUndMenge[i + 1]);
        }
        return m;
    }

    /** Vordefinierte Szenarien fuer die Oberflaeche und die statische Demo. */
    public static List<Szenario> szenarien() {
        return List.of(
                Szenario.basis(),
                new Szenario("halbleiter", "Halbleiter AG (Tier 2) faellt ab KW 49 drei Wochen komplett aus",
                        "L5", 2, 3, 0),
                new Szenario("zellwerk", "Zellwerk Ost liefert ab KW 02 zwei Wochen nur 30 Prozent",
                        "L2", 7, 2, 30),
                new Szenario("kabel", "Kabeltechnik Sued faellt in KW 50 eine Woche aus",
                        "L3", 3, 1, 0),
                new Szenario("antriebe", "Antriebe Mitte liefert ab KW 03 vier Wochen nur 50 Prozent",
                        "L6", 8, 4, 50));
    }
}
