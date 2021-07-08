# Topic Mapping Pipeline [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

[< Previous](MetaParameters.md) | [Index](index.md) | [Next >](LemmatiseModule.md)

---

# Input Modules

The purpose of Input modules is to format the input text data into a standard ***Corpus JSON file***, that will be read
by the next module in the pipeline.

The Input modules are all contained within the `P1_Input` package.

## List of Input Modules

There are currently 4 input modules available:
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

| Name | Description | Optional | Default |
| --- | --- | --- | --- |
| `module` | Module to use: `"CSV"`, `"PDF"`, `"GTR"` or `"TXT"` | No | |
| `source` | Path to the input file or directory * | No | |
| `output` | Path to the output corpus JSON file ** | No | |
- \* This path is relative to the [source directory](MetaParameters.md):
  - the CSV and GTR modules accept a `.csv` file;
  - the PDF module accept a directory input containing `.pdf` files;
  - the TXT module accepts either a single `.txt` file or a directory containing `.txt` files.
- \** This path is relative to the [data directory](MetaParameters.md).

Additional parameters are available depending on the module used.

For CSV and GTR inputs:

| Name | Description | Optional | Default |
| --- | --- | --- | --- |
| `fields` | Document attributes to read from the `.csv` file, e.g., input file has columns `A,B,C` and `fields` is set to `{"a":"A","c":"C"}`, then documents will have `"docData":{"a":...,"c":...}` with the values from columns `A` and `C` respectively | No | |

For the GTR input only:

| Name | Description | Optional | Default |
| --- | --- | --- | --- |
| `GtR_id` | Column name, in the input `.csv` file, containing the GtR project ids to use for fetching data on GtR's website | Yes | `"ProjectId"` |
| `GtR_fields` | List of fields to fetch from GtR's website * | Yes | `{}` (empty object) |
  
- \* Follows the same structure as `fields` above: keys are fields to query, values are how they should be saved on file.
  Supported fields are:
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

| Name | Description | Optional | Default |
| --- | --- | --- | --- |
| `wordsPerDoc` | Maximum size (in number of words) the document corpus should have, allowing for the chunking of large files | Yes | `-1` (no maximum, does not split texts) |

For the TXT input only:

| Name | Description | Optional | Default |
| --- | --- | --- | --- |
|`txt_splitEmptyLines` | Flag for considering empty lines in the `.txt` file(s) as document separators, i.e., one file could contain several documents separated by empty lines | Yes | `false` (one document per file) | 

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

The content of `docData` varies with the input module used.

With the CSV module:
- only the fields explicitly filled in `fields`.

With the GTR module:
- fields explicitly filled in `fields`;
- a `PID` field with the project identifier values found with `GtR_id`;
- API fields filled in `GtR_fields`.

With the PDF module:
- a `text` field with the parsed document content;
- a `dataset` field with the direct parent directory name for each `.pdf` file;
- a `filename` field with the file name for each `.pdf` file;
- in case the files' contents were split:
  - a `splitNumber` field indicating the position of the text portion in the whole document;
  - a `wordRange` field indicating the start and end word indices of the text portion;
  - a `pageRange` field indicating the start and end page numbers of the text portion;
  - `filename` becomes the `.pdf` file name + the split number;
  - a `originalFilename` field gets the `.pdf` file name only.
  
With the TXT module, similar to the PDF module:
- a `text` field with the parsed document content;
- a `dataset` field with the direct parent directory name for each `.txt` file;
- a `filename` field with the file name for each `.txt` file;
- in case the files' contents were split:
  - a `splitNumber` field indicating the position of the text portion in the whole document;
  - `filename` becomes the `.txt` file name + the split number;
  - a `originalFilename` field gets the `.txt` file name only.

---

[< Previous](MetaParameters.md) | [Index](index.md) | [Next >](LemmatiseModule.md)

This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
