"use strict";
// Engpass-Radar: Weboberflaeche "Wetterkarte".
// Laedt die Ergebnisse von der REST-API (Payara) oder, ohne Server, aus den statischen JSON-Dateien in docs/data.
// Werksferien stehen nicht in den Stammdaten, sie sind im Beispielwerk fest auf KW 52 und KW 01 gelegt.
const WERKSFERIEN = new Set(["KW 52", "KW 01"]);
const SYMBOL = { GRUEN: "✓", GELB: "▲", ROT: "✕" };
const AMPEL_TEXT = { GRUEN: "gruen", GELB: "gelb", ROT: "rot" };
const zahl = new Intl.NumberFormat("de-DE");
const eur = (n) => `${zahl.format(n)} EUR`;
const prozent = (x) => `${zahl.format(Math.round(x * 100))} %`;
function esc(s) {
    return s.replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);
}
function el(id) {
    const e = document.getElementById(id);
    if (!e)
        throw new Error(`Element #${id} fehlt`);
    return e;
}
// ---------- Daten laden: erst die API, sonst statische Dateien ----------
// Auf GitHub Pages gibt es keine API; dort gar nicht erst anfragen, damit keine 404 in der Konsole landen.
let quelle = location.hostname.endsWith("github.io") ? "statisch" : "api";
async function laden(api, datei) {
    if (quelle === "api") {
        try {
            const antwort = await fetch(api);
            if (antwort.ok)
                return (await antwort.json());
        }
        catch {
            // kein Server erreichbar
        }
        quelle = "statisch";
    }
    const antwort = await fetch(datei);
    if (!antwort.ok)
        throw new Error(`${datei}: HTTP ${antwort.status}`);
    return (await antwort.json());
}
const cache = new Map();
function analyse(szenario, regel) {
    const schluessel = `${szenario}_${regel}`;
    let p = cache.get(schluessel);
    if (!p) {
        p = laden(`api/analyse?szenario=${encodeURIComponent(szenario)}&regel=${regel}`, `data/analyse/${schluessel}.json`);
        cache.set(schluessel, p);
    }
    return p;
}
// ---------- Darstellung ----------
function kpis(k) {
    const kachel = (titel, wert, warnung = false) => `<div class="kpi${warnung ? " warnung" : ""}"><div class="kpi-titel">${titel}</div><div class="kpi-wert">${wert}</div></div>`;
    return [
        kachel("geplante Fahrzeuge", zahl.format(k.geplant)),
        kachel("nicht baubar", zahl.format(k.nichtGebaut), k.nichtGebaut > 0),
        kachel("Schadenspotenzial", eur(k.schadenspotenzial), k.schadenspotenzial > 0),
        kachel("erste Engpass-KW", k.ersteEngpassWoche ?? "keine", k.ersteEngpassWoche !== undefined),
        kachel("Engpassteile", k.engpassTeile.length ? esc(k.engpassTeile.join(", ")) : "keine", k.engpassTeile.length > 0),
    ].join("");
}
/** Vorprodukte (Tier 2) direkt unter ihrem uebergeordneten Teil einsortieren. */
function zeilenReihenfolge(teile, werk) {
    const kinder = new Map(); // Teil -> Vorprodukt
    for (const t of werk.teile)
        if (t.vorprodukt)
            kinder.set(t.id, t.vorprodukt.teilId);
    const vorprodukte = new Set(kinder.values());
    const ergebnis = [];
    for (const t of teile) {
        if (vorprodukte.has(t.teilId))
            continue;
        ergebnis.push({ zeile: t, eingerueckt: false });
        const kind = teile.find((x) => x.teilId === kinder.get(t.teilId));
        if (kind)
            ergebnis.push({ zeile: kind, eingerueckt: true });
    }
    return ergebnis;
}
function wochenKopf(wochen, erste) {
    const zellen = wochen.map((kw) => WERKSFERIEN.has(kw)
        ? `<th class="ferien" scope="col">${kw}<span class="ferien-tag">Ferien</span></th>`
        : `<th scope="col">${kw}</th>`);
    return `<thead><tr><th scope="col" class="erste">${erste}</th>${zellen.join("")}</tr></thead>`;
}
function karte(e, werk) {
    const s = e.szenario;
    const zeilen = zeilenReihenfolge(e.teile, werk).map(({ zeile, eingerueckt }) => {
        const zellen = zeile.wochen.map((w, i) => {
            const kw = e.wochen[i];
            const gestoert = zeile.lieferantId === s.lieferantId && i >= s.startWoche && i < s.startWoche + s.dauer;
            const text = w.kapazitaet === 0 ? "Ausfall" : prozent(w.auslastung ?? 0);
            const titel = [
                `${zeile.name}, ${kw}: ${AMPEL_TEXT[w.ampel]}`,
                `Bedarf: ${zahl.format(w.bedarf)}`,
                `Kapazitaet: ${zahl.format(w.kapazitaet)}`,
                `aus Lager: ${zahl.format(w.ausLager)}`,
                `Fehlmenge: ${zahl.format(w.fehlmenge)}`,
                `Bestand: ${zahl.format(w.bestandEnde)}`,
            ].join("\n");
            const klassen = [`zelle`, `a-${w.ampel}`, WERKSFERIEN.has(kw) ? "ferien" : "", gestoert ? "gestoert" : ""];
            return `<td class="${klassen.join(" ").trim()}" title="${esc(titel)}">`
                + `<span class="symbol" aria-hidden="true">${SYMBOL[w.ampel]}</span> ${text}`
                + `<span class="sr">, ${AMPEL_TEXT[w.ampel]}, Fehlmenge ${zahl.format(w.fehlmenge)}</span></td>`;
        });
        const tag = zeile.tier === 2 ? ` <span class="tag">Tier 2</span>` : "";
        return `<tr${eingerueckt ? ` class="vorprodukt"` : ""}><th scope="row" class="erste">`
            + `<div class="teil">${eingerueckt ? `<span aria-hidden="true">↳ </span>` : ""}${esc(zeile.name)}${tag}</div>`
            + `<div class="lieferant">${esc(zeile.lieferantName)}</div></th>${zellen.join("")}</tr>`;
    });
    return `<table class="tabelle karte">${wochenKopf(e.wochen, "Teil / Lieferant")}<tbody>${zeilen.join("")}</tbody></table>`;
}
function fahrzeuge(e) {
    const zeilen = e.varianten.map((v) => {
        const zellen = v.gebaut.map((g, i) => {
            const p = v.geplant[i];
            const ferien = WERKSFERIEN.has(e.wochen[i]) ? " ferien" : "";
            return g < p
                ? `<td class="zahl kurz${ferien}" title="${p - g} Fahrzeuge fehlen">${zahl.format(g)}<span class="geplant">von ${zahl.format(p)}</span></td>`
                : `<td class="zahl${ferien}">${zahl.format(g)}</td>`;
        });
        const summeGeplant = v.geplant.reduce((a, b) => a + b, 0);
        const summeGebaut = v.gebaut.reduce((a, b) => a + b, 0);
        const summe = summeGebaut < summeGeplant
            ? `<td class="zahl summe kurz">${zahl.format(summeGebaut)}<span class="geplant">von ${zahl.format(summeGeplant)}</span></td>`
            : `<td class="zahl summe">${zahl.format(summeGebaut)}</td>`;
        return `<tr><th scope="row" class="erste">${esc(v.name)}</th>${zellen.join("")}${summe}</tr>`;
    });
    const kopf = wochenKopf(e.wochen, "Variante").replace("</tr></thead>", `<th scope="col">Summe</th></tr></thead>`);
    return `<table class="tabelle fahrzeuge">${kopf}<tbody>${zeilen.join("")}</tbody></table>`;
}
function massnahme(prop, rdb) {
    const a = prop.kennzahlen;
    const b = rdb.kennzahlen;
    const ersparnis = a.schadenspotenzial - b.schadenspotenzial;
    const spalte = (titel, k) => `<div class="vergleich"><div class="kpi-titel">${titel}</div><div class="kpi-wert">${eur(k.schadenspotenzial)}</div>`
        + `<div class="klein">${zahl.format(k.nichtGebaut)} Fahrzeuge nicht baubar</div></div>`;
    const fazit = a.schadenspotenzial === 0
        ? `<p class="fazit">Kein Schaden in diesem Szenario: Puffer und Kapazitaet reichen, eine Massnahme ist nicht noetig.</p>`
        : `<div class="vergleich ersparnis"><div class="kpi-titel">Ersparnis</div><div class="kpi-wert">${eur(ersparnis)}</div>`
            + `<div class="klein">${zahl.format(a.nichtGebaut - b.nichtGebaut)} Fahrzeuge mehr gebaut</div></div>`;
    return `<h2>Wirkung der Massnahme</h2>`
        + `<p class="hinweis">Massnahme: knappe Teile zuerst an die Varianten mit dem hoechsten Deckungsbeitrag pro Engpassteil.</p>`
        + `<div class="vergleiche">${spalte("Proportional", a)}${spalte("Nach relativem Deckungsbeitrag", b)}${fazit}</div>`;
}
function risiko(eintraege) {
    const zeilen = eintraege.map((r) => `<tr>
    <th scope="row" class="erste">${esc(r.name)}</th>
    <td>${r.tier === 2 ? `<span class="tag">Tier 2</span>` : "Tier 1"}</td>
    <td>${esc(r.teile.join(", "))}</td>
    <td class="zahl">${prozent(r.ausfallwahrscheinlichkeit)}</td>
    <td>${esc(r.kritischeStartwoche)}</td>
    <td class="zahl">${eur(r.maxSchaden)}</td>
    <td class="zahl">${eur(r.maxSchadenMitMassnahme)}</td>
    <td class="zahl stark">${eur(r.erwarteterSchaden)}</td></tr>`);
    return `<table class="tabelle risiko"><thead><tr>
    <th scope="col" class="erste">Lieferant</th><th scope="col">Tier</th><th scope="col">Teile</th>
    <th scope="col">Ausfallwahr&shy;scheinlichkeit</th><th scope="col">kritische Start-KW</th>
    <th scope="col">max. Schaden</th><th scope="col">mit Massnahme</th><th scope="col">erwarteter Schaden</th>
    </tr></thead><tbody>${zeilen.join("")}</tbody></table>`;
}
// ---------- Ablauf ----------
async function start() {
    const auswahl = el("szenario");
    const szenarien = await laden("api/szenarien", "data/szenarien.json");
    const [werk, ranking] = await Promise.all([
        laden("api/stammdaten", "data/stammdaten.json"),
        laden("api/risiko", "data/risiko.json"),
    ]);
    auswahl.innerHTML = szenarien.map((s) => `<option value="${esc(s.id)}">${esc(s.titel)}</option>`).join("");
    const ausHash = location.hash.slice(1);
    if (szenarien.some((s) => s.id === ausHash))
        auswahl.value = ausHash;
    el("risiko").innerHTML = risiko(ranking);
    const regel = () => (document.querySelector('input[name="regel"]:checked')?.value ?? "PROPORTIONAL");
    let letzterAufruf = 0;
    async function zeigen() {
        const aufruf = ++letzterAufruf;
        const id = auswahl.value;
        history.replaceState(null, "", `#${id}`);
        const [prop, rdb] = await Promise.all([analyse(id, "PROPORTIONAL"), analyse(id, "RELATIVER_DECKUNGSBEITRAG")]);
        if (aufruf !== letzterAufruf)
            return; // eine neuere Auswahl ist schon unterwegs
        const e = regel() === "PROPORTIONAL" ? prop : rdb;
        el("kpis").innerHTML = kpis(e.kennzahlen);
        el("karte").innerHTML = karte(e, werk);
        el("fahrzeuge").innerHTML = fahrzeuge(e);
        el("massnahme").innerHTML = massnahme(prop, rdb);
        el("quelle").textContent = quelle === "api"
            ? "Datenquelle: REST-API (Jakarta EE auf Payara Micro)"
            : "Datenquelle: statische JSON-Dateien, mit derselben Engine erzeugt";
    }
    auswahl.addEventListener("change", () => void zeigen().catch(fehler));
    document.querySelectorAll('input[name="regel"]').forEach((r) => r.addEventListener("change", () => void zeigen().catch(fehler)));
    await zeigen();
}
function fehler(e) {
    el("kpis").innerHTML = `<p class="fehlermeldung">Daten konnten nicht geladen werden: ${esc(String(e))}</p>`;
}
start().catch(fehler);
