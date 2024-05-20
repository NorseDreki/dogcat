# Releasing

This procedure creates a new release on GitHub Releases and attaches release notes and source code to it.
Project binaries built for Linux become available for installation via Snapcraft Store.
Binaries built for MacOS become available for installation via Homebrew package manager.

To make this happen, please follow these steps:

1. Update the `VERSION_NAME` in `gradle.properties` to the release version, according
   to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

2. Update the [Changelog](CHANGELOG.md):
    1. Change the `Unreleased` header to the release version.
    2. Add a link URL to ensure the header link works.
    3. Add a new `Unreleased` section to the top.

3. Commit release version information:

   ```
   $ git commit -am "Release version X.Y.X"
   ```

4. Tag this commit with release version and _sign_ it:

   ```
   $ git tag -s -am "Version X.Y.Z" X.Y.Z
   ```

5. Push this commit along with its tag

   ```
   $ git push && git push --tags
   ```

   This will trigger a GitHub Action workflow which will create a GitHub release with the release notes, release Linux
   binaries to Snapcraft Store and send a PR to the Homebrew tap repo.

6. Find [the Homebrew PR](https://github.com/NorseDreki/homebrew-tap/pulls) and mark it with the `pr-pull` label.

   This will trigger the project's Homebrew formula validation. If successful, the PR will be auto-merged, MacOS
   binaries
   will be built and corresponding Homebrew bottles made available for installation.

## Checklist for Release Success

1. The PR to the Homebrew tap repo is auto-merged and a new GitHub release is created in that repo.
2. A new release is created in project's repo, with release notes and attached files.

This should guarantee project binaries are released both to Snapcraft Store and Homebrew and are ready for installation
by a user. Also, that release versions and notes are tracked on GitHub.

