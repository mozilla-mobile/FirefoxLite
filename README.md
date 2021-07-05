## 🚨 Important notice about Firefox Lite

Effective June 30, 2021, this app will no longer receive security or other updates. Download the official Firefox Android app now for a fast, private & safe web browser. 

Learn more about the upcoming changes: 
https://support.mozilla.org/en-US/kb/end-support-firefox-lite

Download Firefox Android: 
https://play.google.com/store/apps/details?id=org.mozilla.firefox&hl=en_US

Privacy Notice:
https://www.mozilla.org/privacy/firefox-lite/

### About Mozilla
Mozilla exists to build the Internet as a public resource accessible to all because we believe open and free is better than closed and controlled. We build products like Firefox to promote choice and transparency and give people more control over their lives online. Learn more at https://www.mozilla.org

---

# Firefox Lite
[![Build Status](https://app.bitrise.io/app/2bee753c3b6709ca/status.svg?token=wKSNHE4YO8gQHd2W_I0tNg&branch=master)](https://app.bitrise.io/app/2bee753c3b6709ca)

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




Build instructions regarding Firebase
------------------

We're leveraging Firebase to offer some extra functionalities. However, Firebase is optional so normally you should be able to just develop on **focusWebkitDebug**.


Pull request checks
----
To mimimize the chance you are blocked by our build checks, you can self check these locally:
1. (build) run `./gradlew clean checkstyle assembleFocusWebkitDebug lint findbugs assembleAndroidTest ktlint`
2. (size check) run `python tools/metrics/apk_size.py focus webkit`
3. (Unit test) run `./gradlew testFocusWebkitDebugUnitTest`
4. (UI test) run `./gradlew connectedAndroidTest`

ktlint
----
- Download ktlint
```
curl -sSLO https://github.com/pinterest/ktlint/releases/download/0.30.0/ktlint &&
  chmod a+x ktlint &&
  sudo mv ktlint /usr/local/bin/
```
- Run `ktlint --install-git-pre-commit-hook` for hooks
- Run `./gradlew ktlint` or `ktlint` to run check
- Run `ktlint applyToIDEAProject` to make your IDE align with ktlint
- If you want to go extreme,run `ktlint -a -F`. This will use Android rule and gives you a lot of complains about max length, but we are not using it right now.
- See https://ktlint.github.io/ for details.

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
