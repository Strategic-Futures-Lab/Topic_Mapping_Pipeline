# Topic Mapping Pipeline [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

[< Previous](CompareDistributionModule.md) | [Index](index.md) | [Next >](TopicMapping.md)

---

# Topic Clustering Module

The Topic Clustering module primarily produces a hierarchical clustering for a group of topics, in the form of a 
linkage table. If sub topics are given it will first generate groups of sub topics according to their assignment to 
main topics: i.e. if two or more sub topics were assigned to the same main topic, a group containing these sub topics 
will be created for that main topic. Note that if sub topics were assigned to more than one main topic, they will be 
duplicated and present in multiple groups.

The linkage tables (and groups) are directly saved in ***Topic JSON file(s)***.

The use of this module is necessary for the mapping of topics.

The Topic Clustering module is contained in the `P4_Analysis.TopicClustering` package, in the `TopicClustering.java`
class.

## Specifications

The Topic Clustering module entry in the project file should have the following structure:
```json5
{...
  "clusterTopics": {
    "topics" | "mainTopics": "path",
    "subTopics": "path",
    "linkageMethod": "max",
    "clusters": 2,
    "output" | "mainOutput": "path",
    "subOutput": "path"
  },
...}
```

| Name | Description | Optional | Default |
| --- | --- | --- | --- |
| `topics` or `mainTopics` (if the model is hierarchical) | Path to the (main) topics JSON file * | No | |
| `subTopics` | Path to the sub topics JSON file * | Required if the model is hierarchical | `""` ** |
| `linkageMethod` | Method to use when merging items during hierarchical clustering, see below | Yes | `"avg"` |
| `clusters` | Number of clusters to identify in the set of (main) topics | Yes | `1` |
| `output` or `mainOutput` (if the model is hierarchical) | Path to the output clustered (main) topics JSON file to generate * | No | |
| `subOutput` | Path to the output clustered sub topics JSON file to generate * |  Required if the model is hierarchical | |
- \* These paths are relative to the [data directory](MetaParameters.md);
- \** This default value implies a non-hierarchical model, if the [model type meta-parameter](MetaParameters.md) is set to `hierarchical`, a path must be provided.

When building the hierarchical clustering, the module will explore the topics' distance matrix to estimate which are 
closest. As it does so, it merges topics together and *collapses* the distance matrix by calculating new distances  
between the merged topics and the other items in the matrix. `linkageMethod` refers to the type of calculation done:
- `avg` gets the average distance between topics;
- `min` gets the minimum distance between topics;
- `max` gets the maximum distance between topics.

## Output

Used on main topics, the Topic Clustering module generates and adds the linkage table data to the main  topic JSON file:
```json5
{...
  "linkageTable": [
    {
      "node1": 2,
      "node2": 9,
      "distance": 0.6991
    },...
  ]
}
```
`node1` and `node2` both points to either a topic index in the list of topics (if the value is less than the number
of topics), or to a node index in the linkage table itself (if the value is greater than or equal to the number of topics).
`distance` is the distance at which the two nodes join in the hierarchy.

It also adds a `clusterId` field to each main topic with a string identifier value.

Used on the sub topics, the Topic Clustering module reorganises the list of topics into a list of sub topic groups:
```json5
{...
  "subTopicGroups": [
    {
      "mainTopicId": "11",
      "topics": [
        {
          "topicId": "4",
          "topicIndex": 4,
          "groupTopicId": "0",
          "groupTopicIndex": 0,
          "topDocs": [ ... ],
          "topWords": [ ... ],
          "mainTopicIds": [ "11", ... ],
          "distributions": [ ... ],
          "totals": [ ... ]
        }, ...
      ],
      "similarities": [ [ ... ], ... ],
      "linkageTable": [ ... ]
    }, ...
  ]
}
```
Each sub topic group contains:
- `mainTopicId` the id of the main topic to which these sub topics were assigned;
- `topics` the list of sub topics in that group, note the addition of two fields:
    - `groupTopicId` which identifies this topic in the context of that group;
    - `groupTopicIndex` to point to that topic in the context of that group, e.g. in the similarity matrix or the
    linkage table;
- `similarities` the topic to topic similarity matrix for topics of that group;
- `linkageTable` with the same structure as presented above, for topics of that group.

---

[< Previous](CompareDistributionModule.md) | [Index](index.md) | [Next >](TopicMapping.md)

This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
