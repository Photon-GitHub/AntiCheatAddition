# Contributing to AntiCheatAddition

AntiCheatAddition is maintained as a production-oriented anti-cheat project. Contributions of any size are welcome, from focused corrections to substantial new checks, integrations, and architectural improvements. The scope of a contribution is not an obstacle, provided that the change is coherent, maintainable, and exhaustively tested.

This guide is intended for experienced Java, Spigot, and packet-processing developers. The repository is not a substitute for the supported plugin distribution. Please do not request source-build assistance or release binaries here; use the [official Spigot resource](https://www.spigotmc.org/resources/anticheataddition.33590/) for matters concerning the distributed plugin.

## Before You Start

Read the relevant module, its configuration section in [`config.yml`](src/main/resources/config.yml), and the surrounding infrastructure before proposing a change. A check is more than its detection condition: it may have module-loader requirements, bypass permissions, violation accumulation, threshold actions, packet listeners, and asynchronous processing to preserve.

Larger contributions should establish a clear design boundary. Separate distinct concerns into reviewable commits and explain any new abstractions, data flows, or compatibility assumptions. Changes may span multiple subsystems when the design requires it; artificial size limits are less useful than a structure that reviewers can follow and validate.

## Engineering Expectations

Changes must respect the following project constraints:

- **False positives require particular attention.** Detection changes need a clear behavioural rationale, tolerances appropriate to networked gameplay, and extensive validation of legitimate client and server behaviour.
- **Configuration is part of the change.** Preserve existing paths, defaults, comments, and threshold semantics unless a deliberate migration is included. Module identifiers are consumed by configuration, permissions, events, and integrations.
- **Threading is explicit.** Packet listeners, batch processors, and Bukkit events may execute asynchronously. Never introduce Bukkit access from an unsafe context, and preserve the concurrency assumptions of shared user data.
- **Version support is intentional.** Do not infer compatibility from a single successful test. Changes to protocol, metadata, inventory, or packet handling must account for the compatibility constraints in `ServerVersion` and PacketEvents.
- **Public APIs require restraint.** Treat `api` and `events` as integration surfaces. Avoid incompatible changes; when one is necessary, document its impact and migration expectations in the pull request.

Follow the existing style in the code you touch: use the established package layout, explicit `final` parameters and locals where customary, braces, and Javadoc for public or non-obvious contracts. Prefer clear domain names over abbreviations. Do not suppress warnings or add broad exception handling merely to make a change compile.

## Testing and Validation

Every contribution must be tested to a depth appropriate to its full behavioural impact.
For detection logic and other runtime-sensitive changes, this means both automated verification and extensive observation under ordinary gameplay.
Passing a narrow reproduction case is not sufficient evidence that a change is safe.

### Unit Tests

Add unit tests wherever the affected behaviour can be isolated.
New calculations, data structures, and regression fixes should have deterministic coverage in `src/test` where possible.
Include boundary conditions and invalid or unexpected input, not only the expected path.

Refactor tightly coupled logic when reasonable to make meaningful unit testing possible.
Do not, however, replace integration-sensitive validation with mocks that cannot represent actual Bukkit, PacketEvents, or protocol behaviour.

### Vanilla Client Testing

Changes that can affect detection, violation levels, packet handling, cancellation, or player-visible behaviour must be tested extensively for false positives with an unmodified vanilla Minecraft client.
Other code changes should, at a minimum, be tested on a current server with a player joining successfully to expose potential runtime errors.

Exercise the complete range of legitimate behaviour relevant to the change, including rapid, repeated, interrupted, and unusual interactions rather than testing only a short ordinary session.

Where relevant, vary the conditions under which the behaviour is tested:

- player latency, jitter, and temporary packet delay, where practical;
- server load and tick-rate degradation;
- supported server and client protocol combinations;
- world changes, teleportation, death, respawn, and reconnects;
- simultaneous or rapidly alternating actions; and
- interactions with relevant optional dependencies and server mechanics.

Also attempt to trigger the detection deliberately with abnormal but legitimate behaviour on a vanilla client.
Even when a legitimate player is deliberately trying to trigger a detection, reaching any serious VL action—the second staff warning or a kick under the default configuration—should require at least one minute of sustained effort.

False-positive testing should run long enough to expose accumulated state, statistical drift, timing boundaries, and infrequent event ordering. A clean result from a few repetitions is not exhaustive validation. Record any flags, violation-level changes, cancellations, console errors, and player-visible anomalies encountered during the session.

Positive detection testing remains necessary where applicable, but it does not replace vanilla-client testing. A check that detects its intended target while also penalising legitimate players is not ready for inclusion.

### General Validation

At a minimum, verify that your change:

- loads only when its module requirements are satisfied;
- honours enabled settings, bypass permissions, and configured thresholds;
- cleans up retained state and registered resources on disconnect and disable paths;
- behaves correctly at timing, numerical, and protocol boundaries; and
- does not regress unaffected modules or supported environments.

Documentation-only changes and internal refactors do not require irrelevant gameplay testing, but they must still receive all applicable automated and functional validation. If part of the affected matrix cannot be tested, state that limitation explicitly; maintainers may require further evidence before accepting the contribution.

## Pull Requests

The pull-request description must make the validation reproducible. Include:

1. the problem being solved and the rationale for the chosen design;
2. the configuration, API, protocol, and compatibility contracts affected;
3. the automated tests added or updated;
4. the server, client, PacketEvents, and integration versions tested;
5. the vanilla-client scenarios exercised for false positives, including their duration and test conditions; and
6. any part of the supported matrix that remains untested.

Keep commits reviewable and avoid generated artifacts, local server files, IDE settings, or unrelated changes. Substantial pull requests are welcome, but reviewers must be able to distinguish structural work from behavioural changes and assess the evidence for each. Maintainers may request additional tests, longer false-positive sessions, configuration documentation, or validation on further environments before a change is accepted.

## License

By contributing, you agree that your contribution is provided under the [GNU General Public License v3.0](LICENSE), consistent with the repository.
