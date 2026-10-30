// Builds all Word files for the EDV-Tag "Excel-Adressliste + Word-Serienbrief".
// Usage: node build_docs.js <outRoot>
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Header, Footer, AlignmentType, BorderStyle,
  SimpleField, SimpleMailMergeField, Table, TableRow, TableCell, WidthType, ShadingType,
  LevelFormat, ImageRun, VerticalAlign, HeightRule, LineRuleType,
} = require("docx");

const OUT = process.argv[2];
const DIR_TN = path.join(OUT, "1_Fuer_Teilnehmende_auf_jeden_PC");
const DIR_PRINT = path.join(OUT, "2_Zum_Ausdrucken");
const DIR_TRAINER = path.join(OUT, "3_Fuer_Trainer");

const MM = 56.6929; // twips per millimetre
const mm = (v) => Math.round(v * MM);
const A4 = { width: 11906, height: 16838 };
const FONT = "Arial";
const LINE = 253; // one line of 11pt Arial, single spacing, in twips

const baseStyles = (size) => ({
  default: {
    document: {
      run: { font: FONT, size, language: { value: "de-DE" } },
      paragraph: { spacing: { after: 0 } },
    },
  },
});

// ---------------------------------------------------------------------------
// Letter (Übung / Lösung / zusammengeführtes Beispiel)
// ---------------------------------------------------------------------------

const MAX = {
  name: "Max Mustermann",
  street: "Musterweg 1",
  city: "25813 Husum",
  place: "Husum",
  phone: "0123 456789",
  mail: "max.mustermann@example.com",
};

const RECORDS = [
  { Firma: "Muster Logistik GmbH", Ansprechpartner: "Frau Anna Petersen", Strasse: "Am Musterhafen 12", PLZ: "25813", Ort: "Husum", Stelle: "Lagerhelfer", Briefanrede: "Sehr geehrte Frau Petersen" },
  { Firma: "Bäckerei Beispiel KG", Ansprechpartner: "Herr Jens Hansen", Strasse: "Beispielweg 3", PLZ: "25821", Ort: "Bredstedt", Stelle: "Verkäufer", Briefanrede: "Sehr geehrter Herr Hansen" },
  { Firma: "Pflegedienst Muster gGmbH", Ansprechpartner: "Frau Maria Jensen", Strasse: "Musterstraße 45", PLZ: "25899", Ort: "Niebüll", Stelle: "Pflegehelfer", Briefanrede: "Sehr geehrte Frau Jensen" },
  { Firma: "Hotel Beispiel am Hafen", Ansprechpartner: "", Strasse: "Probeallee 7", PLZ: "25832", Ort: "Tönning", Stelle: "Küchenhilfe", Briefanrede: "Sehr geehrte Damen und Herren" },
  { Firma: "Muster Gebäudeservice GmbH", Ansprechpartner: "Herr Thomas Möller", Strasse: "Beispielring 21", PLZ: "24937", Ort: "Flensburg", Stelle: "Hausmeister", Briefanrede: "Sehr geehrter Herr Möller" },
];

const letterStyles = {
  ...baseStyles(22),
  paragraphStyles: [
    { id: "Anschrift", name: "Anschrift", basedOn: "Normal", next: "Anschrift", quickFormat: true,
      paragraph: { spacing: { after: 0 } } },
    { id: "Betreff", name: "Betreff", basedOn: "Normal", next: "Brieftext", quickFormat: true,
      run: { bold: true }, paragraph: { spacing: { before: 2 * LINE, after: 0 } } },
    { id: "Brieftext", name: "Brieftext", basedOn: "Normal", next: "Brieftext", quickFormat: true,
      paragraph: { spacing: { after: LINE } } },
  ],
};

function sender(mode) {
  if (mode === "uebung") {
    return {
      name: "[Ihr Vorname Nachname]",
      addr: "[Ihre Straße und Hausnummer] · [Ihre PLZ und Ihr Wohnort]",
      contact: "Telefon: [Ihre Telefonnummer] · E-Mail: [Ihre E-Mail-Adresse]",
      place: "[Ihr Wohnort]",
    };
  }
  return {
    name: MAX.name,
    addr: `${MAX.street} · ${MAX.city}`,
    contact: `Telefon: ${MAX.phone} · E-Mail: ${MAX.mail}`,
    place: MAX.place,
  };
}

function letterHeader(mode) {
  const s = sender(mode);
  return new Header({
    children: [
      new Paragraph({ children: [new TextRun({ text: s.name, bold: true, size: 32, color: "1F3864" })] }),
      new Paragraph({
        spacing: { before: 40 },
        border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: "8EAADB", space: 4 } },
        children: [new TextRun({ text: s.addr, size: 20 })],
      }),
    ],
  });
}

