import argparse
import sys
import csv
import json


BASE_SCHEMA = {
    "source": "telemetry",
    "eventGroups": [
        {
            "eventGroupName": "Rocket",
            "events": []
        }
    ],
    "filters": {
        "docType": [
            "focus-event"
        ],
        "appName": [
            "Zerda",
            "OTHER"
        ],
        "os": [
            "Android"
        ]
    }
}

def get_event(name, categories, methods, objects, values, amplitude_props, descr=""):
    props = {
        "timestamp": {
            "type": "number",
            "minimum": 0
        },
        "category": {
            "type": "string",
            "enum": categories
        },
        "method": {
            "type": "string",
            "enum": methods
        },
        "object": {
            "type": "string",
            "enum": objects
        }
    }

    if values != [""]:
        props["value"] = {
                "type": "string",
                "enum": values
        }

    return {
        "name": name,
        "description": descr,
        "amplitudeProperties": amplitude_props,
        "schema": {
            "$schema": "http://json-schema.org/schema#",
            "type": "object",
            "properties": props,
            "required": list(props.keys())
        }
    },

def convert(filename):
    default = BASE_SCHEMA

    with open(filename, 'r') as f:
        events = list(csv.reader(f, delimiter=',', quotechar='"'))

    for e in events:
        name, cs, ms, os, vs, extra, aps = e

        vs = [x if x != "null" else None for x in vs.strip('\"').split(',')]
        amplitude_props = {}
        if extra:
            separated = extra.strip('\"').split(',')
            for e in separated:
                if '=' in e:
                    k, v = e.split("=")
                    amplitude_props[k] = "extra." + k

        default["eventGroups"][0]["events"] += get_event(name, [cs], [ms], [os], vs, amplitude_props)
    return default


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="CSV to Schema converter for Events to Amplitude",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    parser.add_argument(
            "filename",
            help = "CSV file with the data to convert"
    )
    args = parser.parse_args()
    print json.dumps(convert(args.filename), sort_keys = True, indent=4)