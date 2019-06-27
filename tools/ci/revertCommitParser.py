#git log --pretty=oneline --abbrev-commit -n 40 | python revertCommitParser.py
import re, sys

# with open('log', 'r') as f:
#     commits = f.readlines()
commits = sys.stdin.readlines()

pattern = "\[.+\]"

last_commit = commits[0][:8]
try:
    head = re.search(pattern, commits[0].rstrip()[9:]).group(0)

    for line in commits:
        commit, comment = line[:8], line.rstrip()[9:]
        if comment.startswith(head):
            last_commit = commit
        else:
            break
except:
    pass

print(last_commit)
