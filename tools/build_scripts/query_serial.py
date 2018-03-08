#!/usr/bin/env python
# -*- coding: utf-8 -*-

import httplib2
import os
import argparse
import time

from googleapiclient import discovery
from google.oauth2 import service_account


SCOPES = ['https://www.googleapis.com/auth/spreadsheets']
SERVER_SECRET_FILE = '../bitrise-io.json'

def main(args):
    credentials = service_account.Credentials.from_service_account_file(
        SERVER_SECRET_FILE, scopes=SCOPES)
    service = discovery.build('sheets', 'v4',
                              credentials=credentials)

    spreadsheetId = os.environ['SPREADSHEET_ID']
    rangeName = 'Sample!A2:D'
    result = service.spreadsheets().values().get(
        spreadsheetId=spreadsheetId, range=rangeName).execute()
    values = result.get('values', [])

    if not values:
        print('No data found.')
    elif args.c:
        # consume a new serial number
        try:
            cur_max = max([x[1] for x in values if x[0] == args.v and x[3] == args.w])
        except:
            cur_max = 0
        new_max = int(cur_max) + 1
        value_body = {
            "majorDimension": "ROWS",
            "values": [[args.v, new_max, int(time.time()), args.w]]
        }
        response = service.spreadsheets().values().append(
            spreadsheetId=spreadsheetId, range=rangeName, body=value_body, valueInputOption='USER_ENTERED').execute()
        with open("build.serial", "w") as file:
            file.write("%04d" % new_max)
        print("New build serial %04d" % new_max)
        print(response)
    elif args.q:
        # print current max
        try:
            cur_max = int(max([x[1] for x in values if x[0] == args.v and x[3] == args.w]))
            with open("build.serial", "w") as file:
                file.write("%04d" % cur_max)
            print("Current maximum build serial %04d" % cur_max)
        except:
            print("No data found.")

def get_parser():
    parser = argparse.ArgumentParser(description='Choose parsers')
    parser.add_argument('-q', action='store_true', default=False, help='Query max serial number')
    parser.add_argument('-c', action='store_true', default=False, help='consume a new serial number')
    parser.add_argument('-v', default='2.0.0', help='version constraint')
    parser.add_argument('-w', default='nightly', help='workflow')
    return parser

if __name__ == '__main__':
    print(os.getcwd())
    if not os.path.exists(SERVER_SECRET_FILE):
        import sys
        sys.exit(1)
    parser = get_parser()
    args = parser.parse_args()
    print(args)
    main(args)
