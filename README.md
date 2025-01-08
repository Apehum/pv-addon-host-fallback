# pv-addon-host-fallback
Plasmo Voice proxy addon that attempts to connect players to fallback hosts when the main host fails.

If a player with the `pv.addon.hostfallback.connect` permission fails to connect within the specified window (`connection_window_seconds`),
they will be reconnected to the next fallback host in the list (`fallback_hosts`).

> [!WARNING]  
> This addon requires [2.1.3+](https://github.com/plasmoapp/plasmo-voice/releases/tag/2.1.3-SNAPSHOT) version of Plasmo Voice.

### config.toml
```toml
# If player doesn't connect within the specified "connection_window_seconds",
# they will be connected to the next fallback host.
connection_window_seconds = 5
# fallback_hosts = ["127.0.0.1:25565"]
fallback_hosts = []
```