# Release Notes

Add a new file per version:

```
release-notes/1.2.3.md
```

Content is plain text or Markdown. This is used by `scripts/release.sh` to build the
GitHub release notes and `update.json` for manual update checks.

If you start a draft in `release-notes/new.md`, rename it to the final version
filename (for example `release-notes/1.2.3.md`) before running the release script.
