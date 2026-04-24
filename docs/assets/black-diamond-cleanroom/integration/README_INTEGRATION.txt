AESOLATOR Black Diamond Asset Pack v2
====================================

Included assets
----------------
- Launcher icon fallbacks in mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi
- Adaptive icon XMLs in res/mipmap-anydpi-v26/
- Foreground, background, monochrome layers
- Round launcher fallback icon
- Play Store 512 icon and master renders in extras/

Integration notes
-----------------
1. Copy the res/ directory into your Android project, merging with existing resources.
2. Ensure your AndroidManifest <application> uses:
     android:icon="@mipmap/ic_launcher"
     android:roundIcon="@mipmap/ic_launcher_round"
3. On Android 13+, themed icons will use ic_launcher_monochrome automatically via adaptive icon XML.
4. The adaptive foreground is safe-zone friendly and designed to survive circle, squircle, and rounded-square launcher masks.

Resource map
------------
res/mipmap-*/ic_launcher.png             standard launcher fallback
res/mipmap-*/ic_launcher_round.png       round launcher fallback
res/mipmap-*/ic_launcher_foreground.png  adaptive foreground layer
res/mipmap-*/ic_launcher_background.png  adaptive background layer
res/mipmap-*/ic_launcher_monochrome.png  themed/monochrome adaptive layer
res/mipmap-anydpi-v26/ic_launcher.xml    adaptive icon definition
res/mipmap-anydpi-v26/ic_launcher_round.xml adaptive round icon definition

Theme posture
-------------
- Launcher icon: premium dark circular badge
- Foreground: transparent, clean Android mascot and control-panel composition
- Background: dark green-black gradient for adaptive mask compatibility
- Monochrome: clean high-contrast themed-icon layer
