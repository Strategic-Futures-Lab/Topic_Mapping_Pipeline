# Topic Mapping Pipeline - 2020 [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]
# Input Modules

The purpose of Input modules is to format the input text data into a standard ***Corpus JSON file***, that will be read
by the next module in the pipeline.

The Input modules are contained within the `P1_Input` package.

## List of Input Modules

There is currently only one input module available:
- ***CSV Input*** (`CSVInput.java` class) which reads document data organised in a CSV file.

## Specifications

The Input module entry in the project file should have the following structure:
```json5
{...
  "input": {
      "module": "module name",
      "source": "path",
      "fields": {"key": "value", ...},
      "output": "path"
  }
...}
``` 

Where:
- `module` points to the module to use:
    - `"CSV"` for the CSV Input;
- `source` is the path to the input file or directory (depending on sub module used);
- `output` is the path to the output corpus JSON file;
- `fields` details the document attributes to read from a formatted data input (CSV or JSON), for example:
    - the input csv file has the columns `A,B,C,D,E`, and `fields` has the value `{"a":"A","b":"B","d":"D"}`, then each
    document saved in the corpus file will have `"docData":{"a":...,"b":...,"d":...}` with the values from columns `A`,
    `B`, and `D` respectively;

## Output

Each sub module generate a corpus JSON file with a standard structure:
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
- `docData` the document data, as key-values pairs, note that every value will be saved in a String format.

---

[< Previous](SystemOverview.md) | [Index](index.md) | [Next >](LemmatiseModule.md)

---
This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
