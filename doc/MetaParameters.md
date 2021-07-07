# Topic Mapping Pipeline [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

---

[< Previous](SystemOverview.md) | [Index](index.md) | [Next >](InputModule.md)

---

# Meta-Parameters

The meta-parameters allow you to complete or overwrite module specifications at a project-wide scope.

## Directories
```json5
{...
  "metaParameters": {
    "projectDir": "files/",
    "sourceDir": "projects/test/",
    "dataDir": "output/tmp/",
    "outputDir": "output/",
    ...
  },
... }
```

The directories meta-parameters allow you to set the directory of data file for the whole project.

| Name | Description |
| --- | --- |
| `projectDir` | The top-level directory for the project. |
| `sourceDir` | The directory of any of the input sources. |
| `dataDir` | The directory for all the temporary data files generated. |
| `outputDir` | The directory for all the final data files generated. |

Examples of input sources:
- input corpus files/directories for [Input Modules](InputModule.md);
- previous serialised models for [the Inference Module](InferenceModule.md) as well as previous distribution
  files or map files to compare and/or overwrite after having inferred documents.

Examples of temporary data files:
- corpus from the [Input Modules](InputModule.md);
- lemmas from the [Lemmatise Module](LemmatiseModule.md);
- model files from the [Model Modules](ModelModule.md);
- topic distribution files from the [Distribution Module](TopicDistributionModule.md);
- topic clusters files from the [Cluster Module](TopicClusteringModule.md).


Examples of final data files:
- model export files from the [Export Model Module](ExportModule.md);
- inferred documents files from the [Document Inference Module](InferenceModule.md);
- label index from the [Label Index Module](LabelIndexModule.md);
- separate distribution files from the [Distribution Module](TopicDistributionModule.md);
- map data files from the [Mapping Modules](TopicMapping.md).

All of these parameters are optional, and will only come to complete the filenames used at module level with the 
structure: 
> projectDir + [ sourceDir | dataDir | outputDir ] + filename

By default, they will be set to `""`, hence not affect the filenames set by modules.

## Model Type
```json5
{...
  "metaParameters": {
    ...
    "modelType": "hierarchical",
    ...
  },
... }
```
Because modules after `Model` will behave differently depending on the nature of the model made, you can use the
meta-parameter `modelType` to quickly set those behaviours. It takes two possible values: `hierarchical` and `simple`.
It is optional, and will overwrite the module-level specification if set.

## Doc Fields
```json5
{...
  "metaParameters": {
    ...
    "docFields": ["title", "university", "money", "authors", "date"]
  },
... }
```
`Lemmatise`, `InferDocuments` and `ExportModel` are modules which will set or export a list of document data fields.
Because it is often the case that those lists are identical, you can use the meta-parameter `docFields` to set
these module specifications in one place. It is optional, and will overwrite the module-level specification if set.

---

[< Previous](SystemOverview.md) | [Index](index.md) | [Next >](InputModule.md)

---

This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
