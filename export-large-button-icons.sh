#!/bin/sh
# Converts the Inkscape large-button icon files ic_*.svg to ic_*_big.png files.
#
# NOTE: Save these .svg files with their bg layers hidden, then run this script,
# then restore the .svg files.

PROJECT="BBQ Timer"
INPUT="source-assets/large-button-icons/"
MAIN="${PROJECT}/src/main/"
RES="${MAIN}res/"
DRAWABLE="${RES}drawable"

function export_icon() {
  echo --export-png "'${DRAWABLE}-${1}dpi/${3}_big.png'" -w ${2} "'${INPUT}${3}.svg'"
}

function export_icons() {
  export_icon m     64 ${1}
  export_icon h     96 ${1}
  export_icon xh   128 ${1}
  export_icon xxh  192 ${1}
}

inkscape --shell <<COMMANDS
  `export_icons ic_pause`
  `export_icons ic_play`
  `export_icons ic_replay`
  `export_icons ic_stop`

  quit
COMMANDS
