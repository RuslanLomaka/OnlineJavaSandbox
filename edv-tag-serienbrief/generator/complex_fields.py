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
