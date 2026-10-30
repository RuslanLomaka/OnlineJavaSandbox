# Handoff: EDV-Tag "Excel-Adressliste + Word-Serienbrief"

This file carries over everything from the previous chat so a new agent can continue without it.
It covers the context, everything that was built and why, how the conversation went, open questions,
next steps and (in the appendices) the full source code to regenerate every file.

---

## 0. Where everything lives (no copying needed)

- **Git (durable):** repo `RuslanLomaka/OnlineJavaSandbox`, branch **`claude/informatics-mail-merge-lesson-le4dri`**,
  folder **`edv-tag-serienbrief/`**:
  - `HANDOFF.md`: this file
  - `kit/`: the finished lesson kit (the same content as the ZIP the user received)
  - `generator/`: the build scripts (identical to Appendix A–D)

  Get it with:
  ```bash
  git clone --branch claude/informatics-mail-merge-lesson-le4dri --single-branch \
      https://github.com/RuslanLomaka/OnlineJavaSandbox.git
  ```
  From another cloud session, attach the repo first with the `add_repo` tool (owner `RuslanLomaka`, repo
  `OnlineJavaSandbox`). This branch has nothing to do with the Java app: don't merge it into `master`, because pushes
  to `master` deploy straight to the user's live Raspberry Pi. There is no pull request, and CI only runs on
  `master` and on pull requests.
- **The previous agent's session:** "Informatics lesson planning: mail merge Word/Excel", id
  `session_01MSe3VqvRc26UaFQN1eDnQY` (Claude Code on the web, same account). A cloud session with the
  claude-code-remote tools can:
  - read its full transcript: `list_sessions` → `list_events` (session id above);
  - ask it a question: `send_message`.

  Its container (and scratchpad files) is temporary. The transcript and this git branch are the durable copies.

## 1. Who and what

- **User:** Ruslan. Writes to the assistant in **English**. Teaches in German. Works as a trainer/lecturer for
  participants (*Teilnehmende*, "TN") of the **"Job plus"** program run by AGP in **Husum**, Germany.
  Participants are job seekers with mixed PC skills and possibly limited German.
- **Task source:** a handwritten yellow sticky note from their supervisor (photo was shared). Exact transcription:

  > Ruslan soll einen Tag EDV für die TN planen (evtl. 02.11?)
  > Themen:
  > Excel → Adresslisten
  > &
  > Word Brief → als Serienbrief
  > am Ende Bewertung durch TN

  **Translation:** "Ruslan should plan one IT day for the participants (possibly 02.11?). Topics: Excel → address
  lists, and a Word letter → as a mail merge (Serienbrief). At the end, the participants evaluate the day."
- **Date:** 02.11.2026 is a **Monday**. It was still unconfirmed ("evtl.") when the chat ended (30.10.2026).
- **What the user wanted, in their own words:** to "provide the means of automatic building of a prefilled letter
  with filled addresses and names from Excel to Word". Scenario: a participant applies for 5 jobs. With "one click"
  they get cover letters (*Anschreiben*) where the **frame** is prefilled: header, footer, greeting, date, recipient
  address and the applicant's contact data. The **body text** is written by hand. It must use Microsoft Word and
  Excel formats and "run flawlessly".
- **Key insight given to the user:** this is exactly Word's built-in **Serienbrief** (mail merge) fed by an Excel
  list. No programming is needed, and teaching it is the actual lesson, because participants can repeat it on any PC.
  Rule for the whiteboard: **"What stays the same → Word. What changes → Excel."**

## 2. Status when the chat ended

- A complete lesson kit was built, checked and delivered to the user as `EDV-Tag_Serienbrief.zip`. The day plan PDF
  and the example-result PDF were also sent separately.
- At the end, the kit, the scripts and this handoff were committed to a **separate, never-to-be-merged branch** of
  the user's Java portfolio repo (see section 0), so the next agent can fetch them without the user copying files.
  `master` and the Java app were not touched.
- **Not verified in real Microsoft Word/Excel.** The build environment was Linux (no MS Office). Checks done:
  - Office XML schema validation of all .docx files passed.
  - Every file was rendered with LibreOffice → PDF → images and inspected visually.
  - The Excel formulas were recalculated with LibreOffice: 0 errors, and the values matched the stored cached values.

  **The user still needs a full dry run on a classroom PC** (about 15 minutes, following the handout).
- The user was trying the kit themselves in Excel/Word and asked follow-up questions (see section 7). The last
  question was answered with three automation options. **The user had not yet chosen one.**

## 3. The kit: folder structure and contents

```
EDV-Tag_Serienbrief/
├── LIES_MICH.txt                         short German readme of the folders
├── 1_Fuer_Teilnehmende_auf_jeden_PC/     copy onto EVERY classroom PC, keep the 3 files in ONE folder
│   ├── Bewerbungsliste.xlsx              data source (Excel)
│   ├── Anschreiben_Uebung.docx           letter with [placeholders] for the exercise
│   └── Anschreiben_Loesung.docx          same letter, merge fields already inserted (demo / plan B)
├── 2_Zum_Ausdrucken/
│   ├── Anleitung_Serienbrief.docx/.pdf   3-page step-by-step handout for participants
│   └── Feedbackbogen.docx/.pdf           1-page feedback form ("Bewertung durch TN")
└── 3_Fuer_Trainer/
    ├── Ablaufplan_EDV-Tag.docx/.pdf      3-page day plan for the trainer
    └── Beispiel_Ergebnis.pdf             5 merged example letters (what "Finish & Merge" produces)
```

All participant-facing text is **German, simple language, formal "Sie"**. Font is Arial everywhere. Paper is A4.
All companies and people in the sample data are **fictional** (names contain "Muster"/"Beispiel"). The example
sender is "Max Mustermann, Musterweg 1, 25813 Husum, Tel. 0123 456789, max.mustermann@example.com".

### 3.1 `Bewerbungsliste.xlsx` (data source)

