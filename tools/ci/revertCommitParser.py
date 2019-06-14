import re

with open('log', 'r') as f:
    commits = f.readlines()

pattern = "\[.+\]"

last_commit = commits[0][:7]
try:
    head = re.search(pattern, commits[0].rstrip()[8:]).group(0)

    for line in commits:
        commit, comment = line[:7], line.rstrip()[8:]
        if comment.startswith(head):
            last_commit = commit
        else:
            break
except:
    pass

print(last_commit)
