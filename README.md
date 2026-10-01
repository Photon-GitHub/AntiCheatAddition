# AntiCheatAddition

AntiCheatAddition is a Spigot anti-cheat extension focused on cheat classes and player-information leaks that are not consistently addressed by general-purpose anti-cheat solutions.
It provides configurable detection modules, Sentinel modules, packet-level information hiding, and operational tooling.

This repository is a development resource for experienced Java and Minecraft server developers.
For supported releases, administration guidance, and customer support, use the [official Spigot resource](https://www.spigotmc.org/resources/anticheataddition.33590/).

## Scope

- **Detection modules** cover behaviour including automated inventory interaction, fishing, potion use, tool switching, scaffold behaviour, targeting, and packet anomalies.
- **Sentinel modules** address selected exploit patterns and client or modification identification.
- **Information-protection additions** provide entity-visibility, item-data, damage-indicator, and server-brand controls.
- **Violation management** supplies configurable thresholds, cancellation behaviour, logging, and Bukkit events for integrations.

Checks are deliberately modular.
A module may be unavailable because of a server-version restriction, a missing optional integration, or an explicit configuration setting.

## Runtime Contracts

AntiCheatAddition is a Spigot plugin and requires [PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/).
It also integrates with ViaVersion, Floodgate, and WorldGuard.

Supported server versions are defined in [`ServerVersion`](src/main/java/de/photon/anticheataddition/ServerVersion.java).
The plugin refuses to load on unsupported server versions, and forcing such a load likely leads to runtime errors.

## Project Structure

| Location | Purpose |
| --- | --- |
| [`src/main/java/de/photon/anticheataddition`](src/main/java/de/photon/anticheataddition) | Plugin bootstrap, public API, modules, events, user state, and shared utilities. |
| [`modules`](src/main/java/de/photon/anticheataddition/modules) | Module definitions, loading rules, detection checks, additions, and Sentinel functionality. |
| [`api`](src/main/java/de/photon/anticheataddition/api) | Public integration surface for querying and controlling modules and violation levels. |
| [`events`](src/main/java/de/photon/anticheataddition/events) | Bukkit events emitted when module activity produces a violation. |
| [`src/main/resources/config.yml`](src/main/resources/config.yml) | Default module configuration, thresholds, and operational settings. |
| [`src/test`](src/test) | Unit tests for logic that can be isolated from a live server. |

## Architecture Notes

`ModuleManager` owns the built-in module registry.
Each `Module` has a stable configuration path and is activated through a `ModuleLoader`, which applies configuration, platform, dependency, compatibility, listener, packet-listener, and message-channel requirements.
`ViolationModule` extends that model with violation-level management.

The public `AntiCheatAdditionApi` permits integrations to inspect and adjust violation levels, inspect or change module state, and register external modules.
Some extension points necessarily expose implementation-sensitive behaviour; callers must treat them as version-coupled and validate their integration against the target plugin release.

Packet processing and some module work can occur away from the Bukkit primary thread.
Treat Bukkit APIs and mutable player state according to their thread-safety guarantees.

## Configuration and Operations

The distributed [`config.yml`](src/main/resources/config.yml) is the canonical reference for module names, settings, defaults, and threshold semantics.
Preserve its hierarchy and module identifiers when changing code: identifiers feed configuration lookups, bypass permissions, violation handling, and integrations.

## Contributing

Contributions are welcome when they are well-reasoned and exhaustively tested to the extent the change permits.
Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or pull request.

## Versioning

Release versions follow [Semantic Versioning](https://semver.org/).

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

## Credits

- Photon — initial work
- Fabian Faßbender ([geNAZt](https://github.com/geNAZt)) — contributions across the project
- Janmm14 — assistance with individual issues
- konsolas — foundational anti-cheat work

See the [contributors](https://github.com/Photon-GitHub/AACAdditionPro/contributors) for the complete contribution history.
