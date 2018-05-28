# Firefox Rocket for Android

## Telemetry

For clients that have "Send usage data" enabled Rocket sends a "core" ping and an "event" ping to understand the usage of the app. This can be disabled in the app's settings. It's enabled by default ("opt-out").

### Core ping

Firefox Rocket creates and tries to send a "core" ping whenever the app goes to the background. This core ping uses the same format as Firefox for Android and is [documented in Firefox Source Docs](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/data/core-ping.html).

### Event ping

In addition to the core ping an event ping for UI telemetry is generated and sent as soon as the app is sent to the background. The same information will be sent to [Firebase Analytics](https://firebase.google.com/terms/analytics/)
You can find the list of events in the [wiki page](https://github.com/mozilla-tw/Rocket/wiki/Telemetry)

#### Events

The event ping contains a list of events ([see event format on Firefox Source Docs](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/collection/events.html)) for the following actions:


#### Limits

* An event ping will contain up to but no more than 500 events
* No more than 40 pings per type (core/event) are stored on disk for upload at a later time
* No more than 100 pings are sent per day

### Implementation notes

* Event pings are generated (and stored on disk) whenever the onStop() callback of the main activity is triggered. This happens whenever the main screen of the app is no longer visible (The app is in the background or another screen is displayed on top of the app).

* Whenever we are storing pings we are also scheduling an upload. We are using Android’s JobScheduler API for that. This allows the system to run the background task whenever it is convenient and certain criterias are met. The only criteria we are specifying is that we require an active network connection. In most cases this job is executed immediately after the app is in the background.

* Whenever an upload fails we are scheduling a retry. The first retry will happen after 30 seconds (or later if there’s no active network connection at this time). For further retries a exponential backoff policy is used: [30 seconds] * 2 ^ (num_failures - 1)

* An earlier retry of the upload can happen whenever the app is coming to the foreground and sent to the background again (the previous scheduled job is reset and we are starting all over again).
