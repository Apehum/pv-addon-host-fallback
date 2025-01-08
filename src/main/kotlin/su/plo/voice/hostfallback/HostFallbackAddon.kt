package su.plo.voice.hostfallback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.plo.slib.api.entity.player.McPlayer
import su.plo.slib.api.event.player.McPlayerQuitEvent
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.addon.injectPlasmoVoice
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.proxy.PlasmoVoiceProxy
import su.plo.voice.api.proxy.event.config.VoiceProxyConfigReloadedEvent
import su.plo.voice.api.proxy.event.connection.TcpPacketSendEvent
import su.plo.voice.api.server.event.connection.UdpClientConnectedEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.hostfallback.store.JsonPlayerHostStore
import su.plo.voice.hostfallback.store.PlayerHostStore
import su.plo.voice.proto.packets.tcp.clientbound.ConnectionPacket
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Addon(
    id = "pv-addon-host-fallback",
    scope = AddonLoaderScope.PROXY,
    version = BuildConstants.VERSION,
    authors = ["Apehum"],
)
class HostFallbackAddon : AddonInitializer {
    private val connectionJobs: MutableMap<McPlayer, Job> = HashMap()
    private val currentHosts: MutableMap<UUID, String> = HashMap()

    private val voiceProxy: PlasmoVoiceProxy by injectPlasmoVoice()
    private val minecraftProxy by lazy { voiceProxy.minecraftServer }

    private val logger = McLoggerFactory.createLogger("pv-addon-host-fallback")

    private lateinit var config: AddonConfig
    private lateinit var playerHostStore: PlayerHostStore

    init {
        McPlayerQuitEvent.registerListener(::onPlayerQuit)
    }

    override fun onAddonInitialize() {
        playerHostStore =
            JsonPlayerHostStore(
                File(getAddonFolder(), "player-hosts.json"),
            )

        reloadConfig()

        minecraftProxy.permissionManager.register(
            "pv.addon.hostfallback.connect",
            PermissionDefault.OP,
        )
    }

    override fun onAddonShutdown() {
        playerHostStore.save(currentHosts)
    }

    @EventSubscribe
    fun onConfigReload(event: VoiceProxyConfigReloadedEvent) {
        reloadConfig()
    }

    @EventSubscribe
    fun onClientConnected(event: UdpClientConnectedEvent) {
        val player = event.connection.player.instance
        currentHosts[player.uuid]?.let { host ->
            logger.info("$player connected to fallback $host")
        }

        connectionJobs[player]?.cancel()
    }

    @EventSubscribe
    fun onConnectionPacket(event: TcpPacketSendEvent) {
        val (voicePlayer, packet) = event
        if (packet !is ConnectionPacket) return

        val player = voicePlayer.instance
        if (!player.hasPermission("pv.addon.hostfallback.connect")) return

        val packetHost = "${packet.ip}:${packet.port}"
        if (packetHost !in config.fallbackHosts) {
            currentHosts[player.uuid]
                ?.let {
                    event.isCancelled = true
                    logger.info("Trying to connect $player to $it")
                    voicePlayer.sendConnectionPacket(packet.secret, it)
                }
        }

        connectionJobs[player]?.cancel()
        connectionJobs[player] =
            CoroutineScope(Dispatchers.Default).launch {
                delay(config.connectionWindowSeconds.seconds)
                if (voicePlayer.hasVoiceChat()) return@launch

                val nextHost = nextHost(currentHosts[player.uuid])
                if (nextHost == null) {
                    logger.warn("Failed to connect player $player to any host")
                    currentHosts.remove(player.uuid)
                    return@launch
                }

                logger.info("Trying to connect $player to $nextHost")

                currentHosts[player.uuid] = nextHost

                voicePlayer.sendConnectionPacket(packet.secret, nextHost)
            }
    }

    private fun onPlayerQuit(player: McPlayer) {
        connectionJobs[player]?.cancel()
    }

    private fun nextHost(current: String?): String? {
        if (current == null) {
            return config.fallbackHosts.firstOrNull()
        }

        val indexOfCurrent = config.fallbackHosts.indexOf(current)
        val next = config.fallbackHosts.getOrNull(indexOfCurrent + 1)

        return next
    }

    private fun reloadConfig() {
        this.config = AddonConfig.loadConfig(getAddonFolder())

        try {
            currentHosts.putAll(
                playerHostStore
                    .load()
                    .filter { (_, host) -> host in config.fallbackHosts },
            )
        } catch (e: Throwable) {
            logger.error("Failed to load players host store", e)
        }
    }

    private fun VoicePlayer.sendConnectionPacket(
        secret: UUID,
        host: String,
    ) {
        val ip = host.substringBefore(":")
        val port = host.substringAfter(":").toInt()

        sendPacket(
            ConnectionPacket(secret, ip, port),
        )
    }

    private fun getAddonFolder(): File = File(minecraftProxy.configsFolder, "pv-addon-host-fallback")
}
