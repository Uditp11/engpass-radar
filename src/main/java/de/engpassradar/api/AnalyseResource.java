package de.engpassradar.api;

import de.engpassradar.ergebnis.Ergebnis;
import de.engpassradar.ergebnis.RisikoEintrag;
import de.engpassradar.modell.Szenario;
import de.engpassradar.modell.Werk;
import de.engpassradar.modell.Zuteilungsregel;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * REST-Schnittstelle.
 * <pre>
 * GET /api/stammdaten
 * GET /api/szenarien
 * GET /api/analyse?szenario=halbleiter&amp;regel=RELATIVER_DECKUNGSBEITRAG
 * GET /api/analyse?lieferant=L2&amp;start=3&amp;dauer=2&amp;rest=40     (eigenes Szenario)
 * GET /api/risiko?dauer=3
 * </pre>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class AnalyseResource {

    @Inject
    AnalyseService service;

    @GET
    @Path("stammdaten")
    public Werk stammdaten() {
        return service.werk();
    }

    @GET
    @Path("szenarien")
    public List<Szenario> szenarien() {
        return service.szenarien();
    }

    @GET
    @Path("analyse")
    public Ergebnis analyse(@QueryParam("szenario") String szenarioId,
                            @QueryParam("lieferant") String lieferantId,
                            @QueryParam("start") @DefaultValue("0") int start,
                            @QueryParam("dauer") @DefaultValue("1") int dauer,
                            @QueryParam("rest") @DefaultValue("0") int rest,
                            @QueryParam("regel") @DefaultValue("PROPORTIONAL") String regel) {
        Zuteilungsregel zuteilung = parseRegel(regel);
        Szenario szenario;
        if (lieferantId != null) {
            service.werk().lieferanten().stream().filter(l -> l.id().equals(lieferantId)).findFirst()
                    .orElseThrow(() -> new NotFoundException("Unbekannter Lieferant: " + lieferantId));
            if (start + dauer > service.werk().wochen().size()) {
                throw new BadRequestException("Szenario reicht ueber den Planungshorizont hinaus");
            }
            try {
                szenario = new Szenario("eigenes", "Eigenes Szenario", lieferantId, start, dauer, rest);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(e.getMessage());
            }
        } else {
            String id = szenarioId == null ? "basis" : szenarioId;
            szenario = service.findeSzenario(id)
                    .orElseThrow(() -> new NotFoundException("Unbekanntes Szenario: " + id));
        }
        return service.analysiere(szenario, zuteilung);
    }

    @GET
    @Path("risiko")
    public List<RisikoEintrag> risiko(@QueryParam("dauer") @DefaultValue("3") int dauer) {
        if (dauer < 1 || dauer > service.werk().wochen().size()) {
            throw new BadRequestException("Dauer muss zwischen 1 und der Anzahl Planungswochen liegen");
        }
        return service.risiko(dauer);
    }

    private static Zuteilungsregel parseRegel(String regel) {
        try {
            return Zuteilungsregel.valueOf(regel);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unbekannte Zuteilungsregel: " + regel);
        }
    }
}
