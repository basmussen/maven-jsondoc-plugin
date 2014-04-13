#!/bin/bash
start=$(date +%s)
echo -e "Current repo: $TRAVIS_REPO_SLUG\n"

# git
git config --global user.email "travis@travis-ci.org"
git config --global user.name "travis-ci"

git clone -q --branch=gh-pages https://${GH_TOKEN}@github.com/basmussen/maven-jsondoc-plugin.git gh-pages > /dev/null 2>&1 || error_exit "Error cloning repository";

#git add .
#git commit -q -m "Travis build $TRAVIS_BUILD_NUMBER"
#git push -fq origin gh-pages > /dev/null 2>&1 || error_exit "Error uploading artifacts"

# processing time
end=$(date +%s)
elapsed=$(( $end - $start ))
minutes=$(( $elapsed / 60 ))
seconds=$(( $elapsed % 60 ))
echo "Post-Build process finished in $minutes minute(s) and $seconds seconds"
