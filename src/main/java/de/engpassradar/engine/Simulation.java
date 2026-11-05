package de.engpassradar.engine;

import de.engpassradar.ergebnis.Ergebnis;
import de.engpassradar.ergebnis.Kennzahlen;
import de.engpassradar.ergebnis.TeilWoche;
import de.engpassradar.ergebnis.TeilZeile;
import de.engpassradar.ergebnis.VarianteZeile;
import de.engpassradar.modell.Ampel;
import de.engpassradar.modell.Lieferant;
import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Teil;
import de.engpassradar.modell.Variante;
import de.engpassradar.modell.Werk;
import de.engpassradar.modell.Zuteilungsregel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wochenweise Simulation: Stuecklistenaufloesung, Abgleich mit Lieferantenkapazitaet und
 * Sicherheitsbestand, Vorprodukte der zweiten Stufe, Zuteilung auf Varianten, Schadenspotenzial.
 *
 * Bewusste Vereinfachungen: nicht gebaute Fahrzeuge werden nicht nachgeholt, Lieferzeiten sind null,
 * jedes Vorprodukt gehoert zu genau einem Teil.
 */
public final class Simulation {

    static final double AUSLASTUNG_GELB = 0.85;

    private Simulation() {
    }

    public static Ergebnis berechne(Werk werk, Szenario szenario, Zuteilungsregel regel) {
        int wochenAnzahl = werk.wochen().size();

        Map<String, Integer> bestand = new HashMap<>();
        werk.teile().forEach(t -> bestand.put(t.id(), t.sicherheitsbestand()));

        Map<String, List<TeilWoche>> verlauf = new LinkedHashMap<>();
        werk.teile().forEach(t -> verlauf.put(t.id(), new ArrayList<>()));

        Map<String, List<Integer>> gebautJeVariante = new LinkedHashMap<>();
        werk.varianten().forEach(v -> gebautJeVariante.put(v.id(), new ArrayList<>()));

        for (int w = 0; w < wochenAnzahl; w++) {
            Map<String, Integer> geplant = new LinkedHashMap<>();
            for (Variante v : werk.varianten()) {
                geplant.put(v.id(), werk.programm().get(v.id()).get(w));
            }

            Map<String, Integer> bedarf = new LinkedHashMap<>();
            Map<String, Integer> verfuegbar = new LinkedHashMap<>();
            Map<String, int[]> zwischenwerte = new HashMap<>();

            for (Teil teil : werk.teile()) {
                if (werk.istVorprodukt(teil.id())) {
                    continue; // wird zusammen mit dem uebergeordneten Teil berechnet
                }
                int b = bruttobedarf(werk, teil.id(), geplant);
                int best = bestand.get(teil.id());
                int wunsch = b + Math.max(0, teil.sicherheitsbestand() - best);
                int kap = kapazitaet(werk, teil, szenario, w);

                int liefermenge;
                if (teil.vorprodukt() == null) {
                    liefermenge = Math.min(kap, wunsch);
                } else {
                    liefermenge = fertigeMitVorprodukt(werk, teil, Math.min(kap, wunsch), szenario, w,
                            bestand, verlauf);
                }

                int zumBedarf = Math.min(liefermenge, b);
                int nachfuellen = liefermenge - zumBedarf;
                int ausLager = Math.min(best, b - zumBedarf);
                int verf = zumBedarf + ausLager;

                bestand.put(teil.id(), best - ausLager + nachfuellen);
                bedarf.put(teil.id(), b);
                verfuegbar.put(teil.id(), verf);
                zwischenwerte.put(teil.id(), new int[]{b, kap, zumBedarf, ausLager, b - verf});
            }

            Map<String, Integer> gebaut = Zuteilung.berechne(werk, geplant, bedarf, verfuegbar, regel);
            gebaut.forEach((v, n) -> gebautJeVariante.get(v).add(n));

            // Nicht verbaute Teile bleiben im Werk und erhoehen den Bestand.
            for (String teilId : verfuegbar.keySet()) {
                int verbraucht = 0;
                for (Variante v : werk.varianten()) {
                    verbraucht += werk.mengeProFahrzeug(v.id(), teilId) * gebaut.get(v.id());
                }
                bestand.merge(teilId, verfuegbar.get(teilId) - verbraucht, Integer::sum);
                int[] z = zwischenwerte.get(teilId);
                verlauf.get(teilId).add(teilWoche(z[0], z[1], z[2], z[3], z[4], bestand.get(teilId)));
            }
        }

        return zusammenfassen(werk, szenario, regel, verlauf, gebautJeVariante);
    }

