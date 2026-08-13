<p align="center">
  <img width="778" height="270" alt="aelion-cloud-aero" src="https://github.com/user-attachments/assets/ca83bb24-7dbb-483b-9eb5-1d88cbcdf016" />
</p>

# Aelion Aero

Paper/Spigot, Velocity, and BungeeCord/Waterfall (legacy) plugins that connect Aelion Servers to
[Aelion Cloud](https://github.com/Aelion-Solutions/aelion-cloud). 
In normal cases you dont have to download these manually. This repo is for transparency.

## Modules

| Module | Artifact | Platform |
|--------|----------|----------|
| `aero-api` | (Maven) | Fleet bridge API for sibling plugins |
| `aero-common` | (library) | Config, control DTOs, panel client, commands |
| `aero-bukkit-shared` | (library) | Shared Bukkit fleet + classic `/aes` |
| `aero-bukkit-1_8` | `aero-bukkit-1_8-<version>.jar` | Spigot/Paper **1.8–1.12.2** |
| `aero-bukkit-1_13` | `aero-bukkit-1_13-<version>.jar` | Spigot/Paper **1.13–1.16.5** |
| `aero-paper-1_17` | `aero-paper-1_17-<version>.jar` | Paper **1.17–1.20.x** |
| `aero-paper-1_21` | `aero-paper-1_21-<version>.jar` | Paper **1.21.x** |
| `aero-paper-26` | `aero-paper-26-<version>.jar` | Paper **26.x** |
| `aero-velocity` | `aero-velocity-<version>.jar` | Velocity **3.4.x** (main proxy support) |
| `aero-bungee` | `aero-bungee-<version>.jar` | BungeeCord / Waterfall (legacy support) |

Compatibility matrix: [`compat/aero-compat.yml`](compat/aero-compat.yml).

## Build
Building is not documented, so this repo is for viewing the code to make sure its safe for you.
Releases build artifacts and packages automatically, if you want to build it yourself for whatever reason you should understand how.

## aero-api 

> [!WARNING]
> For now *ANY* plugin can interact with the aero-api (there is no whitelist yet) meaning those can also control the cloud, so keep that in mind when adding unvetted plugins!


aero-api is a small library/api interface to communicate with the cloud service. 
It holds the connection and decides who gets priority and what is allowed. 

You can find more about is [here](https://github.com/Aelion-Solutions/aelion-aero/wiki/API).




## Commands

| Command | Permission | Notes |
|---------|------------|--------|
| `/ae` / `/aec` | `aelion.aero.info` (or admin/create) | Proxy primary + alias (Velocity/Bungee); defaults **op** |
| `/aes` | `aelion.aero.info` (or admin/create) | Backend primary (Bukkit/Paper); defaults **op** |
| `/ae help` or `/aes help` | info | Usage (filtered to verbs you can run) |
| `/ae info` or `/aes info` | info | Version, server-id, panel host (no tokens) |
| `/ae reload` or `/aes reload` | `aelion.aero.admin` | Reload config (+ restart control API on Velocity) |
| `/ae ping` or `/aes ping` | info | Hit panel health/info |
| `/ae servers list [--names]` or `/aes …` | info | Panel fleet for same owner |
| `/ae notify [on\|off]` | info | Proxy only — toggle fleet change chat (session; daemon push) |
| `/ae kick <player> [message…]` or `/aes …` | `aelion.aero.admin` | Kick online player |
| `/ae transfer <player> server=\|group=` or `/aes …` | `aelion.aero.admin` | Transfer via fleet / proxy switch |
| `/ae backends` | info | Live proxy backends (Velocity/Bungee only) |
| `/ae create-server …` | `aelion.aero.create` | Proxy only; `template=` XOR `software=`+`version=` |

`admin` and `create` each imply `info` (YAML children + code). They do not imply each other.

## Cloud Installation - How it works

Aelion Cloud AEpi / Panel connects to this repo via HTTP/S and fetches the latest released compat yaml file to detewrmine what to download.
Based on server requirements it caches and installs the plugin(s) automatically.
