#git log --pretty=oneline --abbrev-commit -n 40 | python revertCommitParser.py
import re, sys

# with open('log', 'r') as f:
#     commits = f.readlines()
commits = sys.stdin.readlines()

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
