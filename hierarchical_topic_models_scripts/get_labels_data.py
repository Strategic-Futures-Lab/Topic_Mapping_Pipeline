import json
from collections import OrderedDict
import csv
import copy
import time


def write_json(url, data):
	open(url, 'w').write(json.dumps(data))

def load_json(url):
	with open(url) as json_file:
   		return json.loads(json_file.read(), object_pairs_hook=OrderedDict)

labelsData = {}

nWords = 10

supMapUrl = 'data/SE_sup_map.json'
supMap = load_json(supMapUrl)
supTopics = supMap['conceptsData']
supLabels = map(lambda x:[x['conceptId'], map(lambda y: y['label'], x['labels'][:nWords])], supTopics)
for l in supLabels:
    for label in l[1]:
        if label != "" and label != "Unclassified":
            if label in labelsData:
                labelsData[label].append([l[0]])
            else:
                labelsData[label] = [[l[0]]]

for i in range(0, 60):
    subMapUrl = 'data/SE_sub_map_'+str(i)+'.json'
    subMap = load_json(subMapUrl)
    subTopics = subMap['conceptsData']
    subLabels = map(lambda x:[x['conceptId'], map(lambda y: y['label'], x['labels'][:nWords])], subTopics)
    for l in subLabels:
        for label in l[1]:
            if label != "" and label != "Unclassified":
                if label in labelsData:
                    labelsData[label].append([i, l[0]])
                else:
                    labelsData[label] = [[i, l[0]]]

write_json('SE_label_index_'+str(nWords)+'.json', labelsData)

# print(labelsData)