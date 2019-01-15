**The blockly-android project is _no longer actively developed_ by the
Google Blockly team. We will continue to review and accept pull requests.
If you are looking to create a new Blockly-based app, we recommend using
the [web version](http://github.com/google/blockly) in a WebView.  We
have also create an
[Android Studio demonstration project](https://github.com/google/blockly/tree/develop/demos/mobile/android)
that does exactly this.**

[Blockly][1] is a library for building drag-and-drop visual editors for
JavaScript and other programming languages.  [Blockly for Android][2] is
a developer preview of Blockly's editor built with standard Android
views and fragments, offering more responsive touch performance and
easier integration in Android apps.

![Blockly Turtle demo running on an Android tablet.](http://google.github.io/blockly-android/screenshot.png
    "Blockly Turtle demo running on an Android tablet.")

[Get started][3] building your own Android app using Blockly.

Google is proud to offer Blockly for Android for free and open source
under the [Apache License, version 2.0][4].

Blockly has an active [developer forum][5]. Please drop by and say hello. Show
us your prototypes early; collectively we have a lot of experience and can
offer hints which will save you time.

Help us focus our development efforts by telling us [what you are doing with
Blockly][6]. The questionnaire only takes a few minutes and will help us better
support the Blockly community.

Want to contribute? Great! First, read [our guidelines for contributors][7].

### How To Use

Add one of the following dependencies to your application's `build.gradle`.

```gradle
dependencies {
    // Default (Full)
    implementation 'com.google.blockly.android:blocklylib-vertical:<Blockly Version>'
    // Full Version (All code lanuage generators)
    implementation 'com.google.blockly.android:blocklylib-vertical:<Blockly Version>:full@aar'
    // Lite Version (Only JavaScript)
    implementation 'com.google.blockly.android:blocklylib-vertical:<Blockly Version>:lite@aar'
}
```

[1]: https://developer.google.com/blockly/ "Blockly documentation"
[2]: https://github.com/google/blockly-android "Blockly for Android repository on GitHub"
[3]: https://developer.google.com/blockly/guides/get-started/android "Blockly for Android developer tutorial"
[4]: https://github.com/google/blockly-android/blob/master/COPYING "Apache open source license, version 2.0"
[5]: https://groups.google.com/forum/#!forum/blockly "Blockly developer forum"
[6]: https://developers.google.com/blockly/registration "Blockly developer registration form"
[7]: https://github.com/google/blockly-android/blob/master/CONTRIBUTING.md "Contributor guidelines"

