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
3. If there are releasable commits since the last tag, it opens or updates a PR titled like `chore(main): release 0.6.1`.
4. Review the PR (changelog + version bumps in `gradle.properties`, `AeroVersion.java`, manifest).
5. **Approve and merge** the release PR.
6. Release-please creates the `vX.Y.Z` tag and GitHub Release.
7. The same workflow builds and uploads:
   - `aero-compat.yml`
   - `aero-bukkit-1_8-X.Y.Z.jar`
   - `aero-bukkit-1_13-X.Y.Z.jar`
   - `aero-paper-1_17-X.Y.Z.jar`
   - `aero-paper-1_21-X.Y.Z.jar`
   - `aero-paper-26-X.Y.Z.jar`
   - `aero-velocity-X.Y.Z.jar`
   - `aero-bungee-X.Y.Z.jar`

### Version bumps (pre-1.0)

Config in `.github/release-please-config.json` uses pre-major softening:

| Commit | Bump (0.x) | Bump (1.0+) |
|--------|------------|-------------|
| `fix:` | patch (`0.6.0` → `0.6.1`) | patch |
| `feat:` | patch (`0.6.0` → `0.6.1`) | minor |
| `BREAKING CHANGE` / `feat!:` | minor (`0.6.0` → `0.7.0`) | major |

That stops at **1.0.0**: after you ship `1.0.0`, `feat:` goes back to minor and breakings go to major. Force a version anytime with a commit footer `Release-As: X.Y.Z`.

## Manual re-run

**Actions → Release → Run workflow** if you need to refresh the release PR without a new push to `main`.

## First release note

Until the first release PR is merged, the manifest version is `0.1.0`. The first merged release PR creates `v0.1.0` (or whatever version release-please computes from commits).

## Version files (bumped by release-please)

| File | Field |
|------|--------|
| `.github/release-please-manifest.json` | Manifest version |
| `gradle.properties` | `version=` (`# x-release-please-version`) |
| `aero-common/.../AeroVersion.java` | `VERSION` (`// x-release-please-version`) |
| `CHANGELOG.md` | Generated notes |

Publish builds with `-Pversion=<tag>` so release JAR names always match the Git tag even if a file bump is missed.
