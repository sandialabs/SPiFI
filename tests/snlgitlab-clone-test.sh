#!/usr/bin/env bash

echo "----------------------------"
echo "Gitlab Clone Repository Test"
echo "----------------------------"
echo ""
repository_url="git@gitlab.sandia.gov:SEMS/sems-snl-gitlab-test-repository.git"
repository_dest="__sems-snl-gitlab-test-repository"

# turn on verbosity
set +x

git clone ${repository_url} ${repository_dest}


# turn on verbosity
set -x