function letterFooter(mode) {
  const s = sender(mode);
  return new Footer({
    children: [
      new Paragraph({
        alignment: AlignmentType.CENTER,
        border: { top: { style: BorderStyle.SINGLE, size: 6, color: "8EAADB", space: 4 } },
        children: [new TextRun({ text: s.contact, size: 18, color: "404040" })],
      }),
    ],
  });
}

// One letter body. mode: "uebung" -> [Platzhalter], "loesung" -> MERGEFIELDs, "merged" -> data.
function letterBody(mode, rec) {
  const s = sender(mode);
  const f = (name) => {
    if (mode === "loesung") return new SimpleMailMergeField(name);
    if (mode === "uebung") return new TextRun(`[${name}]`);
    return new TextRun(rec[name]);
  };
  const date = mode === "merged"
    ? new TextRun("02.11.2026")
    : new SimpleField('DATE \\@ "dd.MM.yyyy"', "28.10.2026");

  const out = [];
  // DIN 5008 Form B: the address zone starts 45 mm (top margin) + 17.7 mm from the top edge.
  // A fixed-height empty line instead of "space before", which some programs drop after a page break.
  out.push(new Paragraph({ style: "Anschrift", spacing: { line: mm(17.7), lineRule: LineRuleType.EXACT }, children: [] }));
  out.push(new Paragraph({ style: "Anschrift", children: [f("Firma")] }));
  // In the real merge Word drops this line when the field is empty (blank-line suppression).
  if (!(mode === "merged" && !rec.Ansprechpartner)) {
    out.push(new Paragraph({ style: "Anschrift", children: [f("Ansprechpartner")] }));
  }
  out.push(new Paragraph({ style: "Anschrift", children: [f("Strasse")] }));
  out.push(new Paragraph({ style: "Anschrift", children: [f("PLZ"), new TextRun(" "), f("Ort")] }));

  out.push(new Paragraph({
    style: "Anschrift", alignment: AlignmentType.RIGHT, spacing: { before: mm(15) },
    children: [new TextRun(`${s.place}, `), date],
  }));
  out.push(new Paragraph({ style: "Betreff", children: [new TextRun("Bewerbung als "), f("Stelle")] }));
  out.push(new Paragraph({ style: "Brieftext", spacing: { before: 2 * LINE }, children: [f("Briefanrede"), new TextRun(",")] }));
  out.push(new Paragraph({
    style: "Brieftext",
    children: [
      new TextRun("mit großem Interesse habe ich Ihre Stellenanzeige gelesen. Hiermit bewerbe ich mich als "),
      f("Stelle"),
      new TextRun(" in Ihrem Unternehmen."),
    ],
  }));
  out.push(new Paragraph({
    style: "Brieftext",
    children: [new TextRun("[Hier schreiben Sie in 3 bis 5 Sätzen: Was können Sie? Welche Erfahrung haben Sie? "
      + "Warum passt diese Stelle zu Ihnen? Diesen Teil schreiben Sie für jede Firma neu.]")],
  }));
  out.push(new Paragraph({
    style: "Brieftext",
    children: [new TextRun("Über eine Einladung zu einem persönlichen Gespräch freue ich mich sehr.")],
  }));
  out.push(new Paragraph({ style: "Anschrift", children: [new TextRun("Mit freundlichen Grüßen")] }));
  out.push(new Paragraph({ style: "Anschrift", spacing: { before: 3 * LINE }, children: [new TextRun(s.name)] }));
  out.push(new Paragraph({ style: "Anschrift", spacing: { before: 2 * LINE }, children: [new TextRun("Anlagen")] }));
  out.push(new Paragraph({ style: "Anschrift", children: [new TextRun("Lebenslauf")] }));
  out.push(new Paragraph({ style: "Anschrift", children: [new TextRun("Zeugnisse")] }));
  return out;
}

const letterPage = {
  size: A4,
  margin: { top: mm(45), right: mm(20), bottom: mm(25), left: mm(25), header: mm(12), footer: mm(10) },
};

function letterDoc(mode) {
  // Like Word's own merge output: one section (new page) per letter, so the
  // DIN spacing above the address is kept on every page.
  const bodies = mode === "merged" ? RECORDS.map((rec) => letterBody(mode, rec)) : [letterBody(mode)];
  return new Document({
    creator: "EDV-Tag",
    title: mode === "uebung" ? "Anschreiben – Übung" : mode === "loesung" ? "Anschreiben – Lösung (Serienbrief)" : "Beispiel-Ergebnis",
    styles: letterStyles,
    sections: bodies.map((children) => ({
      properties: { page: letterPage },
      headers: { default: letterHeader(mode) },
      footers: { default: letterFooter(mode) },
      children,
    })),
  });
}

// ---------------------------------------------------------------------------
// Shared helpers for handout, feedback form and trainer plan
// ---------------------------------------------------------------------------

