# Topic Mapping Pipeline [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

## Documentation Index
- [Getting Started](GettingStarted.md)
- [System Overview](SystemOverview.md)
- [Meta-Parameters](MetaParameters.md)
- Modules
    - [Input](InputModule.md)
    - [Lemmatise](LemmatiseModule.md)
    - [Topic Model](ModelModule.md)
        - [Infer Documents](InferenceModule.md)
        - [Export Topic Model](ExportModule.md)
    - Analysis
        - [Label Indexing](LabelIndexModule.md)
        - [Topic Distribution](TopicDistributionModule.md)
            - [Compare Distribution](CompareDistributionModule.md)
        - [Topic Clustering](TopicClusteringModule.md)
    - [Topic Mapping](TopicMappingModule.md)
        - [Overwrite Map](OverwriteMapModule.md)

## References

### Latent Dirichlet Allocation

> David M. Blei, Andrew Y. Ng, and Michael I. Jordan. 2003.
> Latent dirichlet allocation. In *Journal of machine Learning research*, vol. 3, pp. 993–1022.
```bibtex
@Article{Blei2003,
  author    = {Blei, David M. and Ng, Andrew Y. and Jordan, Michael I.},
  journal   = {Journal of machine Learning research},
  title     = {Latent dirichlet allocation},
  year      = {2003},
  pages     = {993--1022},
  volume    = {3},
  publisher = {JMLR. org},
}
```

### Gibbs Sampling

> Thomas L. Griffiths and Mark Steyvers. 2004.
> Finding scientific topics. In *Proceedings of the National academy of Sciences*, vol. 101, suppl. 1, pp. 5228–5235.
```bibtex
@Article{Griffiths2004,
  title     = {Finding scientific topics},
  author    = {Griffiths, Thomas L and Steyvers, Mark},
  journal   = {Proceedings of the National academy of Sciences},
  volume    = {101},
  number    = {suppl 1},
  pages     = {5228--5235},
  year      = {2004},
  publisher = {National Acad Sciences}
}
```

### Bubble Treemap

> Jochen Görtler, Christoph Schulz, Daniel Weiskopf, and Oliver Deussen. 2018.
> Bubble Treemaps for Uncertainty Visualization. In *IEEE Transactions on Visualization and Computer Graphics*, vol. 24 issue 1, pp. 719-728.
```bibtex
@Article{Goertler2018,
  author     = {Görtler, Jochen and Schulz, Christoph and Weiskopf, Daniel and Deussen, Oliver},
  doi        = {10.1109/TVCG.2017.2743959},
  journal    = {IEEE Transactions on Visualization and Computer Graphics},
  number     = {1},
  pages      = {719-728},
  title      = {Bubble Treemaps for Uncertainty Visualization},
  volume     = {24},
  year       = {2018},
  url        = {http://graphics.uni-konstanz.de/publikationen/Goertler2018BubbleTreemapsUncertainty},
}
```

## Third-Party Software

### Stanford CoreNLP

Website: [stanfordnlp.github.io/CoreNLP](https://stanfordnlp.github.io/CoreNLP/)

License: [GNU General Public License v3 (GPL-3)](https://tldrlegal.com/license/gnu-general-public-license-v3-(gpl-3))

Cite: 
> Christopher D. Manning, Mihai Surdeanu, John Bauer, Jenny Finkel, Steven J. Bethard, and David McClosky. 2014.
> The Stanford CoreNLP Natural Language Processing Toolkit. In *Proceedings of the 52nd Annual Meeting of the Association for Computational Linguistics: System Demonstrations*, pp. 55-60.
```bibtex
@InProceedings{Manning2014,
  author    = {Manning, Christopher D. and  Surdeanu, Mihai  and  Bauer, John  and  Finkel, Jenny  and  Bethard, Steven J. and  McClosky, David},
  title     = {The {Stanford} {CoreNLP} Natural Language Processing Toolkit},
  booktitle = {Association for Computational Linguistics (ACL) System Demonstrations},
  year      = {2014},
  pages     = {55--60},
  url       = {http://www.aclweb.org/anthology/P/P14/P14-5010}
}
```

### Mallet

Website: [mallet.cs.umass.edu](http://mallet.cs.umass.edu/)

License: [Common Public License 1.0 (CPL-1.0)](https://tldrlegal.com/license/common-public-license-1.0-(cpl-1.0))

Cite:
> Andrew Kachites McCallum. 2002.
> MALLET: A Machine Learning for Language Toolkit. [http://mallet.cs.umass.edu](http://mallet.cs.umass.edu)
```bibtex
@unpublished{McCallum2002,
  author     = {McCallum, Andrew Kachites},
  title      = {MALLET: A Machine Learning for Language Toolkit},
  note       = {http://mallet.cs.umass.edu},
  year       = {2002},
}
```

### JBox2D

Website: [www.jbox2d.org](http://www.jbox2d.org/)

License: [BSD 2-Clause License (FreeBSD/Simplified)](https://tldrlegal.com/license/bsd-2-clause-license-(freebsd))

### FastCSV

GitHub: [github.com/osiegmar/FastCSV](https://github.com/osiegmar/FastCSV)

License: [MIT License (Expat)](https://tldrlegal.com/license/mit-license)

### JSON-Simple

GitHub: [github.com/fangyidong/json-simple](https://github.com/fangyidong/json-simple)

License: [Apache License 2.0 (Apache-2.0)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

### PDFBox

Website: [pdfbox.apache.org](https://pdfbox.apache.org/)

License: [Apache License 2.0 (Apache-2.0)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

<!--
No longer using technically.

### D3 - Data Driven Documents

[BSD 3-Clause License](https://tldrlegal.com/license/bsd-3-clause-license-(revised))

Cite:
> Michael Bostock, Vadim Ogievetsky, and Jeffrey Heer. 2011.
> D<sup>3</sup>: Data Driven Documents. In *IEEE Transactions on Visualization and Computer Graphics*, vol. 17 issue 12, pp. 2301-2309.

```bibtex
@article{bostock2011d3,
  title      = {D$^3$ data-driven documents},
  author     = {Bostock, Michael and Ogievetsky, Vadim and Heer, Jeffrey},
  journal    = {IEEE transactions on visualization and computer graphics},
  volume     = {17},
  number     = {12},
  pages      = {2301--2309},
  year       = {2011},
  publisher  = {IEEE}
}
```

### Bubble Treemap

[BSD 3-Clause License](https://tldrlegal.com/license/bsd-3-clause-license-(revised))

Cite:
> Jochen Görtler, Christoph Schulz, Daniel Weiskopf, and Oliver Deussen. 2018.
> Bubble Treemaps for Uncertainty Visualization. In *IEEE Transactions on Visualization and Computer Graphics*, vol. 24 issue 1, pp. 719-728.
```bibtex
@article{Goertler2018,
  author     = {Görtler, Jochen and Schulz, Christoph and Weiskopf, Daniel and Deussen, Oliver},
  doi        = {10.1109/TVCG.2017.2743959},
  journal    = {IEEE Transactions on Visualization and Computer Graphics},
  number     = {1},
  pages      = {719-728},
  title      = {Bubble Treemaps for Uncertainty Visualization},
  volume     = {24},
  year       = {2018},
  url        = {http://graphics.uni-konstanz.de/publikationen/Goertler2018BubbleTreemapsUncertainty},
}
```
-->

---
This work is licensed under a [Creative Commons Attribution 4.0 International
License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: http://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://i.creativecommons.org/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
