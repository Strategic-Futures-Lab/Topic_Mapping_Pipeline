# Topic Mapping Pipeline [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

[< Previous](ExportModule.md) | [Index](index.md) | [Next >](TopicDistributionModule.md)

---

# Label Index Module

The Label Index module reads the top words from topic data to create an index of labels which is then saved in 
an ***Index JSON file***.

The use of this module is optional.

The Label Index module is contained in the `P4_Analysis.LabelIndex` package, in the `LabelIndexing.java` class.

## Specifications

The Label Index module entry in the project file should have the following structure:
```json5
{...
  "indexLabels": {
    "topics" | "mainTopics": "path",
    "subTopics": "path",
    "documents": "path",
    "useAllDocuments": false,
    "useAllLabels": false,
    "output": "path"
  },
...}
``` 

| Name | Description | Optional | Default |
| --- | --- | --- | --- |
| `topics` or `mainTopics` (if the model is hierarchical) | Path to the (main) topics JSON file * | No | |
| `subTopics` | Path to the sub topics JSON file * | Yes | `""` (no indexing of labels in sub topics) ** |
| `documents` | Path to the documents JSON file * | Yes | `""` (no indexing of labels in documents) |
| `useAllDocuments` | Flag for indexing labels from all documents, not just the top documents of each topic | Yes, only needed if `documents` is set | `false` (only index from top documents in topics) |
| `useAllLabels` | Flag for indexing all labels (in documents), not just the top labels of each topic  | Yes, only needed if `documents` is set | `false` (only index top labels in topics) |
| `output` | Path to the output index JSON file *** | No | |
- \* These paths are relative to the [data directory](MetaParameters.md);
- \** This default value implies a non-hierarchical model, if the [model type meta-parameter](MetaParameters.md) is set to `hierarchical`, a path must be provided;
- \*** This path is relative to the [output directory](MetaParameters.md).

## Output

The Label Index module generates an index JSON file with the following structure:
```json5
{
  "label1":{
    "mainTopics": ["id", ... ],
    "subTopics": [["id", ["mainTopicId", ... ]], ... ],
    "documents": ["id", ... ]
  },
  "label2":{
    "mainTopics": ["id", ... ],
    "subTopics": [["id", ["mainTopicId", ... ]], ... ],
    "documents": ["id", ... ]
  },
...}
```

It lists all the labels indexed and attaches an object to them, with the following lists:
- `mainTopics` the list of topic ids from the `mainTopics` JSON file containing this label;
- `subTopics` the list of tuple, with the first item being a topic id from the `subTopics` JSON file containing this 
label, and the second item the list of ids for their assigned main topics;
- `documents` the list of document ids from the `documents` JSON file containing this label.

---

[< Previous](ExportModule.md) | [Index](index.md) | [Next >](TopicDistributionModule.md)

This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
