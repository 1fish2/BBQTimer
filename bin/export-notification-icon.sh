#!/bin/sh
# Converts the Inkscape notification icon file to the app res png files at multiple resolutions.
#
# NOTE: Save the doc with the "background WHILE EDITING" layer hidden, then export, then make
# that layer visible again so the icon doesn't look (invisible) white on white.
# Unfortunately, Inkscape can't set the doc background to transparent and display it over a
# non-white canvas.

PROJECT="BBQ Timer"
IMAGE="notification_icon"
INPUT="source-assets/${IMAGE}.svg"
OUTPUT="${IMAGE}.png"
MAIN="${PROJECT}/src/main/"
RES="${MAIN}res/"
DRAWABLE="${RES}/drawable"

inkscape --shell <<COMMANDS
  --export-png "${DRAWABLE}-mdpi/${OUTPUT}"    -w  24 "${INPUT}"
  --export-png "${DRAWABLE}-hdpi/${OUTPUT}"    -w  36 "${INPUT}"
  --export-png "${DRAWABLE}-xhdpi/${OUTPUT}"   -w  48 "${INPUT}"
  --export-png "${DRAWABLE}-xxhdpi/${OUTPUT}"  -w  72 "${INPUT}"
  --export-png "${DRAWABLE}-xxxhdpi/${OUTPUT}" -w  96 "${INPUT}"
quit
COMMANDS
