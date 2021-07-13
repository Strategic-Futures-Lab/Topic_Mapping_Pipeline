# Topic Mapping Pipeline [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

[< Previous](InputModule.md) | [Index](index.md) | [Next >](ModelModule.md)

---

# Lemmatise Module

The Lemmatise module *cleans* the corpus and produce lemmatised text data for the topic modelling process. 
This data is saved in a ***Lemma JSON file***.

The Lemmatise module is contained in the `P2_Lemmatise` package, in the `Lemmatise.java` class.

## Specifications

The parameters for Lemmatise module entry in the project file should have the following structure:
```json5
{...
  "lemmatise": {
    "corpus": "path",
    "output": "path",
    "textFields": ["key", ...],
    "docFields": ["key", ...],
    "stopWords": ["word", ...],
    "stopPhrases": ["many words", ...],
    "minDocLemmas" | "minLemmas": 1,
    "minLemmaCount": 0
  },
...}
```

| Name | Description | Optional | Default |
| --- | --- | --- | --- |
| `corpus` | Path to the corpus JSON file * | No | |
| `output` | Path to the output lemmas JSON file * | No | |
| `textFields` | List of keys, in the `docData` of documents, to use and build the documents texts with | No | |
| `docFields` | List of keys to keep in the `docData` of documents after lemmatisation (e.g. for further analysis/presentation) ** | Yes | `[]` |
| `stopWords` | List of lemmatised words to excluded from documents' texts (after lemmatisation) *** | Yes | `[]` |
| `stopPhrases` | List of phrases, or groups of words, to exclude from documents' texts (before lemmatisation) *** | Yes | `[]` |
| `minDocLemmas` or `minLemmas` | Minimum number of lemmas a document must have in its text to be kept for modelling | Yes | `1` |
| `minLemmaCount` | Minimum number of times a lemma must be counted, across all documents **** | Yes | `0` |
- \* These paths are relative to the [data directory](MetaParameters.md);
- \** This gets overwritten by the [document fields meta-parameter](MetaParameters.md) (if set);
- \*** This lets you filter out terms which are too generic or over-represented in the corpus;
- \**** This lets you filter out under-represented lemmas, note that **using this option may remove a large amount of information**.

## Output

The Lemmatise module generates a lemma JSON file which follow a similar structure to the corpus file:
```json5
{
  "metadata":{
    "nDocsTooShort": 5,
    "minDocSize": 10,
    "totalDocs": 1000,
    "stopWords":"",
    "stopPhrases": "",
    "nLemmasRemoved": 3,
    "minLemmaCount": 1
  },
  "lemmas":[
    {
      "docId": "0",
      "docIndex": 0,
      "lemmas": "lemma1 lemma2 ...",
      "numLemmas": 100,
      "docData": {"key": "value1", ...}
    },{
      "docId": "1",
      "docIndex": 1,
      "tooShort": true,
      "lemmas": "lemma4 lemma5",
      "numLemmas": 2,
      "docData": {"key": "value2", ...}
    }, ...
  ]
}
```

In addition to the number of documents (`totalDocs`), the `metadata` now also contains:
- the number of documents removed for being too small (`nDocsTooShort`);
- the minimum number of lemmas a document should have to be kept (`minDocSize`);
- the list of stop words (`stopWords`);
- the list of stop phrases (`stopPhrases`);
- if `minLemmaCount` was set above `0` in the specifications:
  - the minimum number of times a lemma should be present in the vocabulary (`minLemmaCount`);
  - the number of lemmas removed from the vocabulary (`nLemmasRemoved`).

Then the file has a `lemmas` list, with one object per document with the following information:
- `docId` the document id;
- `docIndex` the document index;
- `lemmas` the string containing the lemmatised text for that document;
- `numLemmas` the number of lemmas in that document;
- `docData` the document data that was kept with `docFields`;
- if the document was too short (as per the setting of `minDocLemmas` in the specifications), the documents is
  flagged as such with a boolean (`tooShort`).

---

[< Previous](InputModule.md) | [Index](index.md) | [Next >](ModelModule.md)

This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
