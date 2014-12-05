mkdir ./build/
mkdir ./build/packages/
hdiutil create ./build/packages/ClearVolume.dmg -volname "ClearVolume" -fs HFS+ -srcfolder "./build/distribution/"