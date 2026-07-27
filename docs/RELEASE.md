# Release flow

Aelion Aero uses [release-please](https://github.com/googleapis/release-please) on `main`. Releases go through a **PR you approve and merge** — nothing is tagged until that PR lands.

```text
feature PR --> main --> Release Please PR (review) --> merge --> tag vX.Y.Z + GitHub Release + JARs
```

## Prerequisites

CI/Release run on the org self-hosted runners (`self-hosted`, `linux`, `aelion`), same as aelion-cloud.

On `main`, enable branch protection:

- Require a pull request before merging
- Require at least one approving review
- Optionally require status checks (`CI`)

That makes the release-please PR the approval gate.

## Day to day

1. Merge work to `main` with [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, …).
2. The **Release** workflow runs release-please.
3. If there are releasable commits since the last tag, it opens or updates a PR titled like `chore(main): release 0.2.0`.
4. Review the PR (changelog + version bumps in `gradle.properties`, `AeroVersion.java`, manifest).
5. **Approve and merge** the release PR.
6. Release-please creates the `vX.Y.Z` tag and GitHub Release.
7. The same workflow builds and uploads:
   - `aero-paper-X.Y.Z.jar`
   - `aero-velocity-X.Y.Z.jar`
   - `aero-bungee-X.Y.Z.jar`

## Manual re-run

**Actions → Release → Run workflow** if you need to refresh the release PR without a new push to `main`.

## First release note

Until the first release PR is merged, the manifest version is `0.1.0`. The first merged release PR creates `v0.1.0` (or whatever version release-please computes from commits).

## Version files (bumped by release-please)

| File | Field |
|------|--------|
| `.github/release-please-manifest.json` | Manifest version |
| `gradle.properties` | `version=` |
| `aero-common/.../AeroVersion.java` | `VERSION` |
| `CHANGELOG.md` | Generated notes |
