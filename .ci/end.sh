#!/bin/bash -ex

ccache -s -v

if [ ! -z "${ANDROID_KEYSTORE_B64}" ]; then
    chmod +x /home/runner/work/Lemonade/Lemonade/src/android/app/ks.jks
    rm "/home/runner/work/Lemonade/Lemonade/src/android/app/ks.jks"
fi
