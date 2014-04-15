#!/bin/bash
start=$(date +%s)
echo -e "Current repo: $TRAVIS_REPO_SLUG\n"

# git
git config --global user.email "travis@travis-ci.org"
git config --global user.name "travis-ci"
git config --global push.default simple

cd gh_pages

git add .
git commit -q -m "Travis build $TRAVIS_BUILD_NUMBER"
git push -q

# processing time
end=$(date +%s)
elapsed=$(( $end - $start ))
minutes=$(( $elapsed / 60 ))
seconds=$(( $elapsed % 60 ))
echo "Post-Build process finished in $minutes minute(s) and $seconds seconds"
