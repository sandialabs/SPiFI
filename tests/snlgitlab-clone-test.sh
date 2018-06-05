#!/usr/bin/env bash

echo "----------------------------"
echo "Gitlab Clone Repository Test"
echo "----------------------------"
echo ""
repository_url="git@gitlab.sandia.gov:SEMS/sems-snl-gitlab-test-repository.git"
repository_dest="__sems-snl-gitlab-test-repository"

if [ -e ${repository_dest} ]; then
    echo ""
    echo "CLEANUP> ${repository_dest} exists, removing it."
    echo ""
    rm -rf ${repository_dest}
fi

# turn on verbosity
set -x

git clone ${repository_url} ${repository_dest}

# turn off verbosity
set +x

status=$?
if [ $status -ne 0 ]; then
    echo ""
    echo "ERROR> git exited with nonzero exit code: ${status}"
    echo ""
    exit 1
else
    echo ""
    echo "QAPLA> git exited with status: ${status}"
    echo ""
fi

