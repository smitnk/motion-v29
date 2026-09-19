# MotionCanvas V18

## Two new features
1. Batch timeline operations: duplicate, delete and move frame ranges.
2. Built-in diagnostics: validate timeline, audio clip bounds and reference transform values.

## Bug-finding status
Static guardrails were added, but a real device/emulator build and interaction test is still required before calling the app bug-free. Known risk areas include bitmap memory pressure, codec/device differences, and complex gesture/timeline state synchronization.

## Open-source approach
Feature concepts were compared against open-source animation editors such as ToonFrame Studio, Pixelorama and Celstomp. Source code is not copied from repositories with incompatible/proprietary licensing.