    /**
     * Ein Tier-1-Lieferant kann nur so viel fertigen, wie sein Vorprodukt erlaubt.
     * Erst die laufende Kapazitaet des Vorprodukts, dann dessen Puffer.
     */
    private static int fertigeMitVorprodukt(Werk werk, Teil teil, int gewuenscht, Szenario szenario, int w,
                                            Map<String, Integer> bestand, Map<String, List<TeilWoche>> verlauf) {
        Teil vp = werk.findeTeil(teil.vorprodukt().teilId());
        int m = teil.vorprodukt().menge();
        int vpKap = kapazitaet(werk, vp, szenario, w);
        int vpBest = bestand.get(vp.id());

        int vpBedarf = gewuenscht * m;
        int vpAusKapazitaet = Math.min(vpKap, vpBedarf);
        int vpAusLager = Math.min(vpBest, vpBedarf - vpAusKapazitaet);
        int vpVerfuegbar = vpAusKapazitaet + vpAusLager;

        int gefertigt = vpVerfuegbar / m;
        int rest = vpVerfuegbar - gefertigt * m;
        vpBest = vpBest - vpAusLager + rest;
        vpBest += Math.min(vpKap - vpAusKapazitaet, Math.max(0, vp.sicherheitsbestand() - vpBest));
        bestand.put(vp.id(), vpBest);

        verlauf.get(vp.id()).add(teilWoche(vpBedarf, vpKap, vpAusKapazitaet, vpAusLager,
                vpBedarf - vpVerfuegbar, vpBest));
        return gefertigt;
    }

    static int bruttobedarf(Werk werk, String teilId, Map<String, Integer> geplant) {
        int summe = 0;
        for (Map.Entry<String, Integer> e : geplant.entrySet()) {
            summe += e.getValue() * werk.mengeProFahrzeug(e.getKey(), teilId);
        }
        return summe;
    }

    static int kapazitaet(Werk werk, Teil teil, Szenario szenario, int woche) {
        if (szenario.betrifft(teil.lieferantId(), woche)) {
            return teil.kapazitaetProWoche() * szenario.restkapazitaetProzent() / 100;
        }
        return teil.kapazitaetProWoche();
    }

    static TeilWoche teilWoche(int bedarf, int kap, int geliefert, int ausLager, int fehl, int bestandEnde) {
        Double auslastung = kap == 0 ? null : bedarf / (double) kap;
        Ampel ampel;
        if (fehl > 0) {
            ampel = Ampel.ROT;
        } else if (ausLager > 0 || auslastung == null || auslastung > AUSLASTUNG_GELB) {
            ampel = Ampel.GELB;
        } else {
            ampel = Ampel.GRUEN;
        }
        return new TeilWoche(bedarf, kap, geliefert, ausLager, fehl, bestandEnde, auslastung, ampel);
    }

    private static Ergebnis zusammenfassen(Werk werk, Szenario szenario, Zuteilungsregel regel,
                                           Map<String, List<TeilWoche>> verlauf,
                                           Map<String, List<Integer>> gebautJeVariante) {
        List<TeilZeile> teile = new ArrayList<>();
        Set<String> engpassTeile = new LinkedHashSet<>();
        int ersteEngpassWoche = Integer.MAX_VALUE;

        for (Teil t : werk.teile()) {
            Lieferant l = werk.findeLieferant(t.lieferantId());
            List<TeilWoche> wochen = verlauf.get(t.id());
            teile.add(new TeilZeile(t.id(), t.name(), l.id(), l.name(), l.tier(), wochen));
            for (int w = 0; w < wochen.size(); w++) {
                if (wochen.get(w).fehlmenge() > 0) {
                    engpassTeile.add(t.name());
                    ersteEngpassWoche = Math.min(ersteEngpassWoche, w);
                }
            }
        }

        List<VarianteZeile> varianten = new ArrayList<>();
        int geplantSumme = 0;
        int gebautSumme = 0;
        long schaden = 0;
        for (Variante v : werk.varianten()) {
            List<Integer> geplant = werk.programm().get(v.id());
            List<Integer> gebaut = gebautJeVariante.get(v.id());
            varianten.add(new VarianteZeile(v.id(), v.name(), geplant, gebaut));
            for (int w = 0; w < geplant.size(); w++) {
                int fehlend = geplant.get(w) - gebaut.get(w);
                geplantSumme += geplant.get(w);
                gebautSumme += gebaut.get(w);
                schaden += (long) fehlend * v.deckungsbeitrag();
            }
        }

        String ersteWoche = ersteEngpassWoche == Integer.MAX_VALUE ? null : werk.wochen().get(ersteEngpassWoche);
        Kennzahlen kennzahlen = new Kennzahlen(geplantSumme, gebautSumme, geplantSumme - gebautSumme, schaden,
                ersteWoche, List.copyOf(engpassTeile));
        return new Ergebnis(szenario, regel, werk.wochen(), teile, varianten, kennzahlen);
    }
}