- **Exactly one sheet**, named `Bewerbungen`. In Word's table picker it appears as `Bewerbungen$`.
- Headers in row 1, data from row 2. Columns:

  | Col | Header | Type | Notes |
  |---|---|---|---|
  | A | Firma | input (dark-blue header) | company name |
  | B | Anrede | input | dropdown `Frau,Herr`; leave empty if no contact person |
  | C | Vorname | input | contact first name |
  | D | Nachname | input | contact last name |
  | E | Strasse | input | street + number (deliberately no "ß" in the header) |
  | F | PLZ | input | **column formatted as Text (`@`)** so leading zeros survive |
  | G | Ort | input | city |
  | H | Stelle | input | job title, used in subject line and body |
  | I | Ansprechpartner | **formula** (grey header, grey cells) | `=TRIM(B&" "&C&" "&D)`, shown in German Excel as `GLÄTTEN` → "Frau Anna Petersen" or empty |
  | J | Briefanrede | **formula** | `=IF(A="","",IF(B="Frau","Sehr geehrte Frau "&D,IF(B="Herr","Sehr geehrter Herr "&D,"Sehr geehrte Damen und Herren")))`, shown in German Excel as `WENN` with `;` |
  | K | Quelle | tracker (green header) | where the job ad was found (not used in the letter) |
  | L | Beworben_am | tracker | date column (format DD.MM.YYYY); proof of applications for the Jobcenter |
  | M | Status | tracker | dropdown `offen,Einladung,Absage,Zusage` |

- Every header cell has a comment (*Notiz*) explaining what to enter. Freeze panes at B2. Zoom 110%.
  Print setup is landscape, A4, fit to 1 page wide.
- 5 fictional sample rows:

  | Firma | Anrede | Vorname | Nachname | Strasse | PLZ | Ort | Stelle | Quelle |
  |---|---|---|---|---|---|---|---|---|
  | Muster Logistik GmbH | Frau | Anna | Petersen | Am Musterhafen 12 | 25813 | Husum | Lagerhelfer | Jobbörse Arbeitsagentur |
  | Bäckerei Beispiel KG | Herr | Jens | Hansen | Beispielweg 3 | 25821 | Bredstedt | Verkäufer | Aushang im Laden |
  | Pflegedienst Muster gGmbH | Frau | Maria | Jensen | Musterstraße 45 | 25899 | Niebüll | Pflegehelfer | Firmen-Website |
  | Hotel Beispiel am Hafen | (empty) | | | Probeallee 7 | 25832 | Tönning | Küchenhilfe | Zeitung |
  | Muster Gebäudeservice GmbH | Herr | Thomas | Möller | Beispielring 21 | 24937 | Flensburg | Hausmeister | Jobbörse Arbeitsagentur |

  Row 1 has Beworben_am = 02.11.2026. Every row has Status "offen". The hotel row has no contact person on purpose,
  to demonstrate "Sehr geehrte Damen und Herren" and blank-line suppression.

**Why it is built this way (mail-merge robustness; keep these rules if you change the file):**
1. **One sheet only.** Every sheet and every defined name shows up in Word's "Select Table" dialog. For the same
   reason there are **no print titles**: they create the defined name `_xlnm.Print_Titles`.
2. **Never format or pre-fill empty rows.** Word reads Excel's *used range*. Formatted empty cells, or formula rows
   below the data, become **blank recipients**, which means blank letters. Formats for future rows are set on the
   **column** (`<col style=…>`), which creates no cells. Data validation (dropdowns) lives in a separate XML element
   and also creates no cells. The sheet dimension is exactly `A1:M6`.
3. **Cached formula values are injected into the XML.** openpyxl writes formulas without results, and Word's Excel
   reader (ACE OLE DB) reads only the stored results; it never recalculates. Without the injected values, the
   Briefanrede and Ansprechpartner fields would merge as empty until someone opens and saves the file in Excel.
   The script writes `<c … t="str"><f>…</f><v>value</v></c>` for each formula cell.
4. **No autofilter** in the file. Participants add it themselves (*Daten → Filtern*) as an Excel exercise, and a
   fixed autofilter range would not cover rows added later.
5. **Header names** contain no spaces or special characters, because Word turns headers into field names.
6. **PLZ stored as text.** Mixed number/text columns confuse ACE's type guessing, and numbers lose leading zeros
   (e.g. 01067 becomes 1067).
7. Briefanrede is built **in Excel, not with a Word rule.** Word's own "Grußzeile" produces the ugly German
   "Sehr geehrte(r)", and Excel formulas are simpler and more robust than Word IF fields. The Word rule
   (*Regeln → Wenn…Dann…Sonst*) is offered as a bonus task.

### 3.2 `Anschreiben_Uebung.docx` and `Anschreiben_Loesung.docx` (letter templates)

DIN 5008 Form B style, A4. Measurements:
- **Margins:** top 45 mm, left 25 mm, right 20 mm, bottom 25 mm. Header distance 12 mm, footer distance 10 mm.
- **Header (Kopfzeile):** the applicant's name (bold, 16 pt, colour 1F3864), then "Street · PLZ City" (10 pt) with a
  thin blue bottom border.
- **Footer (Fußzeile):** "Telefon: … · E-Mail: …" (9 pt, grey, centred) with a top border. Using both header and
  footer is deliberate: it teaches both.
- **Body:**
  1. An **empty paragraph with exact line height 17.7 mm**, so the address starts at 62.7 mm from the top (the
     DIN 5008 Form B address zone, which fits a window envelope). A fixed-height line is used instead of "space
     before", because LibreOffice (and possibly Word) drops space-before after a page or section break.
  2. Address lines (style "Anschrift", no spacing): Firma / Ansprechpartner / Strasse / PLZ Ort.
  3. Date line, right-aligned, 15 mm space before: `<Wohnort>, ` + Word field `DATE \@ "dd.MM.yyyy"`.
  4. Subject (style "Betreff", bold via the style so the merge field is bold too, 2 lines before):
     "Bewerbung als «Stelle»".
  5. "«Briefanrede»," (2 lines before, 1 after).
  6. Body: "mit großem Interesse habe ich Ihre Stellenanzeige gelesen. Hiermit bewerbe ich mich als «Stelle» in
     Ihrem Unternehmen." → a bracketed hint paragraph "[Hier schreiben Sie in 3 bis 5 Sätzen: Was können Sie?
     Welche Erfahrung haben Sie? Warum passt diese Stelle zu Ihnen? Diesen Teil schreiben Sie für jede Firma neu.]"
     → "Über eine Einladung zu einem persönlichen Gespräch freue ich mich sehr." The "Brieftext" style has one blank
     line of spacing after each paragraph.
  7. "Mit freundlichen Grüßen", 3 blank lines for the signature, then the name.
  8. "Anlagen" / "Lebenslauf" / "Zeugnisse".
