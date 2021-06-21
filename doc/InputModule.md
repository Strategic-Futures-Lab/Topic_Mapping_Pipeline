# Topic Mapping Pipeline - 2020 [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]
# Input Modules

The purpose of Input modules is to format the input text data into a standard ***Corpus JSON file***, that will be read
by the next module in the pipeline.

The Input modules are all contained within the `P1_Input` package.

## List of Input Modules

There is currently 4 input module available:
- ***CSV Input*** (`CSVInput.java` class) which reads document data structured in a CSV file;
- ***PDF Input*** (`PDFInput.java` class) which parses a collection of pdf files in a directory; 
- ***GTR Input*** (`GTRInput.java` class) is specific to the Gateway to Research (GtR) API, it reads document data
  from a CSV file, which must contain a GtR project ID, and will also crawl for additional data from GtR's website;
- ***TXT Input*** (`TXTInput.java` class) which reads documents from a `.txt` file or from a directory of `.txt` files.

## Specifications

The parameters for the Input module in the project file should have the following structure:
```json5
{...
  "input": {
    "module": "module name", 
    "source": "path",
    "output": "path",
    // CSV and GTR inputs only
    "fields": {"key": "value", ...},
    // GTR input only
    "GtR_id": "key",
    "GtR_fields": {"key": "value", ...},
    // PDF and TXT inputs only
    "wordsPerDoc" : -1,
    // TXT input only
    "txt_splitEmptyLines": false
  },
...}
``` 

Where:
- `module` points to the module to use:
  - `"CSV"` for the CSV Input;
  - `"PDF"` for the PDF Input;
  - `"GTR"` for the GTR Input;
  - `"TXT"` for the TXT Input;
- `source` is the path to the input file or directory (depending on the module used):
  - CSV or GTR: path to a `.csv` file;
  - PDF: path to a directory containing `.pdf` files;
  - TXT: path to single `.txt` file or a directory containing `.txt` files;
- `output` is the path to the output corpus JSON file.
  
For CSV and GTR inputs:
- `fields` details the document attributes to read from the CSV formatted data input, for example:
  - the input `.csv` file has the columns `A,B,C,D,E`, and `fields` has the value `{"b":"B","d":"D"}`, then each
    document saved in the corpus file will have `"docData":{"b":...,"d":...}` with the values from columns `B` and `D` respectively;

For the GTR input only:
- `GtR_id` indicates which column, in the input `.csv` file, contains the GtR project ids to use for fetching data on GtR's website;
- `GtR_fields` follows the same structure as `fields` (keys are the fields you can query, see below, and values are
  how they should be saved in the corpus) but instead details which fields to fetch from GtR's website, the
  supported fields are:
  - `"Abstract"` reads the project's abstract;
  - `"TechAbstract"` (rare) reads the project technical abstract;
  - `"Impact"` reads the project's impact;
  - `"Title"` reads the project's title;
  - `"Funder"` reads the project's funder (i.e., research council);
  - `"Institution"` reads the project's lead institution;
  - `"Investigator"` reads the project's principal investigator;
  - `"StartDate"` reads the project start date;
  - `"EndDate"` reads the project end date.

For PDF and TXT inputs:
- `wordsPerDoc` sets the maximum size (in number of words) the document corpus should have. This allows for the
  chunking of large files. It defaults to `-1` meaning that no chunking is done.

For the TXT input only:
- `txt_splitEmptyLines` is a boolean flag for considering empty lines in the `.txt` file(s) as document separators,
  i.e., one file could contain several documents separated empty lines. 

## Output

Every input module generate a corpus JSON file with the same structure:
```json5
{
  "metadata":{
    "totalDocs": 1000
  },
  "corpus":[
    {
      "docId": "0",
      "docIndex": 0,
      "docData": {"key": "value", ...}
    }, ...
  ]
}
```

The `metadata` only contains the number of documents read by the module (additional metadata will be added by other
modules). Then the file has a `corpus` list, with one object per document with the following information:
- `docId` the document id;
- `docIndex` the document index;
- `docData` the document data, as key-value pairs, note that every value will be saved in a String format.

---

[< Previous](MetaParameters.md) | [Index](index.md) | [Next >](LemmatiseModule.md)

---
This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
