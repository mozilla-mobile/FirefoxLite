# Firefox Lite
[![Bitrise build status](https://app.bitrise.io/app/2bee753c3b6709ca.svg?token=wKSNHE4YO8gQHd2W_I0tNg&branch=master)](https://www.bitrise.io/app/2bee753c3b6709ca)

Getting Involved
----------------

We encourage you to participate in this open source project. We love Pull Requests, Bug Reports, ideas, (security) code reviews or any kind of positive contribution. Please read the [Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/).

* Issues: [https://github.com/mozilla-tw/FirefoxLite/issues](https://github.com/mozilla-tw/FirefoxLite/issues)

Build instructions
------------------

1. Clone the repository:

  ```shell
  git clone https://github.com/mozilla-tw/FirefoxLite
  ```
2. Since we're using submodule, run:

  ```shell
git submodule init
git submodule update
  ```


3. Open Android Studio and select File->Open and select FirefoxLite to open the project. Make sure to select the right build variant in Android Studio: **focusWebkitDebug**

4. Disable Instant Run in: Settings/Build, Execution, Deployment > Instant Run. (See note #1 for details.)

[1] We currently don't support instant run. An error message similar to:
```
/Users/tyu/Documents/zerda/FirefoxLite/app/src/main/java/org/mozilla/focus/utils/FirebaseHelper.java:29: error: cannot find symbol
final public class FirebaseHelper extends FirebaseWrapper {
                                          ^
  symbol: class FirebaseWrapper
```
will shown when building with instant run. We have an [issue](https://github.com/mozilla-tw/FirefoxLite/issues/2143) for this so feel free to help with this limitation. To disable instant run , press Cmd+Shit+a on Mac or Ctrl+Shift+a on Windows and enter "instant run" under "Preference" category. If it still doesn't work, try enter `./gradlew clean"` on mac or `gradlew clean` on Windows using command line in the project root.

Build instructions regarding Firebase
------------------

We're leveraging Firebase to offer some extra functionalities. However, Firebase is optional so normally you should be able to just develop on **focusWebkitDebug**.

Here are some Firebase build workarounds that you may need during normal development:

1. If you want to run UI test without setting up firebase tokens, remove `testBuildType "firebase"` from [build.gradle](https://github.com/mozilla-tw/FirefoxLite/blob/4fedf245c4382122283ca8ec701a5ff18c9bf779/app/build.gradle#L122) or you will most likely see error message like this when you run connectedAndroidTest directly:

```
No tests found. This usually means that your test classes are not in the form that your test runner expects (e.g. don't inherit from TestCase or lack @Test annotations).
```

If you'd like to test the fully functional Firebase build, you should first setup the environment variables following these [instructions](https://github.com/mozilla-tw/Rocket/blob/4fedf245c4382122283ca8ec701a5ff18c9bf779/app/build.gradle#L346) and select **focusWebkitFirebase**

Pull request checks
----
To mimimize the chance you are blocked by our build checks, you can self check these locally:
1. (build) run `./gradlew clean checkstyle assembleFocusWebkitDebug lint findbugs assembleAndroidTest ktlint`
2. (size check) run `python tools/metrics/apk_size.py focus webkit`
3. (Unit test) run `./gradlew testFocusWebkitDebugUnitTest`
4. (UI test) run `./gradlew connectedAndroidTest`

ktlint
----
Run `ktlint --install-git-pre-commit-hook` or `ktlint --install-git-pre-push-hook` for hooks
Run `./gradlew ktlint` or `ktlint` to run check
Run `./gradlew ktlintformat -F` or `ktlint -F` to fix format
If you want to go extreme,run `ktlint -a -F`. This will use Android rule and gives you a lot of complains about max length, but we are not using right now.
See https://ktlint.github.io/ for details.

Docs
----

* [Content blocking](docs/contentblocking.md)
* [Translations](docs/translations.md)
* [Search](docs/search.md)
* [Telemetry](docs/telemetry.md)

License
-------

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
