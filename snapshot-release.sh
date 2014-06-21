#!/bin/sh
# Snapshots a release APK and its Proguard files.
#
# Arguments:
#   $1 -- a release name (defaults to the current date)

DIR="BBQ Timer/build/outputs"
OUTPUT="releases/snapshot_${1:-`date "+%Y%m%d"`}.tar.bz2"
tar cjvf "${OUTPUT}" "${DIR}/apk/BBQ Timer-release.apk" \
    "${DIR}/proguard/release/mapping.txt"
