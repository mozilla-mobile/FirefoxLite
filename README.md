# Rocket
[![Bitrise build status](https://www.bitrise.io/app/2bee753c3b6709ca/status.svg?token=wKSNHE4YO8gQHd2W_I0tNg&branch=master)](https://www.bitrise.io/app/2bee753c3b6709ca)

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

2. Import the project into Android Studio or build on the command line:

  ```shell
  ./gradlew clean app:assembleFocusWebkitDebug
  ```

3. Make sure to select the right build variant in Android Studio: **focusWebkitDebug**

4. If you want to run UI test, remove  `c "firebase"` from [build.gradle](https://github.com/mozilla-tw/Rocket/blob/master/app/build.gradle#L121)

5. Some build types use Firebase, replace "firebase"  in [matchingFallbacks](https://github.com/mozilla-tw/Rocket/blob/master/app/build.gradle#L87) to "firebase_no_op" if you don't want it. Otherwise, you need to setup the environment variables (https://github.com/mozilla-tw/Rocket/blob/master/buildSrc/src/main/java/Dependencies.kt#L35)

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