- Document language is de-DE (default run style). Compatibility mode 15 (no "compatibility mode" banner).
- **Übung version:** the sender data is placeholders: `[Ihr Vorname Nachname]`,
  `[Ihre Straße und Hausnummer] · [Ihre PLZ und Ihr Wohnort]`,
  `Telefon: [Ihre Telefonnummer] · E-Mail: [Ihre E-Mail-Adresse]`, `[Ihr Wohnort]` before the date, and the name
  under the greeting. The merge placeholders use **exactly the field names**: `[Firma] [Ansprechpartner] [Strasse]
  [PLZ] [Ort] [Stelle]` (twice: subject and body) `[Briefanrede]`.
  - **Rule taught:** `[Ihr…]/[Ihre…]` → type it yourself. `[Firma]` etc. → select it and use *Insert Merge Field*.
  - Placeholders are deliberately **not highlighted**. A highlight would be inherited by the inserted field or typed
    text and would end up printed yellow.
- **Lösung version:** Max Mustermann as sender, with 8 real `MERGEFIELD`s already in place (Firma, Ansprechpartner,
  Strasse, PLZ, Ort, Stelle ×2, Briefanrede) plus the DATE field.
- **Neither file is connected to a data source.** Word stores an absolute path when you connect, which would break
  on other PCs. Merge fields bind by name as soon as the user picks the Excel list.
- Fields are written as **complex fields** (`fldChar begin / instrText / separate / result / end`), the form Word
  itself saves. docx-js emits `w:fldSimple`, and a post-processing script converts it.
