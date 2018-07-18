# Firefox Rocket for Android

## Updating translations

Firefox Rocket for Android is getting localized on [Pontoon](https://pontoon.mozilla.org/projects/firefox-rocket/).

For converting between Android XML files and Gettext PO files (to be consumed by Pontoon) we use a local, slightly modified version of [android2po](https://github.com/miracle2k/android2po) (See `tools/l10n/android2po`).

### Setup

1. Python, Pip and Git need to be installed.

2. Run following command to install required dependencies.

  ```shell
  pip install lxml babel termcolor
  ```

### Export strings for translation

After running:

  ```shell
  bash tools/l10n/export-strings.sh
  ```

you should have a branch which is ready to submit as pull request in `l10n-repo`

### Import translated strings

By running:

  ```shell
  bash tools/l10n/import-strings.sh
  ```
you should be able to get the latest localized strings.

1. Verify the changes and then commit and push the updated XML files to the app repository.
