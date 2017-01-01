#!/bin/sh
# Converts the Inkscape icon file ic_launcher_web.svg to the launcher web & app png files.

INPUT="source-assets/ic_launcher_web.svg"
MAIN="app/src/main/"
RES="${MAIN}res/"
DRAWABLE="${RES}drawable"

function launcher_icon() {
    echo --export-png "'${DRAWABLE}-${1}dpi/ic_launcher.png'" -w ${2} "'${INPUT}'"
}

function large_notification_icon() {
    echo --export-png "'${DRAWABLE}-${1}dpi/ic_large_notification.png'" -w ${2} "'${INPUT}'"
}

inkscape --shell <<COMMANDS
  --export-png "${MAIN}ic_launcher-web.png"         -w 512 "${INPUT}"

  `launcher_icon m     48`
  `launcher_icon h     72`
  `launcher_icon xh    96`
  `launcher_icon xxh  144`
  `launcher_icon xxxh 192`

  `large_notification_icon m     64`
  `large_notification_icon h     96`
  `large_notification_icon xh   128`
  `large_notification_icon xxh  192`
  `large_notification_icon xxxh 256`

  quit
COMMANDS
