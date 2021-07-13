# Topic Mapping Pipeline [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

[< Previous](TopicMappingModule.md) | [Index](index.md) <!-- | [Next >]() -->

---

# Overwrite Map Module

The Overwrite Map module creates a ***Map JSON file*** based on existing mappable data (from an existing Map JSON file), 
and with new topic data, such as labels and topic size (as computed by the [Topic Distribution module](TopicDistributionModule.md)).

The use of this module is optional, and only fits with the use of the [Document Inference module](InferenceModule.md):
with new documents having their topic distribution inferred, topics have new weights.

The Overwrite Map module is contained in the `P5_TopicMapping` package, in the `OverwriteMap.java` class.

## Specifications

The Overwrite Map module entry in the project file should have the following structure:
```json5
{...,
  "overwriteMap": {
    "distribution" | "mainDistribution": "path",
    "subDistribution": "path",
    "map" | "mainMap": "path",
    "subMaps": "path",
    "mapOutput" | "mainMapOutput": "path",
    "subMapsOutput": "path",
    "sizeName": "distributionName",
    "overwriteLabels": false
  },
...}
```

| Name | Description | Optional | Default |
| --- | --- | --- | --- |
| `distribution` or `mainDistribution` (if the model is hierarchical) | Path to the distributed (main) topics JSON file, with updated topics * | No | |
| `subDistribution` | Path to the sub topics JSON file, with updated topics * | Required if the model is hierarchical | `""` ** |
| `map` or `mainMap` (if the model is hierarchical) | Path to the existing (main) map JSON file *** | No | |
| `subMaps` | Path to the existing sub map JSON file *** | Required if the model is hierarchical | |
| `mapOutput` or `mainMapOutput` (if the model is hierarchical) | Path to the output new (main) map JSON file **** | No | |
| `subMapsOutput` | Path to the output new sub map JSON file **** | Required if the model is hierarchical | |
| `sizeName` | Name of the topic distribution to use to update topic size data in the map | Yes | `""` (no update) |
| `overwriteLabels` | Flag for updating the topics' top labels in the map | Yes | `false` (no update) |
- \* These paths are relative to the [data directory](MetaParameters.md);
- \** This default value implies a non-hierarchical model, if the [model type meta-parameter](MetaParameters.md) is set to `hierarchical`, a path must be provided;
- \*** These paths are relative to the [source directory](MetaParameters.md);
- \**** These paths are relative to the [output directory](MetaParameters.md).

## Output

The output of the Overwrite Map module is similar to the output of the [Topic Mapping module](TopicMappingModule.md).

---

[< Previous](TopicMappingModule.md) | [Index](index.md) <!-- | [Next >]() -->

This work is licensed under a [Creative Commons Attribution 4.0 International License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
