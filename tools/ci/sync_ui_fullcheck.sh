current_branch=$(git status | head -n 1)

if [[ $current_branch == "On branch master" ]]; then
    git checkout ui_fullcheck
	git merge --ff-only origin/master
	git push origin ui_fullcheck
	git checkout master
fi
