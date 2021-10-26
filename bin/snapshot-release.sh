#!/bin/sh
# Snapshots a release APK and its Proguard files.
#
# Arguments:
#   $1 -- a release name (defaults to the current date)

# TODO: Include all of ${DIR}?
# TODO: Is the snapshot moot with App Bundles, which apparently auto-uploads mapping.txt?
#    https://developer.android.com/studio/build/shrink-code#decode-stack-trace

# TODO: Also zip up the symbols for uploading to the Play console?
# per https://developer.android.com/studio/build/shrink-code
#   (cd app/build/intermediates/cmake/universal/release/obj; zip -r symbols.zip .)

DIR="app/build/outputs"
OUTPUT="tmp/snapshot_${1:-`date "+%Y%m%d"`}.tar.bz2"
tar cjvf "${OUTPUT}" "${DIR}/apk/BBQTimer-release.apk" \
    "${DIR}/mapping/release/mapping.txt"
