# Changelog

## [0.6.1](https://github.com/Aelion-Solutions/aelion-aero/compare/v0.6.0...v0.6.1) (2026-07-27)


### Features

* add control API graceful shutdown for daemon stop ([606ccb3](https://github.com/Aelion-Solutions/aelion-aero/commit/606ccb35e7bc57a9dbc3345423fd6aa304a23260))
* add control API graceful shutdown for daemon stop ([6cb9b60](https://github.com/Aelion-Solutions/aelion-aero/commit/6cb9b605e1ed37de4d62f96ddb7f992413da7ff3))
* aero.ae identity split + kick/transfer player utils ([43ea759](https://github.com/Aelion-Solutions/aelion-aero/commit/43ea7591acbda798cf7a53ce40eabc4e476a8a72))
* **control:** add GET /v1/players for online player list ([ec6d4b4](https://github.com/Aelion-Solutions/aelion-aero/commit/ec6d4b4c8c43f4c6babcd7fac419143b1818d21f))
* **control:** add GET /v1/players for online player list ([5adb74f](https://github.com/Aelion-Solutions/aelion-aero/commit/5adb74fd638842b0a6b72f6c1d87460d2bdd8ca7))
* split identity into aero.ae and add kick/transfer utils ([6ceb591](https://github.com/Aelion-Solutions/aelion-aero/commit/6ceb591d400d951e5cc8cc03fa19ebadaafbaabd))


### Bug Fixes

* **bukkit:** fail loudly when required control API cannot bind ([dd429d3](https://github.com/Aelion-Solutions/aelion-aero/commit/dd429d351a97494e731eb8b587fbc6d4a50894f4))
* **bukkit:** reset shutdownRequested when control server restarts ([a6e9386](https://github.com/Aelion-Solutions/aelion-aero/commit/a6e93862445fefb865400252b6c1bd9402c452e3))

## [0.6.0](https://github.com/Aelion-Solutions/aelion-aero/compare/v0.5.0...v0.6.0) (2026-07-27)


### Features

* split proxy/backend cmds and allow insecure panel TLS ([9b2d423](https://github.com/Aelion-Solutions/aelion-aero/commit/9b2d423fb27ea16f464c0e3f020b08b6cb3c469c))
* split proxy/backend commands and allow insecure panel TLS ([dcbc758](https://github.com/Aelion-Solutions/aelion-aero/commit/dcbc7586b20023a561cecf0343d5d724605aac6f))


### Bug Fixes

* drop Brigadier catch-all subcommand arg ([f48f8b0](https://github.com/Aelion-Solutions/aelion-aero/commit/f48f8b0c36e7d716507ff99e9b4804e63883461f))
* restore Brigadier fallback for styled Aero errors ([9364067](https://github.com/Aelion-Solutions/aelion-aero/commit/93640673a54b1330fe7888ec7b20f698f8803dfe))

## [0.5.0](https://github.com/Aelion-Solutions/aelion-aero/compare/v0.4.0...v0.5.0) (2026-07-27)


### Features

* **release:** attach aero-compat.yml to GitHub Releases ([448e425](https://github.com/Aelion-Solutions/aelion-aero/commit/448e425bd783e5cdacbe41cbb971fa55f810f29c))
* **release:** attach aero-compat.yml to GitHub Releases ([1e1346d](https://github.com/Aelion-Solutions/aelion-aero/commit/1e1346d6da799543cf72b8ef3c47f0d5baf68c64))

## [0.4.0](https://github.com/Aelion-Solutions/aelion-aero/compare/v0.3.0...v0.4.0) (2026-07-27)


### Features

* add Paper 26 backend band ([c1a6596](https://github.com/Aelion-Solutions/aelion-aero/commit/c1a6596a85fcec8a6445af10d6478de9925d7b7f))
* add Paper 26 backend band ([551e2ab](https://github.com/Aelion-Solutions/aelion-aero/commit/551e2aba36babe1befb7aba7b0c7660d55363ca0))
* ship multi-version backend bands 1.8-1.21 ([10bb6c8](https://github.com/Aelion-Solutions/aelion-aero/commit/10bb6c85ab816a9326a0b7cdcdbb6800c8e3c82d))
* ship multi-version backend bands 1.8-1.21 ([5f505df](https://github.com/Aelion-Solutions/aelion-aero/commit/5f505dfc92d2210c2cce7c9161702d6efdef45bd))


### Bug Fixes

* **ci:** install JDK 25 for paper-26 builds ([6162013](https://github.com/Aelion-Solutions/aelion-aero/commit/6162013be140a5467f0d2989fa78b29c522b52b2))
* **ci:** install JDK 25 for paper-26 builds ([432710d](https://github.com/Aelion-Solutions/aelion-aero/commit/432710d9c8b47bc2ae1f269fb0decf7b5c4a6b4e))

## [0.3.0](https://github.com/Aelion-Solutions/aelion-aero/compare/v0.2.1...v0.3.0) (2026-07-27)


### Features

* fleet bridge for sibling plugins and /ae command UX ([92c5be1](https://github.com/Aelion-Solutions/aelion-aero/commit/92c5be1de6bd9d67d9412cd78c84ed4e6c669657))
* publish aero-api to GitHub Packages ([b22bdfb](https://github.com/Aelion-Solutions/aelion-aero/commit/b22bdfbb587690959bf3a710708c7e75a010392c))

## [0.2.1](https://github.com/Aelion-Solutions/aelion-aero/compare/v0.2.0...v0.2.1) (2026-07-27)


### Bug Fixes

* **ci:** bump plugin version files and build release JARs from tag ([aa45799](https://github.com/Aelion-Solutions/aelion-aero/commit/aa45799f6789bf6c227acde9ef7cc0a6878bde4d))

## [0.2.0](https://github.com/Aelion-Solutions/aelion-aero/compare/v0.1.0...v0.2.0) (2026-07-27)


### Features

* live proxy registry routing and aero-bungee module ([eb066b8](https://github.com/Aelion-Solutions/aelion-aero/commit/eb066b853036e8203d0496455743121705dcb22e))
* live proxy registry routing and aero-bungee module ([cbf073e](https://github.com/Aelion-Solutions/aelion-aero/commit/cbf073e98125c8c53eb019e45301c6dc9b3ce464))

## [0.1.0](https://github.com/Aelion-Solutions/aelion-aero/compare/v0.1.0...v0.1.0) (2026-07-26)


### Features

* initial Aelion Aero plugins and release-please flow ([f90b8d5](https://github.com/Aelion-Solutions/aelion-aero/commit/f90b8d5fe50e41abe85e8b1ac8adcd81830fd10c))

## [0.1.0](https://github.com/Aelion-Solutions/aelion-aero/compare/aelion-aero-v0.1.0...aelion-aero-v0.1.0) (2026-07-26)


### Features

* initial Aelion Aero plugins and release-please flow ([f90b8d5](https://github.com/Aelion-Solutions/aelion-aero/commit/f90b8d5fe50e41abe85e8b1ac8adcd81830fd10c))
