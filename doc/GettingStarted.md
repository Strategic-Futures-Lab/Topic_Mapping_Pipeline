# Topic Mapping Pipeline - 2020 [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]
# Getting Started

The pipeline is programed in Java. There are no compiled `.jar` library yet, you must therefore either run it through
a Java IDE (i.e. IntelliJ IDEA, or Eclipse), or compile and run via command line.

The sources are in the `src` folder.
    
Before compiling and running sources, make sure you have downloaded the dependencies in the `lib` folder and make sure
to add them to the project structure in your IDE:
- `fastcsv-1.0.2.jar` for reading and writing csv files;
- `json-simple-1.1.1.jar` for reading and writing json files;
- `pdfbox-app-2.0.9.jar` for reading pdf documents (not used in this version of the pipeline yet);
- `stanford-corenlp-3.9.2.jar` and `stanford-corenlp-3.9.2-models.jar` for lemmatising text;
- `mallet.jar` and `mallet-deps.jar` for modelling topics.

---

[Index](index.md) | [Next >](System Overview)

---
This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
