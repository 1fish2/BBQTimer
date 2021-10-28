#!/bin/sh
# Resizes a widget preview image to multiple dpis.
#
# Arguments:
#   $1 -- input mdpi filename, defaults to ${1:-source-assets/widget_preview.png}
#
# Android resolutions:
#   ldpi    ≈120 dpi 0.75x
#   mdpi    ≈160 dpi 1x
#   hdpi    ≈240 dpi 1.5x
#   xhdpi   ≈320 dpi 2x
#   xxhdpi  ≈480 dpi 3x
#   xxxhdpi ≈640 dpi 4x
#   ^ 3:4:6:8:12:16 scaling ratio between the six primary densities

INFILE=${1:-source-assets/widget_preview.png}
DIR="app/src/main/res/drawable-"

function export_image() {
    echo convert ${INFILE}  -colorspace RGB  -resize ${1}%  -colorspace sRGB  ${DIR}${2}/widget_preview.png
    convert      ${INFILE}  -colorspace RGB  -resize ${1}%  -colorspace sRGB  ${DIR}${2}/widget_preview.png
}

export_image   75 ldpi
export_image  100 mdpi
export_image  150 hdpi
export_image  200 xhdpi
export_image  300 xxhdpi
export_image  400 xxxhdpi
