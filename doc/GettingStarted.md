# Topic Mapping Pipeline [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

[Index](index.md) | [Next >](SystemOverview.md)

---

# Getting Started

The pipeline is written in Java, version 11. You will therefore need to have a JRE and JDK that supports this version.

The sources are in the `src` folder. Before compiling and running sources, make sure you have downloaded the
dependencies in the `lib` folder and make sure  to add them to the project structure in your IDE:
- `fastcsv-1.0.2.jar` for reading and writing csv files;
- `json-simple-1.1.1.jar` for reading and writing json files;
- `pdfbox-app-2.0.9.jar` for reading pdf documents;
- `stanford-corenlp-3.9.2.jar` and `stanford-corenlp-3.9.2-models.jar` for lemmatising text;
- `mallet.jar` and `mallet-deps.jar` for modelling topics;
- `jbox2d-library-2.2.1.1.jar` for mapping topics.

To handle version control of these large dependencies, you might need to install [Git LFS](https://git-lfs.github.com/).

A compiled JAR file of the pipeline can be found in the `out/artifacts/Topic_Mapping_Pipeline_jar` folder.
Make sure to download the main `.jar` file and the dependencies in the `lib` folder.

> Although it has been deprecated, the previous version of the mapping module has been written in JavaScript.
  If you wish to use this version, you will need to install [NodeJS](https://nodejs.org/en/) to execute JavaScript.

---

[Index](index.md) | [Next >](SystemOverview.md)

This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
