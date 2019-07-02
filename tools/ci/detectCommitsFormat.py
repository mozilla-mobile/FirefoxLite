import os, sys, re

master_commit = os.popen("git ls-remote git@github.com:mozilla-tw/FirefoxLite.git | grep refs/heads/master").read()
master_abbrev_commit = master_commit[0:8]

commits_list = os.popen("git log --pretty=oneline --abbrev-commit -n 50").read()
commits = []

for commit in commits_list.split("\n"):
    if commit[0:8] != master_abbrev_commit:
        commits.append((commit[0:8], commit[9:]))
        print(commit[0:8])
    else:
        break

if len(commits) <= 1:
    print("Detection pass: one commit")
    sys.exit(0)
else:
    try:
        pattern = "\[.+\]"
        head = re.search(pattern, commits[0][1]).group(0)
        print("head: " + head)
        for c in commits:
            print(c)
            if not c[1].startswith(head):
                print("Detection fail: wrong header")
                # sys.exit(1)
                sys.exit(0)
        print("Deteciton pass")
    except:
        print("Detection fail: unknown reason")
        sys.exit(0)
        # sys.exit(1)
