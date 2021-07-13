# Topic Mapping Pipeline [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

[Index](index.md) | [Next >](SystemOverview.md)

---

# Getting Started

## Running the Topic Mapping Pipeline

1. You will need to have a Java Runtime Environment (JRE) installed on your machine, version 11 at least.

2. Download the pipeline's JAR from the [release page](https://github.com/strategicfutureslab/Topic_Mapping_Pipeline/releases).

3. Create a `project.json` file:
    - follow the rest of this guide for more details, starting with the [System Overview](SystemOverview.md);
    - check `files/project.json` for an example;
    - note that in the `project.json` paths are relative to the location of `Topic_Mapping_Pipeline.jar` on your machine.

4. Run the pipeline using the following command:
```shell
$ java -jar -Xmx4g Topic_Mapping_Pipeline.jar project.json
```
The `-Xmx` option lets you adjust the maximum heap size of the application, in this instance 4 Gigabytes. 

## Developing the Topic Mapping Pipeline

1. You will need to have a Java Development Kit (JDK) installed on your machine, version 11 at least.

2. Download the project:
    - `build` contains the pipeline JAR build;
    - `doc` contains all the user documentation (don't forget to update it as you modify, add, remove things);
    - `files` contains an example `project.json` file and sample data files to test the application;
    - `lib` contains all the project dependencies, see below;
    - `src` contains the sources.
  
3. Before compiling and running sources, make sure your IDE uses the correct JDK, and that you have added the 
   dependencies to your project structure:
    - `fastcsv-1.0.2.jar` for reading and writing csv files;
    - `json-simple-1.1.1.jar` for reading and writing json files;
    - `pdfbox-app-2.0.9.jar` for reading pdf documents;
    - `stanford-corenlp-3.9.2.jar` and `stanford-corenlp-3.9.2-models.jar` for lemmatising text;
    - `mallet.jar` and `mallet-deps.jar` for modelling topics;
    - `jbox2d-library-2.2.1.1.jar` for mapping topics.
  
4. The main class to use for compilation and run is `src/TopicMapping`:
    - don't forget to instruct your IDE to take `project.json` as parameter;
    - you can also instruct your IDE to adjust the maximum heap size  of the application using the `-Xmx` option.
   
5. When setting up the build process, to create an updated JAR of the pipeline:
    - you should instruct your IDE to extract dependencies to the target JAR file;
    - make sure to publish the [new release](https://github.com/strategicfutureslab/Topic_Mapping_Pipeline/releases/new).
   
6. The `.idea` folder and `Topic_Mapping_Pipeline.iml` file should allow you to set up the project with IntelliJ IDEA.

Note on using Git:
- You might need to install [Git LFS](https://git-lfs.github.com/) to handle version control of large files, such as 
  some JAR files.

> Although it has been deprecated, the previous version of the mapping module has been written in JavaScript (`js_scripts` folder).
  If you wish to use this version, you will need to install [NodeJS](https://nodejs.org/en/) to execute JavaScript.

---

[Index](index.md) | [Next >](SystemOverview.md)

This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
