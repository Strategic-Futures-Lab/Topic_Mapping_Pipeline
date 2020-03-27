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

def load_csv(url):
	with open(url) as csv_file:
		data = []
		for row in csv.reader(csv_file, delimiter=','):
			data.append(row)
		return data


supTMUrl = './data/SE_sup_model.json'

supTM = load_json(supTMUrl)

supTM_metadata = supTM['metadata']
supTM_failedRetrievals = supTM['failedRetrievals']
supTM_rowData = supTM['rowData']
supTM_timeSlices = supTM['timeSlices']
supTM_topicDetails = supTM['topicDetails']
supTM_topicSimilarities = supTM['topicSimilarities']

TMUrl = './SE_sup_model_small.json'
# write_json(TMUrl, {'topicDetails': supTM_topicDetails,
# 				   'topicSimilarities': supTM_topicSimilarities})
write_json(TMUrl, {'topics': [[y['label'] for y in x['topWords']] for x in supTM_topicDetails ]})