const CONTENT_W = A4.width - 2 * 1134; // 20 mm margins left/right
const docPage = { size: A4, margin: { top: 1134, right: 1134, bottom: 1134, left: 1134, header: 567, footer: 567 } };

// "Text mit **fett**" -> TextRuns
function runs(text, opts = {}) {
  return text.split(/(\*\*[^*]+\*\*)/).filter(Boolean).map((part) =>
    part.startsWith("**")
      ? new TextRun({ ...opts, text: part.slice(2, -2), bold: true })
      : new TextRun({ ...opts, text: part }));
}

const P = (text, opts = {}) => new Paragraph({ spacing: { after: 120 }, ...opts, children: runs(text, opts.run || {}) });
const H1 = (text) => new Paragraph({ spacing: { before: 0, after: 120 }, children: [new TextRun({ text, bold: true, size: 36, color: "1F3864" })] });
const H2 = (text) => new Paragraph({
  spacing: { before: 280, after: 120 }, keepNext: true,
  border: { bottom: { style: BorderStyle.SINGLE, size: 8, color: "1F3864", space: 2 } },
  children: [new TextRun({ text, bold: true, size: 28, color: "1F3864" })],
});
let stepInstance = 0;
const newSteps = () => ++stepInstance;
const Step = (inst, text) => new Paragraph({ numbering: { reference: "steps", level: 0, instance: inst }, spacing: { after: 100 }, children: runs(text) });
const Bullet = (text) => new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 }, children: runs(text) });
const Check = (text) => new Paragraph({ numbering: { reference: "checks", level: 0 }, spacing: { after: 60 }, children: runs(text) });

const numbering = {
  config: [
    { reference: "steps", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.START,
      style: { paragraph: { indent: { left: 454, hanging: 454 } }, run: { bold: true, color: "1F3864" } } }] },
    { reference: "bullets", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.START,
      style: { paragraph: { indent: { left: 360, hanging: 260 } } } }] },
    { reference: "checks", levels: [{ level: 0, format: LevelFormat.BULLET, text: "☐", alignment: AlignmentType.START,
      style: { paragraph: { indent: { left: 400, hanging: 400 } }, run: { font: "Segoe UI Symbol" } } }] },
  ],
};

const cellBorder = { style: BorderStyle.SINGLE, size: 4, color: "A6A6A6" };
const allBorders = { top: cellBorder, bottom: cellBorder, left: cellBorder, right: cellBorder };

// One-cell callout box
function Box(title, lines, fill = "FFF2CC", edge = "BF9000") {
  const b = { style: BorderStyle.SINGLE, size: 4, color: edge };
  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [CONTENT_W],
    rows: [new TableRow({ children: [new TableCell({
      width: { size: CONTENT_W, type: WidthType.DXA },
      shading: { fill, type: ShadingType.CLEAR, color: "auto" },
      borders: { top: b, bottom: b, right: b, left: { style: BorderStyle.SINGLE, size: 24, color: edge } },
      margins: { top: 100, bottom: 100, left: 180, right: 180 },
      children: [
        ...(title ? [new Paragraph({ spacing: { after: 60 }, children: [new TextRun({ text: title, bold: true })] })] : []),
        ...lines.map((l) => (l instanceof Paragraph ? l : new Paragraph({ spacing: { after: 60 }, children: runs(l) }))),
      ],
    })] })],
  });
}

const spacer = (after = 120) => new Paragraph({ spacing: { after }, children: [] });

function Grid(widths, header, rows, opts = {}) {
  const total = widths.reduce((a, b) => a + b, 0);
  const mkCell = (content, i, isHead) => new TableCell({
    width: { size: widths[i], type: WidthType.DXA },
    borders: allBorders,
    verticalAlign: opts.vCenter ? VerticalAlign.CENTER : VerticalAlign.TOP,
    shading: isHead ? { fill: "1F3864", type: ShadingType.CLEAR, color: "auto" } : (opts.zebra && opts._z ? { fill: "F2F2F2", type: ShadingType.CLEAR, color: "auto" } : undefined),
    margins: { top: 60, bottom: 60, left: 100, right: 100 },
    children: (Array.isArray(content) ? content : [content]).map((c) =>
      c instanceof Paragraph ? c : new Paragraph({ children: runs(String(c), isHead ? { bold: true, color: "FFFFFF" } : {}) })),
  });
  const tableRows = [];
  if (header) tableRows.push(new TableRow({ tableHeader: true, children: header.map((h, i) => mkCell(h, i, true)) }));
  rows.forEach((r, idx) => {
    opts._z = idx % 2 === 1;
    tableRows.push(new TableRow({
      cantSplit: true,
      height: opts.rowHeight ? { value: opts.rowHeight, rule: HeightRule.ATLEAST } : undefined,
      children: r.map((c, i) => mkCell(c, i, false)),
    }));
  });
  return new Table({ width: { size: total, type: WidthType.DXA }, columnWidths: widths, rows: tableRows });
}