- In a merge, Word suppresses the Ansprechpartner line when it is empty, because the paragraph contains only one
  field (blank-line suppression is Word's default). That is why Ansprechpartner is **one** Excel column and not
  three fields on one line.

### 3.3 `Beispiel_Ergebnis.pdf`

Five merged letters (one section per letter, like Word's own merge output), built by substituting the sample data
into the same layout. The date is shown as 02.11.2026. It is used at the start of the day ("this is what you'll
have by lunchtime") and as a reference for the trainer.

### 3.4 `Anleitung_Serienbrief` (participant handout, 3 pages, German)

Structure: title "Bewerbungen mit einem Klick" → box "Die Idee" (same → Word, changes → Excel) → list of the 3 files.
- **Teil 1 – Excel:** open the file; click *Bearbeitung aktivieren* if Protected View shows; rows = companies,
  columns = information; overwrite the samples, fill in only the blue columns; Anrede via dropdown, leave empty if
  there is no name; don't type in the grey columns; for more than 5 companies, drag the 2 grey cells down with the
  fill handle (*Ausfüllkästchen*) only as far as there are companies; **Strg+S, then close Excel**.
  Box: **"Die 5 goldenen Regeln"** (headers in row 1 without spaces; 1 row = 1 company; no empty rows; no merged
  cells; PLZ as text).
- **Teil 2 – Word:** open Anschreiben_Uebung.docx; double-click the header (*Kopfzeile*) to enter your own data; do
  the same in the footer (*Fußzeile*); fill in Wohnort and the name under the greeting; Strg+S.
  Box: "Eckige Klammern – was mache ich?"
- **Teil 3 – Serienbrief:** *Sendungen → Seriendruck starten → Briefe*; *Empfänger auswählen → Vorhandene Liste
  verwenden…*; choose Bewerbungsliste.xlsx; in *Tabelle auswählen* pick `Bewerbungen$` and keep *Erste Datenzeile
  enthält Spaltenüberschriften* ticked → OK; select `[Firma]` → lower half of *Seriendruckfeld einfügen* → Firma;
  repeat (checklist table of all 8 insertions); *Vorschau Ergebnisse* with the ◄ ► arrows; *Datei → Speichern
  unter* → `Anschreiben_Serienbrief` in the same folder.
- **Teil 4 – Alle Briefe mit einem Klick:** *Fertig stellen und zusammenführen → Einzelne Dokumente bearbeiten… →
  Alle → OK*; save as `Meine_Bewerbungen`; write your own text in each letter; save as PDF.
  Box "Später: nur EIN Brief": new row in Excel → save/close → open Anschreiben_Serienbrief → answer **Ja** to the
  SQL question → preview, go to the new company → *Fertig stellen… → Einzelne Dokumente bearbeiten → Aktueller
  Datensatz*.
- **Teil 5 – troubleshooting table:**

  | Problem | Lösung |
  |---|---|
  | A question about a "SQL-Befehl" appears when opening | Click **Ja**. This is normal: Word is loading the list. |
  | The buttons under *Sendungen* are grey | Protected View → *Bearbeitung aktivieren*. |
  | Word can't find the list | It was moved or renamed → *Empfänger auswählen → Vorhandene Liste verwenden* → pick it again. |
  | New companies are missing | Save and close Excel → *Empfängerliste bearbeiten* → click the data source → *Aktualisieren*. |
  | Blank letters at the end | Empty rows with formulas in Excel → delete the whole rows, or filter the recipient list on Firma → *(Nicht leere)*. |
  | The leading 0 of the PLZ is missing | Format the cell as Text and type the PLZ again. |
  | The letter shows «Firma» instead of the name | Click *Vorschau Ergebnisse*. |
  | Words stick together | Add a space between the fields. |
  | The date changes every day | In the finished letters press Strg+A, then Strg+Umschalt+F9 (turns fields into plain text). |

- Final box "Ihre Daten gehören Ihnen": save your files to your own USB stick or email them to yourself, then
  delete your personal files from the classroom PC.

### 3.5 `Feedbackbogen` (1 page, German)

Title "Wie war der EDV-Tag?", a date blank, "keinen Namen", "Kreuz pro Zeile".
- A table with 3 smiley columns (green ja / yellow teils / red nein; the smileys are PNGs drawn with PIL so they
  look the same everywhere) and these statements:
  1. Der Tag hat mir gefallen.
  2. Ich habe die Erklärungen verstanden.
  3. Die Anleitung (Handout) hat mir geholfen.
  4. Ich kann jetzt eine Adressliste in Excel machen.
  5. Ich kann jetzt einen Serienbrief in Word machen.
  6. Ich benutze das für meine Bewerbungen.
- Tempo: ☐ zu schnell / ☐ genau richtig / ☐ zu langsam.
- A grade from 1 to 6 (circle one).
- Open fields: "Das war gut", "Das war schwierig / das kann besser werden", "Das möchte ich am Computer noch lernen".
- Closes with "Vielen Dank!".

### 3.6 `Ablaufplan_EDV-Tag` (trainer plan, 3 pages, German)

- **Meta:** target group TN Job plus; date possibly Mon 02.11.2026; about 5½ h (example 09:00–14:30, **assumed,
  not confirmed**); 1 PC per TN, projector, Word + Excel 2016/2019/2021/365 in German.
- **Learning goals:** build and maintain an address list (also as application proof); adapt a business letter with
  header and footer; connect Excel as the data source and insert merge fields; produce all letters with one click,
  edit them individually and save as PDF.
- **Preparation checklist:**
  - Dry run on a classroom PC.
  - Copy the folder locally to each PC (not OneDrive or a network drive), plus a spare USB stick.
  - Files from email or the internet are blocked (Protected View / Mark of the Web): click *Bearbeitung aktivieren*,
    or right-click → Eigenschaften → *Zulassen*.
  - Ask the TN to bring 3–5 real job ads (plan B: the Jobbörse der Arbeitsagentur, or the sample data).
  - Print the handout and feedback form.
  - Have USB sticks or email ready so TN can take their files home.
- **Schedule:**

  | Zeit | Phase | Inhalt |
  |---|---|---|
  | 09:00–09:20 | Start | Welcome, goal, **demo** with the Lösung file: Finish & Merge → 5 letters in 1 min; ask who has used Word/Excel before |
  | 09:20–09:35 | Prinzip | Whiteboard: same → Word, changes → Excel; structure of a DIN 5008 Anschreiben |
  | 09:35–10:30 | Excel 1 | Cell/row/column, the 5 golden rules, enter own job ads |
  | 10:30–10:45 | Pause | |
  | 10:45–11:30 | Excel 2 | Explain the formulas (GLÄTTEN, WENN), fill handle, *Daten → Filtern/Sortieren*, Beworben_am/Status as Jobcenter proof, save + close |
  | 11:30–12:15 | Word | Header and footer with own data, Wohnort, name; the bracket rule |
  | 12:15–12:45 | Mittag | |
  | 12:45–13:45 | Serienbrief | Step by step on the projector: connect, insert fields, preview, save, Finish & Merge; write the first letter |
  | 13:45–14:00 | Puffer | |
  | 14:00–14:15 | Sichern | PDF, USB/email, **delete personal files from the PC**; bonus tasks for fast TN |
  | 14:15–14:30 | Abschluss | Round "Was nehme ich mit?", fill in the feedback form anonymously |

- **Differentiation:** slower TN use Anschreiben_Loesung.docx (only adapt header/footer, connect, merge) and work in
  tandems. Faster TN get bonus tasks: envelopes or labels from the same list; filter the recipient list to Status
  "offen"; the Word rule *Wenn…Dann…Sonst*; add their own "Kennziffer" column and use it in the subject line.
  Write technical terms on the board.
- **Pitfalls table:** same as the handout, plus "grey formula cell overwritten → drag the formula down again" and
  "Briefanrede empty in preview → open and save the Excel file once, then *Aktualisieren* in Word".
- **Datenschutz:** participants work with their own personal data → delete it at the end, including the recycle bin
  and "recently used" lists.
- **Evaluation:** count the crosses per question and compute the average grade; summarise the open answers.

## 4. Correct Word/Excel menu names (UI language not confirmed)

The user's Office UI language was asked about but **never confirmed**. Answers so far gave English labels with the
German ones in parentheses.

| English | German |
|---|---|
| Mailings | Sendungen |
| Start Mail Merge → Letters | Seriendruck starten → Briefe |
| Select Recipients → Use an Existing List… | Empfänger auswählen → Vorhandene Liste verwenden… |
| Select Table / "First row of data contains column headers" | Tabelle auswählen / "Erste Datenzeile enthält Spaltenüberschriften" |
| Edit Recipient List (→ Refresh) | Empfängerliste bearbeiten (→ Aktualisieren) |
| Insert Merge Field | Seriendruckfeld einfügen |
| Rules → If…Then…Else | Regeln → Wenn…Dann…Sonst |
| Preview Results | Vorschau Ergebnisse |
| Finish & Merge → Edit Individual Documents… → All / Current record / From–To | Fertig stellen und zusammenführen → Einzelne Dokumente bearbeiten… → Alle / Aktueller Datensatz / Von–Bis |
| Protected View → Enable Editing | Geschützte Ansicht → Bearbeitung aktivieren |
| Header / Footer | Kopfzeile / Fußzeile |
| Fill handle | Ausfüllkästchen |
| Format Cells → Text | Zellen formatieren → Text |
| Data → Filter / Sort | Daten → Filtern / Sortieren |
| File → New → Blank workbook (Ctrl+N) | Datei → Neu → Leere Arbeitsmappe (Strg+N) |
| Unlink fields: Ctrl+A, Ctrl+Shift+F9 | Strg+A, Strg+Umschalt+F9 |

## 5. Things the user should know (gotchas)

- Connecting the list (*Empfänger auswählen*) **shows no visible change** in the letter. This confused the user
  ("after step 3.4 nothing happened"). Success means the buttons *Seriendruckfeld einfügen* and *Vorschau
  Ergebnisse* become active, and *Empfängerliste bearbeiten* lists the companies.
- Save and **close Excel** before connecting or merging; otherwise Word may fail or read stale data.
- Don't move or rename the Excel file after connecting; if it moves, reconnect.
- Reopening a connected letter always asks the "SQL-Befehl" question → **Ja**.
- The DATE field keeps updating in merged letters; freeze it with Strg+A, Strg+Umschalt+F9.
- Protected View disables the Mailings buttons and Excel editing.
- **Mail merge starts in Word, never in Excel.** The user first asked "I'm in the Excel file, how do I create a new
  document?".

## 6. Conversation log (condensed)

1. The user shared the sticky note and their idea. The assistant explained that it is Word's Serienbrief, built and
   verified the kit (section 3), sent the ZIP plus the day plan PDF and the Beispiel_Ergebnis PDF, and asked:
   real course hours? number of TN and their PC/German level? Office version? English copy of the day plan?
   **(No answers yet.)**
2. **"I'm in the Excel file now, how do I create a new document?"** Answer: the merge starts in Word. Save and close
   Excel → open Word (template or blank document) → enable editing → Mailings → Start Mail Merge → Letters →
   Select Recipients → Use an Existing List → pick the file → `Bewerbungen$` + headers checkbox → OK → insert fields
   → Preview → Finish & Merge. A new Excel workbook would be File → New → Blank workbook / Ctrl+N. The assistant
   asked which one they meant and their UI language. **(No answer.)**
3. **"After step 3.4 nothing happened."** Answer: that is normal (see section 5) → how to verify → continue with
   inserting fields. If the buttons stay grey: a dialog hidden behind the window (Alt+Tab), Excel still open, or
   Protected View. The assistant asked for a screenshot. **(No answer; it's unknown whether it worked.)**
4. **"Can this work more automatically?"** Three options were offered (section 8). The recommendation was option 1
   for class and option 3 as an optional demo if the PCs allow macros. Questions asked: which option to build,
   which folder the files will be in on the classroom PCs, and whether macros are allowed. **(No answer.)**
5. The user then started a new project and requested this handoff file.

## 7. Open questions for the user

1. Is the date really **Mon 02.11.2026**? What are the real course hours?
2. How many participants? What is their PC and German level?
3. Which **Office version and UI language** do the classroom PCs have (and the user's own PC)?
4. Did connecting the list work after the explanation, or are the buttons still grey?
5. Which automation option (section 8)? For option 2: the exact folder path on all PCs. For option 3: are macros
   allowed on the classroom PCs?
6. Do they want an English version of the day plan?

## 8. Automation options (offered, not built)

**Option 1 – connect once, then 3 clicks (no build needed; recommended for class).** After connecting and inserting
the fields, save the letter as `Anschreiben_Serienbrief.docx` next to the Excel file. From then on: add a row in
Excel → save/close → open the Word file → **Ja** → Finish & Merge → All (or *Current record* for just the newest
job). No reconnecting.

**Option 2 – pre-connected template.** Ship the .docx already linked to the list, so participants skip the connect
step. The catch: Word stores an **absolute path**, so all PCs need the identical folder (e.g. `C:\EDV-Tag\`). If the
file isn't there, Word asks the user to locate the data source (recoverable). Implementation sketch, **untested**,
verify against a file saved by real Word first: in `word/settings.xml` add

```xml
<w:mailMerge>
  <w:mainDocumentType w:val="formLetters"/>
  <w:linkToQuery/>
  <w:dataType w:val="native"/>
  <w:connectString w:val="Provider=Microsoft.ACE.OLEDB.12.0;User ID=Admin;Data Source=C:\EDV-Tag\Bewerbungsliste.xlsx;Mode=Read;Extended Properties=&quot;HDR=YES;IMEX=1;&quot;;Jet OLEDB:Engine Type=37;"/>
  <w:query w:val="SELECT * FROM `Bewerbungen$` "/>
  <w:dataSource r:id="rIdMMDS"/>
  <w:viewMergedData/>
</w:mailMerge>
```

and in `word/_rels/settings.xml.rels` add a relationship `rIdMMDS` of type
`http://schemas.openxmlformats.org/officeDocument/2006/relationships/mailMergeSource` with
`Target="file:///C:\EDV-Tag\Bewerbungsliste.xlsx" TargetMode="External"`. Real Word also writes a `<w:odso>` block.
The most reliable approach is to connect once in real Word, save, unzip, and copy exactly what Word wrote.

**Option 3 – one-click button in Excel ("Briefe erstellen").** An `.xlsm` with a macro that saves the workbook,
starts Word, opens `Anschreiben_Serienbrief.docx`/`Anschreiben_Loesung.docx` from the same folder, attaches the data
source, merges to a new document, and optionally saves one PDF per company (e.g.
`Bewerbung_Muster_Logistik_GmbH.pdf`). Caveats:
- Macros in files from email or the internet are blocked by default; IT may block them entirely.
- Windows desktop Office only.
- Participants learn less and can't repeat it elsewhere.
- **Untested**, because there was no Office in the build environment.

Sketch, **untested**:

```vba
Sub BriefeErstellen()
    Const wdFormLetters = 0, wdSendToNewDocument = 0, wdFormatPDF = 17
    ThisWorkbook.Save
    Dim wd As Object, main As Object, i As Long, n As Long, firma As String, outDoc As Object
    Set wd = CreateObject("Word.Application")
    wd.Visible = True
    Set main = wd.Documents.Open(ThisWorkbook.Path & "\Anschreiben_Loesung.docx")
    main.MailMerge.MainDocumentType = wdFormLetters
    main.MailMerge.OpenDataSource Name:=ThisWorkbook.FullName, _
        SQLStatement:="SELECT * FROM `Bewerbungen$`"
    n = Application.WorksheetFunction.CountA(ThisWorkbook.Worksheets("Bewerbungen").Range("A:A")) - 1
    For i = 1 To n
        With main.MailMerge
            .Destination = wdSendToNewDocument
            .DataSource.FirstRecord = i
            .DataSource.LastRecord = i
            .Execute Pause:=False
        End With
        Set outDoc = wd.ActiveDocument
        firma = ThisWorkbook.Worksheets("Bewerbungen").Cells(i + 1, 1).Value
        firma = Replace(Replace(Replace(firma, " ", "_"), "/", "_"), "\", "_")
        outDoc.SaveAs2 ThisWorkbook.Path & "\Bewerbung_" & firma & ".docx"
        outDoc.ExportAsFixedFormat ThisWorkbook.Path & "\Bewerbung_" & firma & ".pdf", wdFormatPDF
    Next i
    main.Close SaveChanges:=False
    MsgBox n & " Briefe erstellt.", vbInformation
End Sub
```

Open risks for option 3:
- ACE reading the workbook while it is open in Excel (it usually reads the last saved state, hence `ThisWorkbook.Save`).
- The record count is based on column A.
- Keep the PDF step optional, because the body text still has to be written per letter. Exporting PDFs before the
  body is written only makes sense for the frame. Probably better: create the .docx files only.

## 9. How to regenerate the files

The previous environment was Linux, with:
- Node 22 + `docx` npm package **9.7.1**
- Python 3 + `openpyxl` 3.1.5 + Pillow
- LibreOffice (`soffice`) + `pdftoppm` for rendering checks

Pipeline (scripts in Appendix A–D, all in one folder; it is called `build/` below and `generator/` in the git branch;
`OUT` is the kit folder):

```bash
OUT=out/EDV-Tag_Serienbrief
mkdir -p "$OUT/1_Fuer_Teilnehmende_auf_jeden_PC" "$OUT/2_Zum_Ausdrucken" "$OUT/3_Fuer_Trainer"
cd build
python3 make_xlsx.py "../$OUT/1_Fuer_Teilnehmende_auf_jeden_PC/Bewerbungsliste.xlsx"   # also injects cached values
python3 smileys.py                                   # writes smiley_*.png next to the scripts (needed by the feedback form)
node build_docs.js "../$OUT"                         # writes all .docx; Beispiel_Ergebnis.docx lands in build/
python3 complex_fields.py "../$OUT/1_Fuer_Teilnehmende_auf_jeden_PC/Anschreiben_Uebung.docx" \
                          "../$OUT/1_Fuer_Teilnehmende_auf_jeden_PC/Anschreiben_Loesung.docx"
# PDFs: soffice --headless --convert-to pdf --outdir <dir> <file.docx>
#   Beispiel_Ergebnis.docx -> 3_Fuer_Trainer/Beispiel_Ergebnis.pdf
#   Anleitung, Feedbackbogen, Ablaufplan -> PDF next to their .docx
# Then add LIES_MICH.txt (Appendix E) and zip the folder.
```

Verification steps used:
- `openpyxl.load_workbook(..., data_only=True)` must show the formula values (not `None`), and the sheet dimension
  must be `A1:M6`.
- A LibreOffice recalc of a **copy** must give 0 errors and the same values.
- Validate the docx XML, render to PDF/JPG and look at every page.
- Expected page counts: letters 1 page each, example result 5, handout 3, feedback form **exactly 1** (it is close
  to the limit; the bottom/top margins were reduced to 15 mm for that), day plan 3.
- `grep -o 'MERGEFIELD [A-Za-z]*'` on the Lösung `document.xml` should give 8 hits (`complex_fields.py` reports 9
  converted fields, because that count includes DATE).

## 10. Suggested next steps for the new agent

1. Get answers to section 7, especially the date, Office version and UI language, and whether the connect step works.
2. If the user's Office is English, offer an English handout variant, or at least add the English menu names to the
   German handout (participants' PCs are probably German).
3. Adjust the times in the day plan to the real course hours.
4. If the user wants it, build option 2 or 3, and have the user test it on a real PC before the lesson.
5. Remind the user to do the dry run on a classroom PC and to print the handout and feedback forms.

---

## Appendix A – `make_xlsx.py`

````python
"""Builds Bewerbungsliste.xlsx: the mail-merge data source for the Serienbrief lesson.

Design rules (they are what makes the Word mail merge reliable):
- exactly ONE sheet, headers in row 1, data from row 2, no empty rows, no merged cells
- header names without spaces/special characters (Word turns them into field names)
- formatting for future input rows is set on the COLUMN, never on empty cells,
  otherwise Excel's used range grows and Word shows empty recipients
- formula cells get cached values injected (see inject_cached_values), because Word
  reads the stored values and never recalculates the workbook itself
"""
import re
import shutil
import sys
import zipfile
from datetime import date

from openpyxl import Workbook
from openpyxl.comments import Comment
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.worksheet.datavalidation import DataValidation

OUT = sys.argv[1]

HEADERS = [
    # name, width, kind, comment
    ("Firma", 34, "input", "Name der Firma, genau wie in der Stellenanzeige."),
    ("Anrede", 10, "input", "Frau oder Herr auswählen (Pfeil rechts in der Zelle).\n"
                            "Leer lassen, wenn Sie keinen Namen kennen."),
    ("Vorname", 12, "input", "Vorname der Ansprechperson (kann leer bleiben)."),
    ("Nachname", 14, "input", "Nachname der Ansprechperson (kann leer bleiben)."),
    ("Strasse", 24, "input", "Straße und Hausnummer der Firma."),
    ("PLZ", 8, "input", "Postleitzahl. Die Spalte ist als TEXT formatiert,\n"
                        "damit eine 0 am Anfang nicht verschwindet."),
    ("Ort", 14, "input", "Ort der Firma."),
    ("Stelle", 22, "input", "Name der Stelle, z. B. Lagerhelfer.\n"
                            "Erscheint im Betreff: Bewerbung als ..."),
    ("Ansprechpartner", 24, "formula", "FORMEL – nicht überschreiben!\n"
                                       "Wird automatisch aus Anrede, Vorname und Nachname gebildet."),
    ("Briefanrede", 34, "formula", "FORMEL – nicht überschreiben!\n"
                                   "Frau → Sehr geehrte Frau ...\nHerr → Sehr geehrter Herr ...\n"
                                   "leer → Sehr geehrte Damen und Herren"),
    ("Quelle", 26, "tracker", "Wo haben Sie die Stelle gefunden?\n(Nur für Ihre Übersicht, nicht im Brief.)"),
    ("Beworben_am", 13, "tracker", "Datum der Bewerbung.\nHilft beim Nachweis für das Jobcenter."),
    ("Status", 12, "tracker", "offen / Einladung / Absage / Zusage"),
]

# Fictional sample data (no real companies, no real people).
ROWS = [
    ["Muster Logistik GmbH", "Frau", "Anna", "Petersen", "Am Musterhafen 12", "25813", "Husum",
     "Lagerhelfer", "Jobbörse Arbeitsagentur", date(2026, 11, 2), "offen"],
    ["Bäckerei Beispiel KG", "Herr", "Jens", "Hansen", "Beispielweg 3", "25821", "Bredstedt",
     "Verkäufer", "Aushang im Laden", None, "offen"],
    ["Pflegedienst Muster gGmbH", "Frau", "Maria", "Jensen", "Musterstraße 45", "25899", "Niebüll",
     "Pflegehelfer", "Firmen-Website", None, "offen"],
    ["Hotel Beispiel am Hafen", None, None, None, "Probeallee 7", "25832", "Tönning",
     "Küchenhilfe", "Zeitung", None, "offen"],
    ["Muster Gebäudeservice GmbH", "Herr", "Thomas", "Möller", "Beispielring 21", "24937", "Flensburg",
     "Hausmeister", "Jobbörse Arbeitsagentur", None, "offen"],
]

COL = {name: i + 1 for i, (name, *_rest) in enumerate(HEADERS)}
LETTER = {name: chr(ord("A") + i) for i, (name, *_rest) in enumerate(HEADERS)}

FONT = "Arial"
thin = Side(style="thin", color="BFBFBF")
border = Border(left=thin, right=thin, top=thin, bottom=thin)
head_fill = {"input": "1F4E79", "formula": "7F7F7F", "tracker": "548235"}
formula_fill = PatternFill("solid", fgColor="EDEDED")


def ansprechpartner(anrede, vorname, nachname):
    return " ".join(p for p in (anrede or "", vorname or "", nachname or "") if p).strip()


def briefanrede(firma, anrede, nachname):
    if not firma:
        return ""
    if anrede == "Frau":
        return "Sehr geehrte Frau " + (nachname or "")
    if anrede == "Herr":
        return "Sehr geehrter Herr " + (nachname or "")
    return "Sehr geehrte Damen und Herren"


def build():
    wb = Workbook()
    ws = wb.active
    ws.title = "Bewerbungen"

    for name, width, kind, note in HEADERS:
        c = ws.cell(row=1, column=COL[name], value=name)
        c.font = Font(name=FONT, bold=True, color="FFFFFF")
        c.fill = PatternFill("solid", fgColor=head_fill[kind])
        c.alignment = Alignment(vertical="center")
        c.border = border
        c.comment = Comment(note, "EDV-Tag")
        c.comment.width = 260
        c.comment.height = 90
        dim = ws.column_dimensions[LETTER[name]]
        dim.width = width
        # Column-level formats apply to rows participants add later,
        # without creating empty cells that Word would read as recipients.
        dim.font = Font(name=FONT)
        if name == "PLZ":
            dim.number_format = "@"
        if name == "Beworben_am":
            dim.number_format = "DD.MM.YYYY"
    ws.row_dimensions[1].height = 20

    expected = {}
    for r, row in enumerate(ROWS, start=2):
        firma, anrede, vorname, nachname, strasse, plz, ort, stelle, quelle, beworben, status = row
        values = {
            "Firma": firma, "Anrede": anrede, "Vorname": vorname, "Nachname": nachname,
            "Strasse": strasse, "PLZ": plz, "Ort": ort, "Stelle": stelle,
            "Quelle": quelle, "Beworben_am": beworben, "Status": status,
        }
        for name, v in values.items():
            if v is not None:
                ws.cell(row=r, column=COL[name], value=v)
        a, b, c_, d = (f"{LETTER[n]}{r}" for n in ("Firma", "Anrede", "Vorname", "Nachname"))
        # German Excel shows these as GLÄTTEN / WENN with semicolons.
        ws.cell(row=r, column=COL["Ansprechpartner"], value=f'=TRIM({b}&" "&{c_}&" "&{d})')
        ws.cell(row=r, column=COL["Briefanrede"], value=(
            f'=IF({a}="","",IF({b}="Frau","Sehr geehrte Frau "&{d},'
            f'IF({b}="Herr","Sehr geehrter Herr "&{d},"Sehr geehrte Damen und Herren")))'))
        expected[f"{LETTER['Ansprechpartner']}{r}"] = ansprechpartner(anrede, vorname, nachname)
        expected[f"{LETTER['Briefanrede']}{r}"] = briefanrede(firma, anrede, nachname)

        for name in COL:
            cell = ws.cell(row=r, column=COL[name])
            cell.font = Font(name=FONT)
            cell.border = border
            if name in ("Ansprechpartner", "Briefanrede"):
                cell.fill = formula_fill
            if name == "PLZ":
                cell.number_format = "@"
            if name == "Beworben_am":
                cell.number_format = "DD.MM.YYYY"

    # Dropdowns. Data validation lives in its own XML element and does not create cells.
    dv_anrede = DataValidation(type="list", formula1='"Frau,Herr"', allow_blank=True,
                               showErrorMessage=True, errorTitle="Anrede",
                               error="Bitte Frau oder Herr auswählen – oder leer lassen.")
    dv_anrede.add(f"{LETTER['Anrede']}2:{LETTER['Anrede']}500")
    dv_status = DataValidation(type="list", formula1='"offen,Einladung,Absage,Zusage"',
                               allow_blank=True)
    dv_status.add(f"{LETTER['Status']}2:{LETTER['Status']}500")
    ws.add_data_validation(dv_anrede)
    ws.add_data_validation(dv_status)

    ws.freeze_panes = "B2"
    # Printable as proof of applications: landscape, one page wide (no print titles:
    # they create a defined name that shows up as an extra table in Word's picker).
    ws.page_setup.orientation = "landscape"
    ws.page_setup.paperSize = ws.PAPERSIZE_A4
    ws.sheet_properties.pageSetUpPr.fitToPage = True
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 0
    ws.sheet_view.zoomScale = 110
    wb.save(OUT)
    return expected


def inject_cached_values(path, expected):
    """openpyxl writes formulas without results; Word's Excel reader only sees stored results."""
    tmp = path + ".tmp"
    with zipfile.ZipFile(path) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == "xl/worksheets/sheet1.xml":
                xml = data.decode("utf-8")
                for ref, value in expected.items():
                    pattern = re.compile(rf'<c r="{ref}"([^>]*)><f>(.*?)</f><v\s*/?>(?:</v>)?</c>')
                    m = pattern.search(xml)
                    if not m:
                        raise SystemExit(f"formula cell {ref} not found in sheet XML")
                    attrs = re.sub(r'\s*t="[^"]*"', "", m.group(1))
                    esc = value.replace("&", "&amp;").replace("<", "&lt;")
                    xml = xml[:m.start()] + f'<c r="{ref}"{attrs} t="str"><f>{m.group(2)}</f><v>{esc}</v></c>' + xml[m.end():]
                data = xml.encode("utf-8")
            zout.writestr(item, data)
    shutil.move(tmp, path)


if __name__ == "__main__":
    exp = build()
    inject_cached_values(OUT, exp)
    for k, v in exp.items():
        print(k, "=>", repr(v))
````

## Appendix B – `build_docs.js`

````javascript
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
````

## Appendix C – `complex_fields.py`

````python
"""Rewrites <w:fldSimple> fields as begin/instrText/separate/result/end runs, the form Word itself saves."""
import re
import shutil
import sys
import zipfile

FLD = re.compile(r'<w:fldSimple w:instr="([^"]*)">(<w:r>(<w:rPr>.*?</w:rPr>)?<w:t[^>]*>(.*?)</w:t></w:r>)</w:fldSimple>')


def to_complex(m):
    instr, rpr, result = m.group(1), m.group(3) or "", m.group(4)
    run = lambda inner: f"<w:r>{rpr}{inner}</w:r>"
    return (run('<w:fldChar w:fldCharType="begin"/>')
            + run(f'<w:instrText xml:space="preserve"> {instr.strip()} </w:instrText>')
            + run('<w:fldChar w:fldCharType="separate"/>')
            + run(f'<w:t xml:space="preserve">{result}</w:t>')
            + run('<w:fldChar w:fldCharType="end"/>'))


for path in sys.argv[1:]:
    tmp = path + ".tmp"
    count = 0
    with zipfile.ZipFile(path) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == "word/document.xml":
                xml, count = FLD.subn(to_complex, data.decode("utf-8"))
                if "fldSimple" in xml:
                    raise SystemExit(f"{path}: unconverted fldSimple left")
                data = xml.encode("utf-8")
            zout.writestr(item, data)
    shutil.move(tmp, path)
    print(f"{path}: {count} fields converted")
````

## Appendix D – `smileys.py`

````python
from PIL import Image, ImageDraw
def face(path, fill, mood):
    s = 4; W = 120 * s
    im = Image.new("RGBA", (W, W), (255, 255, 255, 0)); d = ImageDraw.Draw(im)
    d.ellipse([4*s, 4*s, W-4*s, W-4*s], fill=fill, outline=(60, 60, 60), width=4*s)
    for x in (40, 80):
        d.ellipse([(x-8)*s, 38*s, (x+8)*s, 56*s], fill=(40, 40, 40))
    if mood == "happy":
        d.arc([30*s, 45*s, 90*s, 95*s], 20, 160, fill=(40, 40, 40), width=6*s)
    elif mood == "neutral":
        d.line([36*s, 80*s, 84*s, 80*s], fill=(40, 40, 40), width=6*s)
    else:
        d.arc([30*s, 72*s, 90*s, 118*s], 200, 340, fill=(40, 40, 40), width=6*s)
    im.resize((240, 240), Image.LANCZOS).save(path)
face("smiley_gut.png", (112, 190, 90), "happy")
face("smiley_mittel.png", (250, 210, 70), "neutral")
face("smiley_schlecht.png", (235, 110, 95), "sad")
````

## Appendix E – `LIES_MICH.txt`

````text
EDV-Tag: Excel-Adressliste + Word-Serienbrief
==============================================

1_Fuer_Teilnehmende_auf_jeden_PC
   Diesen Ordner auf jeden Schulungs-PC kopieren (lokal, z. B. "Dokumente\EDV-Tag").
   Die drei Dateien muessen im selben Ordner bleiben.
   - Bewerbungsliste.xlsx       Datenquelle (eine Zeile pro Firma, Beispieldaten sind erfunden)
   - Anschreiben_Uebung.docx    Brief mit [Platzhaltern] zum Ueben
   - Anschreiben_Loesung.docx   gleicher Brief, Seriendruckfelder schon eingefuegt (Plan B / Demo)

2_Zum_Ausdrucken
   - Anleitung_Serienbrief      Schritt-fuer-Schritt-Anleitung, 1x pro TN
   - Feedbackbogen              Bewertung durch die TN am Ende, 1x pro TN

3_Fuer_Trainer
   - Ablaufplan_EDV-Tag         Zeitplan, Lernziele, Vorbereitung, Stolperfallen
   - Beispiel_Ergebnis.pdf      So sehen die 5 Briefe nach "Fertig stellen und zusammenfuehren" aus

WICHTIG: Vorher einmal komplett an einem Schulungs-PC durchspielen (Generalprobe).
````
