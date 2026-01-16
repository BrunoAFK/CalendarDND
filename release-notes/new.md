New features
- Added an experimental Event Keywords filter that lets automation react only to events whose titles match keywords or regex.
- New filter screen under Settings > Event Filters with toggle, match mode, input, and a red experimental warning.
- Keyword mode supports comma or line separated terms (case-insensitive) plus examples; regex mode supports Kotlin regex with examples and validation.

Bugfixes
- Added a Debug Tools "Force update" action to test the installer flow.
- Update flow now checks "install unknown apps" permission and surfaces installer errors.
- Update installs now use the package installer intent, with extra logging for debug.
