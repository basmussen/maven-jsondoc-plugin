#!/bin/bash
start=$(date +%s)
echo -e "Current repo: $TRAVIS_REPO_SLUG\n"

# git
git config --global user.email "travis@travis-ci.org"
git config --global user.name "travis-ci"

git clone -q --branch=gh-pages https://${GH_TOKEN}@github.com/basmussen/maven-jsondoc-plugin.git gh-pages

# processing time
end=$(date +%s)
elapsed=$(( $end - $start ))
minutes=$(( $elapsed / 60 ))
seconds=$(( $elapsed % 60 ))
echo "Before-Build process finished in $minutes minute(s) and $seconds seconds"