const pageFooter = (text) => new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text, size: 16, color: "808080" })] })] });

// ---------------------------------------------------------------------------
// Handout for participants
// ---------------------------------------------------------------------------

function handout() {
  const s1 = newSteps(), s2 = newSteps(), s3 = newSteps(), s4 = newSteps(), s5 = newSteps();
  const children = [
    H1("Bewerbungen mit einem Klick"),
    P("Excel-Adressliste + Word-Serienbrief · Anleitung zum EDV-Tag", { run: { size: 24, color: "595959" } }),
    Box("Die Idee", [
      "Was in **jedem Brief gleich** ist (Ihr Name, Ihre Adresse, der Gruß) → schreiben Sie **einmal in Word**.",
      "Was sich **ändert** (Firma, Adresse, Ansprechperson, Stelle) → steht **in Excel**, eine Zeile pro Firma.",
      "Word holt die Daten aus Excel und macht daraus **fertige Briefe**. Das heißt **Serienbrief**.",
    ], "DEEAF6", "2E75B6"),
    spacer(),
    P("**Ihre Dateien** (alle im selben Ordner – bitte nicht verschieben):"),
    Bullet("**Bewerbungsliste.xlsx** – Ihre Liste mit Firmen (Excel)"),
    Bullet("**Anschreiben_Uebung.docx** – Ihr Brief zum Üben (Word)"),
    Bullet("**Anschreiben_Loesung.docx** – fertiges Beispiel, falls Sie nicht weiterkommen"),

    H2("Teil 1 – Excel: die Adressliste"),
    Step(s1, "Öffnen Sie **Bewerbungsliste.xlsx** mit einem Doppelklick."),
    Step(s1, "Steht oben ein gelber Balken **Geschützte Ansicht**? Klicken Sie auf **Bearbeitung aktivieren**."),
    Step(s1, "Jede **Zeile** ist eine Firma. Jede **Spalte** ist eine Information (Firma, Anrede, Straße …)."),
    Step(s1, "Überschreiben Sie die Beispiel-Firmen mit **Ihren** Stellenanzeigen. Füllen Sie nur die **blauen** Spalten aus."),
    Step(s1, "**Anrede:** In die Zelle klicken → kleiner Pfeil rechts → **Frau** oder **Herr**. Kein Name in der Anzeige? Dann Anrede, Vorname und Nachname **leer** lassen."),
    Step(s1, "Die **grauen** Spalten (Ansprechpartner, Briefanrede) füllen sich **von selbst**. Bitte nicht hineinschreiben."),
    Step(s1, "Mehr als 5 Firmen? Markieren Sie die zwei grauen Zellen der letzten Zeile. Ziehen Sie das **kleine Quadrat unten rechts** (Ausfüllkästchen) nach unten – nur so weit, wie Firmen da sind."),
    Step(s1, "Speichern mit **Strg + S**. Dann **Excel schließen** (X oben rechts). Wichtig für Teil 3!"),
    Box("Die 5 goldenen Regeln für eine Adressliste", [
      "1. In **Zeile 1** stehen die Überschriften – kurz und ohne Leerzeichen.",
      "2. **Eine Zeile = eine Firma.**",
      "3. **Keine leeren Zeilen** zwischen den Firmen.",
      "4. **Keine verbundenen Zellen.**",
      "5. Die **PLZ als Text** speichern (sonst wird aus 01067 → 1067). In dieser Liste ist das schon eingestellt.",
    ]),

    H2("Teil 2 – Word: Ihr Brief"),
    Step(s2, "Öffnen Sie **Anschreiben_Uebung.docx** (wenn nötig: **Bearbeitung aktivieren**)."),
    Step(s2, "Machen Sie einen **Doppelklick oben** auf den Briefkopf (**Kopfzeile**). Ersetzen Sie **[Ihr Vorname Nachname]** und **[Ihre …]** durch Ihre Daten."),
    Step(s2, "Unten auf der Seite (**Fußzeile**): Telefon und E-Mail eintragen. Mit einem Doppelklick in den Brief kommen Sie zurück."),
    Step(s2, "Ersetzen Sie **[Ihr Wohnort]** vor dem Datum und **[Ihr Vorname Nachname]** unter dem Gruß."),
    Step(s2, "Speichern mit **Strg + S**."),
    Box("Eckige Klammern – was mache ich?", [
      "**[Ihr …] / [Ihre …]** → tippen Sie **selbst** ein.",
      "**[Firma], [Stelle], [Briefanrede] …** → **nicht abtippen!** Diese Daten kommen gleich aus Excel (Teil 3).",
    ], "E2EFDA", "548235"),

    H2("Teil 3 – Serienbrief: Word mit Excel verbinden"),
    Step(s3, "Oben im Menüband: Registerkarte **Sendungen** → **Seriendruck starten** → **Briefe**."),
    Step(s3, "**Sendungen** → **Empfänger auswählen** → **Vorhandene Liste verwenden …**"),
    Step(s3, "Wählen Sie **Bewerbungsliste.xlsx** → **Öffnen**."),
    Step(s3, "Fenster **Tabelle auswählen**: **Bewerbungen$** anklicken. Der Haken bei **Erste Datenzeile enthält Spaltenüberschriften** muss gesetzt sein → **OK**."),
    Step(s3, "Markieren Sie im Brief **[Firma]** (mit den Klammern). Klicken Sie auf den **unteren Teil** des Knopfs **Seriendruckfeld einfügen** (mit dem kleinen Pfeil) → **Firma**. Jetzt steht dort **«Firma»**."),
    Step(s3, "Machen Sie das Gleiche für alle Felder. Haken Sie ab:"),
    Grid([3200, 3200, 1100], ["Im Brief markieren", "Seriendruckfeld", "Erledigt"], [
      ["[Firma]", "Firma", ""], ["[Ansprechpartner]", "Ansprechpartner", ""], ["[Strasse]", "Strasse", ""],
      ["[PLZ]", "PLZ", ""], ["[Ort]", "Ort", ""], ["[Stelle]  (im Betreff)", "Stelle", ""],
      ["[Stelle]  (im Text)", "Stelle", ""], ["[Briefanrede]", "Briefanrede", ""],
    ]),
    spacer(80),
    Step(s3, "**Sendungen** → **Vorschau Ergebnisse**. Mit den Pfeilen **◄ ►** blättern Sie durch Ihre Firmen. Prüfen Sie: Leerzeichen da? Komma nach der Anrede?"),
    Step(s3, "**Datei** → **Speichern unter** → Name: **Anschreiben_Serienbrief** – im **gleichen Ordner** wie die Excel-Liste."),

    H2("Teil 4 – Alle Briefe mit einem Klick"),
    Step(s4, "**Sendungen** → **Fertig stellen und zusammenführen** → **Einzelne Dokumente bearbeiten …**"),
    Step(s4, "**Alle** → **OK**. Word öffnet ein **neues Dokument** mit allen Briefen – ein Brief pro Seite."),
    Step(s4, "Speichern Sie es: **Datei** → **Speichern unter** → z. B. **Meine_Bewerbungen**."),
    Step(s4, "Schreiben Sie jetzt in **jedem** Brief Ihren eigenen Text (dort, wo **[Hier schreiben Sie …]** steht)."),
    Step(s4, "Für eine E-Mail-Bewerbung: **Datei** → **Speichern unter** → Dateityp **PDF**."),
    Box("Später: nur EIN Brief für eine neue Stelle", [
      "1. Neue Firma in Excel eintragen → speichern → Excel schließen.",
      "2. **Anschreiben_Serienbrief** öffnen. Frage mit **SQL-Befehl** → **Ja**.",
      "3. **Vorschau Ergebnisse** → mit ► zur neuen Firma blättern.",
      "4. **Fertig stellen und zusammenführen** → **Einzelne Dokumente bearbeiten** → **Aktueller Datensatz** → **OK**.",
    ], "DEEAF6", "2E75B6"),

    H2("Teil 5 – Wenn etwas nicht klappt"),
    Grid([3900, 5738], ["Problem", "Lösung"], [
      ["Beim Öffnen kommt eine Frage mit **SQL-Befehl**.", "**Ja** klicken. Das ist normal: Word lädt die Excel-Liste."],
      ["Die Knöpfe unter **Sendungen** sind grau.", "Oben steht **Geschützte Ansicht** → **Bearbeitung aktivieren**."],
      ["Word findet die Excel-Liste nicht.", "Die Datei wurde verschoben oder umbenannt. **Empfänger auswählen** → **Vorhandene Liste verwenden** → Liste neu auswählen."],
      ["Neue Firmen aus Excel fehlen.", "Excel **speichern und schließen**. In Word: **Empfängerliste bearbeiten** → Datenquelle anklicken → **Aktualisieren**."],
      ["Am Ende kommen **leere Briefe**.", "In Excel stehen leere Zeilen mit Formeln. Ganze Zeilen löschen (Rechtsklick auf die Zeilennummer → **Zellen löschen**). Oder in Word: **Empfängerliste bearbeiten** → Pfeil bei **Firma** → **(Nicht leere)**."],
      ["Die **0** am Anfang der PLZ fehlt.", "PLZ-Zelle: Rechtsklick → **Zellen formatieren** → **Text**. PLZ neu eintippen."],
      ["Im Brief steht **«Firma»** statt dem Namen.", "**Vorschau Ergebnisse** anklicken."],
      ["Wörter kleben zusammen.", "Leerzeichen zwischen den Feldern einfügen, z. B. «PLZ» «Ort»."],
      ["Das Datum ändert sich jeden Tag.", "In den fertigen Briefen: **Strg + A**, dann **Strg + Umschalt + F9**. Jetzt ist das Datum fester Text."],
    ]),
    spacer(200),
    Box("Ihre Daten gehören Ihnen", [
      "Speichern Sie Ihre Dateien auf **Ihrem USB-Stick** oder schicken Sie sie **per E-Mail an sich selbst**.",
      "Löschen Sie am Ende Ihre persönlichen Dateien vom Schulungs-PC.",
    ], "FBE5D6", "C55A11"),
  ];
  return new Document({
    creator: "EDV-Tag", title: "Anleitung Serienbrief", numbering,
    styles: baseStyles(24),
    sections: [{ properties: { page: docPage }, footers: { default: pageFooter("EDV-Tag · Excel-Adressliste + Word-Serienbrief") }, children }],
  });
}

