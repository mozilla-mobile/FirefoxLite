# Rocket
[![Bitrise build status](https://app.bitrise.io/app/2bee753c3b6709ca.svg?token=wKSNHE4YO8gQHd2W_I0tNg&branch=master)](https://www.bitrise.io/app/2bee753c3b6709ca)

Getting Involved
----------------

We encourage you to participate in this open source project. We love Pull Requests, Bug Reports, ideas, (security) code reviews or any kind of positive contribution. Please read the [Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/).

* Issues: [https://github.com/mozilla-tw/Rocket/issues](https://github.com/mozilla-tw/Rocket/issues)

Build instructions
------------------

1. Clone the repository:

  ```shell
  git clone https://github.com/mozilla-tw/Rocket
  ```

2. Make sure to select the right build variant in Android Studio: **focusWebkitDebug**

3. Disable Instant Run in: Settings/Build, Execution, Deployment > Instant Run. (See note #1 for details.)

[1] We currently don't support instant run. An error message similar to:
```
/Users/tyu/Documents/zerda/Rocket/app/src/main/java/org/mozilla/focus/utils/FirebaseHelper.java:29: error: cannot find symbol
final public class FirebaseHelper extends FirebaseWrapper {
                                          ^
  symbol: class FirebaseWrapper
```
will shown when building with instant run. We have an [issue](https://github.com/mozilla-tw/Rocket/issues/2143) for this so feel free to help with this limitation.

Build instructions regarding Firebase
------------------

We're leveraging Firebase to offer some extra functionalities. However, Firebase is optional so normally you should be able to just develop on **focusWebkitDebug**.

Here are some Firebase build workarounds that you may need during normal development:

1. If you want to run UI test, remove `testBuildType "firebase"` from [build.gradle](https://github.com/mozilla-tw/Rocket/blob/4fedf245c4382122283ca8ec701a5ff18c9bf779/app/build.gradle#L122)

If you'd like to test the fully functional Firebase build, you should first setup the environment variables following these [instructions](https://github.com/mozilla-tw/Rocket/blob/4fedf245c4382122283ca8ec701a5ff18c9bf779/app/build.gradle#L346) and select **focusWebkitFirebase**

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
