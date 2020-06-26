# Topic Mapping Pipeline - 2020 [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]
# Export Model Module

The Export Model module is the fourth module of the Topic Mapping Pipeline. It gathers the data generated by the 
[Topic Model module](ModelModule.md) to generate concise model data that can be uses by other applications. These
data are saved as ***Topic JSON file(s)*** and optionally as ***Document CSV file(s)***.

The Export Model module is contained in the `P3_TopicModelling` package.

## Specifications

The Export Model module entry in the project file should has the following structure:
```json5
{...
  "exportTopicModel": {
    "mainTopics": "path",
    "subTopics": "path",
    "documents": "path",
    "docFields": ["key", ... ],
    "mainOutput": "path",
    "subOutput": "path",
    "mainOutputCSV": "path",
    "subOutputCSV": "parh",
    "numWordId": 3
  }
...}
```
Where:
- `mainTopics` is the path to the topics JSON file (generated by the simple [Topic Modelling module](ModelModule.md)),
or to the main topics JSON file (generated by the [Hierarchical Topic Modelling module](ModelModule.md));
- `subTopics` is the path to the sub topics JSON file (generated by the
[Hierarchical Topic Modelling module](ModelModule.md)), it is optional and won't export sub topics if not 
specified;
- `documents` is the path to the documents JSON file (generated by the [Topic Modelling module](ModelModule.md));
- `docFields` specifies which key from the `docData` entry of documents should be exported, note that this list is
overwritten by the meta-parameter `docFields`;
- `mainOutput` is the path to the topic JSON file exported in which `mainTopics` are listed;
- `subOutput` is the path to the topic JSON file exported in which `subTopics` are listed, it is only required
 if sub topics are exported;
- `mainOutputCSV` is the path to the document CSV file listing documents and their weights in the main topics, it
is optional, and defaults to `""` meaning that no CSV should be created;
- `subOutputCSV` is the path to the document CSV file listing documents and their weights in the sub topics, it is
optional and defaults to `""` meaning that no CSV should be created, it is only used if sub topics are exported;
- `numWordId` is the number of labels to use to identify a topic in the document CSV files.

Note that sub-topics won't be exported if the meta-parameter `modelType` is set to `simple`.

## Output

The Export Model module outputs multiple files.

First, the topic JSON file, which follows a similar structure to the topic files generated by the
[Topic Model Modules](ModelModule.md):
```json5
{
  "metadata": { ... },
  "topics": [
    {
      "topicId": "0",
      "topicIndex": 0,
      "topDocs": [{
        "docId": "id", 
        "weight": 0.7778, 
        "docData": {
          "wordCount": 100,
          "key1": "value1",
          "key2": "value2",
          ...
        }
`     }, ... ],
      "topWords": [{"label": "risk", "weight": 85.0}, ... ],
      "subTopicIds": [ ... ],
      "mainTopicIds": [ ... ]
    }, ...
  ],
}
```
Note that `docData` has been added to each top document, containing a list of key-value pairs, following the 
`docFields` specification, as well as the `wordCount` for that document. 

Then, the document CSV file, if set in the specifications, following this structure:
```csv
"docId", "key1",   "key2",   ..., "wordCount", "includedInModel", "reasonForRemoval", "topic-1-labels", "topic-2-labels", ...
"0",     "value1", "value2", ..., "107",       "true",            "",                 "0.0197",         "0.0099",         ...
```
Each row represents a document, with `key1`, `key2`, etc. being the keys set in `docFields`. The CSV also includes the 
`wordCount` per document, whether the document was included in the model or not, and, if not, the reason for its 
removal (e.g. `"Too short"`). Finally, for each topic, identified by a list of their top labels, there is the weight of
that topic in the document.

---

[< Previous](ModelModule.md) | [Index](index.md) | [Next >](LabelIndexModule.md)

---
This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg