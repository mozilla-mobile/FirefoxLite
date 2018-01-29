# Rocket
[![BuddyBuild](https://dashboard.buddybuild.com/api/statusImage?appID=594b679a6ed87e0001fca2cd&build=latest)](https://dashboard.buddybuild.com/public/apps/594b679a6ed87e0001fca2cd/build/latest)
[![Nevercode build status](https://app.nevercode.io/api/projects/9c39e5e8-f3b2-4ab1-a016-fa93789299e3/workflows/e004af8e-ae21-4083-84de-d1a6b1ef7571/status_badge.svg?branch=master)](https://app.nevercode.io/#/project/9c39e5e8-f3b2-4ab1-a016-fa93789299e3/workflow/e004af8e-ae21-4083-84de-d1a6b1ef7571/latestBuild?branch=master)

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

1. Import the project into Android Studio or build on the command line:

  ```shell
  ./gradlew clean app:assembleFocusWebkitDebug
  ```

1. Make sure to select the right build variant in Android Studio: **focusWebkitDebug**

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