// ---------------------------------------------------------------------------
// Feedback form (Bewertung durch TN)
// ---------------------------------------------------------------------------

function feedback(buildDir) {
  const img = (f) => new Paragraph({ alignment: AlignmentType.CENTER, children: [new ImageRun({
    type: "png", data: fs.readFileSync(path.join(buildDir, f)), transformation: { width: 40, height: 40 } })] });
  const label = (t) => new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: t, size: 20, bold: true })] });
  const statements = [
    "Der Tag hat mir gefallen.",
    "Ich habe die Erklärungen verstanden.",
    "Die Anleitung (Handout) hat mir geholfen.",
    "Ich kann jetzt eine Adressliste in Excel machen.",
    "Ich kann jetzt einen Serienbrief in Word machen.",
    "Ich benutze das für meine Bewerbungen.",
  ];
  const line = () => new Paragraph({ spacing: { before: 260 }, border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: "808080", space: 1 } }, children: [] });
  const children = [
    H1("Wie war der EDV-Tag?"),
    P("Excel-Adressliste + Word-Serienbrief · Datum: ______________", { run: { color: "595959" } }),
    P("Ihre Meinung hilft uns. Sie müssen **keinen Namen** schreiben. Machen Sie ein **Kreuz** ✗ pro Zeile."),
    spacer(60),
    Grid([5638, 1333, 1333, 1334], ["", [img("smiley_gut.png"), label("ja")], [img("smiley_mittel.png"), label("teils")], [img("smiley_schlecht.png"), label("nein")]],
      statements.map((s) => [s, "", "", ""]), { rowHeight: 480, vCenter: true }),
    spacer(120),
    P("**Das Tempo war für mich:**"),
    P("☐  zu schnell          ☐  genau richtig          ☐  zu langsam", { run: { size: 26 } }),
    spacer(60),
    P("**Meine Note für den Tag** (bitte einkreisen):"),
    P("1        2        3        4        5        6", { run: { size: 30, bold: true }, indent: { left: 400 } }),
    P("1 = sehr gut   …   6 = ungenügend", { run: { size: 18, color: "595959" }, indent: { left: 400 } }),
    spacer(60),
    P("**Das war gut:**"), line(), line(),
    spacer(60),
    P("**Das war schwierig / das kann besser werden:**"), line(), line(),
    spacer(60),
    P("**Das möchte ich am Computer noch lernen:**"), line(), line(),
    spacer(120),
    new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "Vielen Dank!", bold: true, size: 32, color: "1F3864" })] }),
  ];
  return new Document({
    creator: "EDV-Tag", title: "Feedbackbogen", styles: baseStyles(24),
    sections: [{ properties: { page: { ...docPage, margin: { ...docPage.margin, top: 850, bottom: 850 } } }, children }],
  });
}

