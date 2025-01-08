# pv-addon-host-fallback
Plasmo Voice proxy addon that attempts to connect players to fallback hosts when the main host fails.

If a player with the `pv.addon.hostfallback.connect` permission fails to connect within the specified window (`connection_window_seconds`),
they will be reconnected to the next fallback host in the list (`fallback_hosts`).
If the player successfully connects to a fallback host, that host will be stored for the player and used for their next connection. 

> [!WARNING]  
> This addon requires [2.1.3+](https://github.com/plasmoapp/plasmo-voice/releases/tag/2.1.3-SNAPSHOT) version of Plasmo Voice.

### Download
Addon is currently only available to download in actions' artifacts: https://github.com/Apehum/pv-addon-host-fallback/actions.

### config.toml
```toml
# If player doesn't connect within the specified "connection_window_seconds",
# they will be connected to the next fallback host.
connection_window_seconds = 5
# fallback_hosts = ["127.0.0.1:25565"]
fallback_hosts = []
```