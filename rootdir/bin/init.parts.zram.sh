#!/vendor/bin/sh
#
# Copyright (C) 2026 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

size_mb=$(getprop sys.parts.zram.size_mb)
algo=$(getprop sys.parts.zram.algorithm)

[ -e /sys/block/zram0/reset ] || exit 0

swapoff /dev/block/zram0 2>/dev/null
echo 1 > /sys/block/zram0/reset

if [ -n "$algo" ] && [ -w /sys/block/zram0/comp_algorithm ]; then
    echo "$algo" > /sys/block/zram0/comp_algorithm
fi

if [ -z "$size_mb" ] || [ "$size_mb" -le 0 ] 2>/dev/null; then
    exit 0
fi

echo "${size_mb}M" > /sys/block/zram0/disksize
mkswap /dev/block/zram0
swapon /dev/block/zram0 -p 32758