// ---------------------------------------------------------------------------
// Trainer plan
// ---------------------------------------------------------------------------

function trainerPlan() {
  const children = [
    H1("EDV-Tag: Excel-Adressliste & Word-Serienbrief"),
    P("Ablaufplan für die Lehrkraft", { run: { size: 24, color: "595959" } }),
    Grid([2400, 7238], null, [
      ["**Zielgruppe**", "Teilnehmende (TN) der Maßnahme Job plus – gemischte PC-Kenntnisse"],
      ["**Termin**", "evtl. Montag, 02.11.2026"],
      ["**Dauer**", "ca. 5½ Stunden (Beispiel 09:00–14:30 – an die Kurszeiten anpassen)"],
      ["**Raum / Technik**", "1 PC pro TN, Beamer, Microsoft Word + Excel (2016/2019/2021/365, deutsch)"],
      ["**Ergebnis**", "Jede/r TN geht mit einer eigenen Bewerbungsliste (Excel) und 5 fertigen Anschreiben-Rahmen (Word/PDF) nach Hause."],
    ]),

    H2("Lernziele – die TN können …"),
    Bullet("eine Adressliste in Excel nach festen Regeln anlegen und pflegen (auch als Nachweis der Bewerbungen),"),
    Bullet("einen Geschäftsbrief mit Kopf- und Fußzeile in Word an die eigenen Daten anpassen,"),
    Bullet("die Excel-Liste als Datenquelle mit einem Word-Serienbrief verbinden und Seriendruckfelder einfügen,"),
    Bullet("mit einem Klick alle Briefe erzeugen, einzeln weiterbearbeiten und als PDF speichern."),

    H2("Vorbereitung (bis zum Vortag)"),
    Check("**Generalprobe** an einem Schulungs-PC: die Anleitung einmal komplett durchspielen (ca. 15 Min.). Dabei Office-Version und Sprache prüfen."),
    Check("Ordner **EDV-Tag** mit Bewerbungsliste.xlsx, Anschreiben_Uebung.docx, Anschreiben_Loesung.docx auf **jeden PC** kopieren (lokal, z. B. Dokumente – nicht OneDrive/Netzlaufwerk). Kopie auf USB-Stick als Reserve."),
    Check("Dateien aus E-Mail/Internet sind oft **gesperrt** (Geschützte Ansicht): einmal öffnen → **Bearbeitung aktivieren**, oder Rechtsklick auf die Datei → Eigenschaften → **Zulassen**."),
    Check("TN vorher bitten: **3–5 echte Stellenanzeigen** mitbringen (Ausdruck, Foto oder Link). Plan B: Jobbörse der Arbeitsagentur im Kurs oder die Beispieldaten."),
    Check("Ausdrucken: **Anleitung** und **Feedbackbogen** (je 1 pro TN)."),
    Check("USB-Sticks oder E-Mail-Zugang, damit die TN ihre Dateien mitnehmen können."),

    H2("Ablauf"),
    Grid([1600, 1550, 4588, 1900], ["Zeit", "Phase", "Inhalt", "Material"], [
      ["09:00–09:20", "**Start**", "Begrüßung, Ziel des Tages. **Demo:** Excel-Liste + Lösung → Fertig stellen → 5 Briefe in 1 Minute („Das können Sie heute Mittag auch.“). Kurze Abfrage: Wer hat schon mit Word/Excel gearbeitet?", "Beamer, Beispiel-Ergebnis (PDF)"],
      ["09:20–09:35", "**Prinzip**", "Tafelbild: „Was **gleich** bleibt → Word. Was sich **ändert** → Excel.“ Aufbau eines Anschreibens nach DIN 5008 zeigen (Absender, Anschrift, Datum, Betreff, Anrede, Text, Gruß, Anlagen).", "Whiteboard"],
      ["09:35–10:30", "**Excel 1**", "Grundbegriffe: Zelle, Zeile, Spalte. Liste öffnen, **5 goldene Regeln**. Eigene Stellenanzeigen eintragen (Anrede-Auswahl, PLZ als Text).", "Excel-Liste, Anleitung Teil 1"],
      ["10:30–10:45", "Pause", "", ""],
      ["10:45–11:30", "**Excel 2**", "Formeln **erklären, nicht schreiben lassen**: GLÄTTEN und WENN in den grauen Spalten. Ausfüllkästchen. **Daten → Filtern / Sortieren**. Spalten Beworben_am / Status als Bewerbungs-Nachweis (z. B. fürs Jobcenter). Speichern + **Excel schließen**.", "Anleitung Teil 1"],
      ["11:30–12:15", "**Word**", "Kopf- und Fußzeile mit eigenen Daten, Wohnort, Name unter dem Gruß. Regel: [Ihr …] selbst tippen, [Firma] usw. kommt aus Excel.", "Übungsbrief, Anleitung Teil 2"],
      ["12:15–12:45", "Mittag", "", ""],
      ["12:45–13:45", "**Serienbrief**", "Gemeinsam am Beamer, Schritt für Schritt: Briefe → Empfänger auswählen → Felder einfügen → Vorschau → Speichern → **Fertig stellen und zusammenführen**. Danach den ersten Brief individuell schreiben.", "Anleitung Teil 3 + 4"],
      ["13:45–14:00", "Puffer", "", ""],
      ["14:00–14:15", "**Sichern**", "PDF speichern, Dateien auf Stick / per E-Mail. **Persönliche Dateien vom PC löschen.** Schnelle TN: Bonusaufgaben.", "Anleitung Teil 4 + 5"],
      ["14:15–14:30", "**Abschluss**", "Runde: „Was nehme ich mit?“ **Feedbackbogen** anonym ausfüllen und einsammeln.", "Feedbackbogen"],
    ], { zebra: true }),

    H2("Differenzierung"),
    P("**Wer nicht mitkommt:** mit **Anschreiben_Loesung.docx** weiterarbeiten – die Felder sind schon drin. Nur Kopf-/Fußzeile anpassen, dann Teil 3 ab Schritt 1–4 (Liste verbinden) und Teil 4. Tandems bilden (schnell + langsam)."),
    P("**Wer schnell fertig ist (Bonus):**"),
    Bullet("**Briefumschläge** oder **Etiketten** aus derselben Liste: Sendungen → Seriendruck starten → Umschläge / Etiketten."),
    Bullet("In der Empfängerliste nur Firmen mit Status **offen** auswählen (Empfängerliste bearbeiten → Filtern)."),
    Bullet("Word-Regel statt Excel-Formel: Sendungen → **Regeln** → **Wenn…Dann…Sonst** für „Sehr geehrte Frau / Sehr geehrter Herr“."),
    Bullet("Eigene Spalte **Kennziffer** in Excel ergänzen und im Betreff verwenden."),
    P("**Sprache:** Fachwörter an die Tafel (Zelle, Zeile, Spalte, Datenquelle, Seriendruckfeld, Serienbrief). Einfache, kurze Sätze; jeden Schritt einmal vormachen, dann nachmachen lassen."),

    H2("Typische Stolperfallen"),
    Grid([3700, 5938], ["Was passiert", "Ursache / Lösung"], [
      ["Sendungen-Knöpfe grau, Excel lässt nichts eintippen", "**Geschützte Ansicht** → Bearbeitung aktivieren (siehe Vorbereitung)."],
      ["Frage mit **SQL-Befehl** beim Öffnen", "Normal bei Serienbriefen → **Ja**. Vorher ankündigen!"],
      ["Word findet die Liste nicht", "Excel-Datei verschoben/umbenannt → Empfänger auswählen → Liste neu wählen."],
      ["Neue Zeilen erscheinen nicht", "Excel nicht gespeichert oder noch offen → speichern, schließen, in Word **Aktualisieren**."],
      ["Leere Briefe am Ende", "Formeln/Formate in leeren Excel-Zeilen → Zeilen löschen, oder Empfängerliste filtern: Firma **(Nicht leere)**."],
      ["Graue Formelzelle überschrieben", "Formel aus der Zeile darüber mit dem Ausfüllkästchen herunterziehen."],
      ["PLZ ohne führende 0", "Zelle als **Text** formatieren und PLZ neu eingeben."],
      ["Briefanrede leer in der Vorschau", "Excel-Datei einmal öffnen und speichern (berechnet die Formeln), dann in Word aktualisieren."],
      ["Datum ändert sich später", "Im fertigen Dokument **Strg + A**, **Strg + Umschalt + F9**."],
    ]),

    H2("Datenschutz"),
    P("Die TN arbeiten mit ihren **eigenen persönlichen Daten**. Am Ende: Dateien auf Stick/E-Mail sichern und vom Schulungs-PC löschen (auch aus dem Papierkorb und ggf. „Zuletzt verwendet“). Die Beispiel-Firmen und -Personen in den Dateien sind erfunden."),

    H2("Bewertung durch die TN"),
    P("Feedbackbogen in den letzten 15 Minuten ausfüllen lassen – anonym, z. B. in eine Box. Für die Auswertung pro Frage die Kreuze zählen (ja / teils / nein) und die Durchschnittsnote berechnen; die offenen Antworten kurz zusammenfassen."),
  ];
  return new Document({
    creator: "EDV-Tag", title: "Ablaufplan EDV-Tag", numbering,
    styles: baseStyles(22),
    sections: [{ properties: { page: docPage }, footers: { default: pageFooter("Ablaufplan EDV-Tag · Excel-Adressliste & Word-Serienbrief") }, children }],
  });
}

// ---------------------------------------------------------------------------

async function write(doc, file) {
  fs.writeFileSync(file, await Packer.toBuffer(doc));
  console.log("wrote", file);
}

(async () => {
  const buildDir = __dirname;
  await write(letterDoc("uebung"), path.join(DIR_TN, "Anschreiben_Uebung.docx"));
  await write(letterDoc("loesung"), path.join(DIR_TN, "Anschreiben_Loesung.docx"));
  await write(letterDoc("merged"), path.join(buildDir, "Beispiel_Ergebnis.docx"));
  await write(handout(), path.join(DIR_PRINT, "Anleitung_Serienbrief.docx"));
  await write(feedback(buildDir), path.join(DIR_PRINT, "Feedbackbogen.docx"));
  await write(trainerPlan(), path.join(DIR_TRAINER, "Ablaufplan_EDV-Tag.docx"));
})();
