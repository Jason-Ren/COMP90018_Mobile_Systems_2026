# Heads Up forehead pose

Duo and clash mode can start automatically from the Ready screen. Hold the phone
sideways above the forehead with its screen upright and facing the other player
for at least 600 ms. Either landscape direction works. A three-second countdown
then starts the round. Leaving the pose for 900 ms pauses the countdown, card
timer and scoring; returning resumes the same card with the remaining time.
Brief up/down scoring tilts are tolerated by wider hold thresholds and the exit
delay. Pausing the app or losing the clash connection remains an independent
hold: returning to the pose cannot clear those holds.

Tap **Start** to use manual play instead. A manually started round does not
require the pose. Devices without an available sensor (including failed listener
registration) keep manual Start. Replaying an automatically started round waits
on Ready for the pose; manually started replays keep the existing countdown.

`ForeheadPoseClassifier` accepts timestamped gravity samples, independently of
Android APIs, as the integration point for Person 6's sensor fusion primitives.
The Android adapter currently prefers fused `TYPE_GRAVITY`, then
`TYPE_GAME_ROTATION_VECTOR` / `TYPE_ROTATION_VECTOR` (world-up projected into
device coordinates), then an accelerometer with a 200 ms low-pass filter. No
camera, location permission or heading calibration is needed. Sensor listeners
run only while the screen is resumed and reset when stopped.
The Compose activity handles orientation/screen-size changes without recreation,
so entering landscape does not create a background pause during auto-start.

The entry orientation requires normalized absolute X >= 0.85 and absolute Z <=
0.40; the wider hold limits are X >= 0.65 and Z <= 0.65. Magnitudes outside
7–12 m/s² are rejected, and gaps above 250 ms reset the transition dwell.
Android's sensor coordinates stay fixed to the device's natural orientation,
independent of display rotation. This assumes a phone with natural portrait
orientation; landscape-native tablets would need an axis remap.

Orientation cannot establish the phone's physical height or detect a forehead.
An upright landscape phone elsewhere can also trigger this heuristic. Android
documents gravity and rotation vectors as orientation signals in its
[motion sensor guide](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion).

## Device verification

1. Open a nonempty Duo deck. Hold portrait, face-up, then face-down: no auto-start.
2. Hold at the forehead in each landscape direction: one countdown starts.
3. Lower the phone during 3–2–1: the number holds. Return: it continues.
4. Lower during a card front: the card/time stay fixed and taps cannot reveal it.
5. Lower while the back is shown: taps and tilts cannot score until returning.
6. Try quick up/down scoring tilts and slight hand jitter: no unintended pause.
7. Background/foreground the app: pose return alone does not clear a background
   hold on a front/countdown; tap Resume after returning to the pose.
8. Finish and replay: pose play re-arms on Ready without reusing a paused timer.
9. Tap Start manually (or use a device with no motion sensor): ordinary play works.
10. In clash mode, disconnect/reconnect while pose-paused: the clock runs only
    after the connection and pose holds have both cleared.

Unit tests cover thresholds, dwell, invalid data, jitter, scoring tilts, sample
gaps/reset and round transitions. Real device testing is needed to tune the
thresholds and exit delay against different grips and scoring motions.
