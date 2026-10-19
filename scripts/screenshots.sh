#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# The sample opens each screen from the `scene` extra and loads it live from the OpenF1 API, with the drivers'
# headshots and countries' flags from formula1.com. The race scenes look up the 2025 Abu Dhabi Grand Prix through
# the sessions endpoint at runtime. Each scene logs when its data and every image on screen have loaded; a scene that
# shows an error or does not finish loading fails the run. Every capture is also checked for the expected text and
# for a blank image.
#
#   bash scripts/screenshots.sh tablet   # pixel_tablet in landscape: classification and Norris's race, light and dark
#   bash scripts/screenshots.sh phone    # pixel_7: season, classification, driver grid, Norris's race, light and dark
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

device="${1:-phone}"

# OpenF1's free tier allows 30 requests a minute and a race scene makes nine. The app spaces its own requests, but
# every launch starts a fresh process, so leave a pause between launches as well.
PAUSE="${PAUSE:-15}"

# The text each scene must show; the capture fails without it. None of these appear on the error screen.
expected_text() {
  case "$1" in
    meetings) echo "Australian Grand Prix" ;;
    race) echo "Abu Dhabi Grand Prix" ;;
    drivers) echo "Abu Dhabi Grand Prix" ;;
    driver) echo "Lap times" ;;
    tablet) echo "Lap times" ;;
  esac
}

suffix() {
  if [ "$1" = dark ]; then echo "-dark"; else echo ""; fi
}

install_sample
if [ "$device" = tablet ]; then
  ensure_landscape
  for mode in light dark; do
    set_night_mode "$mode"
    fresh_launch --es scene tablet
    wait_for_scene tablet
    capture "tablet-race$(suffix "$mode")" "$(expected_text tablet)"
    sleep "$PAUSE"
  done
else
  for mode in light dark; do
    set_night_mode "$mode"
    for scene in meetings race drivers driver; do
      fresh_launch --es scene "$scene"
      wait_for_scene "$scene"
      capture "phone-$scene$(suffix "$mode")" "$(expected_text "$scene")"
      sleep "$PAUSE"
    done
  done
fi
