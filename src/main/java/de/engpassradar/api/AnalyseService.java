package de.engpassradar.api;

import de.engpassradar.daten.BeispielWerk;
import de.engpassradar.engine.RisikoAnalyse;
import de.engpassradar.engine.Simulation;
import de.engpassradar.ergebnis.Ergebnis;
import de.engpassradar.ergebnis.RisikoEintrag;
import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Werk;
import de.engpassradar.modell.Zuteilungsregel;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Haelt die Stammdaten und fuehrt Simulationen aus. Die Engine selbst ist frei von Jakarta EE. */
@ApplicationScoped
public class AnalyseService {

    private final Werk werk = BeispielWerk.erstellen();
    private final List<Szenario> szenarien = BeispielWerk.szenarien();

    public Werk werk() {
        return werk;
    }

    public List<Szenario> szenarien() {
        return szenarien;
    }

    public Optional<Szenario> findeSzenario(String id) {
        return szenarien.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    public Ergebnis analysiere(Szenario szenario, Zuteilungsregel regel) {
        return Simulation.berechne(werk, szenario, regel);
    }

    public List<RisikoEintrag> risiko(int dauer) {
        return RisikoAnalyse.bewerte(werk, dauer);
    }
}
