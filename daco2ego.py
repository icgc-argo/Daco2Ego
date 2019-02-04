#!/usr/bin/env python3
import json
from collections import OrderedDict
import sys
from aes import decrypt_file
from ego_client import EgoClient
from requests import Session
import csv

from daco_client import DacoClient

def read_config(name="config/default.conf"):
    with open(name) as f:
         conf = json.load(f)
    return conf

def users(data):
    text = data.decode()
    csvreader = csv.DictReader(text.splitlines())
    return OrderedDict([ (u['openid'], u['user name']) for u in csvreader])

def send_report(issues):
    for issue in issues:
        print(issue) 

def init(args):
    if args:
        config = read_config(args[0])
    else:
        config = read_config()

    key = config['aes']['key']
    iv  = config['aes']['iv']

    daco_users = users(decrypt_file(config['daco_file'], key, iv))
    cloud_users = users(decrypt_file(config['cloud_file'], key, iv))
    auth_token = config['client']['auth_token']
    base_url   = config['client']['base_url']
    verbose_log = config['verbose']

    rest_client = Session()
    ego_client = EgoClient(base_url, auth_token, rest_client)
    client = DacoClient(daco_users, cloud_users, ego_client,verbose=verbose_log)

    return client

def main(_program_name, *args):
    issues = []

    try:
        daco_client = init(args)
    except Exception as e:
        # Scenario 5 (Start-up failed)
        issues.append("Initialization error:" + str(e))
    else:
        # Scenarios 1,2,3,4,6
        issues = daco_client.update_ego()
    send_report(issues)

if __name__ == "__main__":
   main(*sys.argv)
