# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning: SemVer.

## [1.0.0] - 2026-11-25

### Added

- Fluid overlay for Xaero's World Map:
  - Pipe runs drawn from `FluidPipeBlock.isOpenAt`, so casings, brackets and tees are
    accurate.
  - Markers for mechanical pumps, fluid tanks, spouts and isolated pipes.
  - On-map **Fluid** toggle with a live pump counter.
- Server-side index of pipe/pump/tank/spout block entities (chunk load/unload, block
  change rescans, self-healing sweep).
- Demand-driven sync with chunked topology packets.
- Client and server config, `/fluidroutes stats` and `/fluidroutes rescan`.
- English and Spanish translations.
