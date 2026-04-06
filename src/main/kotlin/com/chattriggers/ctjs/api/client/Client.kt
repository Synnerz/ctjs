package com.chattriggers.ctjs.api.client

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.inventory.Slot
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.internal.listeners.ClientListener
import com.chattriggers.ctjs.internal.mixins.AbstractContainerScreenAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import com.mojang.realmsclient.RealmsMainScreen
import gg.essential.universal.UKeyboard
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.PlayerTabOverlay
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import kotlin.math.max
import kotlin.math.roundToInt

object Client {
    internal var referenceSystemTime: Long = 0

    @JvmField
    val currentGui = CurrentGuiWrapper()

    @JvmField
    val camera = CameraWrapper()

    /**
     * Gets Minecraft's Minecraft object
     *
     * @return The Minecraft object
     */
    @JvmStatic
    fun getMinecraft(): Minecraft = Minecraft.getInstance()

    /**
     * Gets Minecraft's NetHandlerPlayClient object
     *
     * @return The NetHandlerPlayClient object
     */
    @JvmStatic
    fun getConnection(): ClientPacketListener? = getMinecraft().connection

    /**
     * Schedule's a task to run on Minecraft's main thread in [delay] ticks.
     * Defaults to the next tick.
     * @param delay The delay in ticks
     * @param callback The task to run on the main thread
     */
    @JvmStatic
    @JvmOverloads
    fun scheduleTask(delay: Int = 0, callback: () -> Unit) {
        ClientListener.addTask(delay, callback)
    }

    /**
     * Quits the client back to the main menu.
     * This acts just like clicking the "Disconnect" or "Save and quit to title" button.
     */
    @JvmStatic
    fun disconnect() {
        scheduleTask {
            World.toMC()?.disconnect(Component.empty())

            getMinecraft().setScreen(
                when {
                    getMinecraft().isSingleplayer -> TitleScreen()
                    getMinecraft().currentServer?.isRealm == true -> RealmsMainScreen(TitleScreen())
                    else -> JoinMultiplayerScreen(TitleScreen())
                }
            )
        }
    }

    /**
     * Connects to the server with the given ip.
     * @param ip The ip to connect to
     */
    @JvmStatic
    @JvmOverloads
    fun connect(ip: String, port: Int = 25565) {
        scheduleTask {
            ConnectScreen.startConnecting(
                JoinMultiplayerScreen(TitleScreen()),
                getMinecraft(),
                ServerAddress(ip, port),
                ServerData("Server", ip, ServerData.Type.OTHER),
                false,
                null,
            )
        }
    }

    /**
     * Gets the Minecraft ChatHud object for the chat gui
     *
     * @return The GuiNewChat object for the chat gui
     */
    @JvmStatic
    fun getChatGui(): ChatComponent = getMinecraft().gui.chat

    @JvmStatic
    fun isInChat(): Boolean = getMinecraft().screen is ChatScreen

    @JvmStatic
    fun getTabGui(): PlayerTabOverlay = getMinecraft().gui.tabList

    @JvmStatic
    fun isInTab(): Boolean = getMinecraft().options.keyPlayerList.isDown

    /**
     * Gets whether the Minecraft window is active
     * and in the foreground of the user's screen.
     *
     * @return true if the game is active, false otherwise
     */
    @JvmStatic
    fun isTabbedIn(): Boolean = getMinecraft().isWindowActive

    @JvmStatic
    fun isControlDown(): Boolean = UKeyboard.isCtrlKeyDown()

    @JvmStatic
    fun isShiftDown(): Boolean = UKeyboard.isShiftKeyDown()

    @JvmStatic
    fun isAltDown(): Boolean = UKeyboard.isAltKeyDown()

    @JvmStatic
    fun getFPS(): Int = getMinecraft().fps

    @JvmStatic
    fun getVersion(): String = getMinecraft().versionType

    @JvmStatic
    fun getMaxMemory(): Long = Runtime.getRuntime().maxMemory()

    @JvmStatic
    fun getTotalMemory(): Long = Runtime.getRuntime().totalMemory()

    @JvmStatic
    fun getFreeMemory(): Long = Runtime.getRuntime().freeMemory()

    @JvmStatic
    fun getMemoryUsage(): Int = ((getTotalMemory() - getFreeMemory()) * 100 / getMaxMemory().toFloat()).roundToInt()

    @JvmStatic
    fun getSystemTime(): Long = (System.nanoTime() - referenceSystemTime) / 1_000_000

    @JvmStatic
    fun getMouseX() = getMinecraft().mouseHandler.xpos() * getMinecraft().window.guiScaledWidth / max(1, getMinecraft().window.guiScaledWidth)

    @JvmStatic
    fun getMouseY() = getMinecraft().mouseHandler.ypos() * getMinecraft().window.guiScaledHeight / max(1, getMinecraft().window.guiScaledHeight)

    @JvmStatic
    fun isInGui(): Boolean = currentGui.get() != null

    @JvmStatic
    fun sendPacket(packet: Packet<*>) {
        getConnection()?.connection?.send(packet)
    }

    /**
     * Display a title.
     *
     * @param title title text
     * @param subtitle subtitle text
     * @param fadeIn time to fade in
     * @param time time to stay on screen
     * @param fadeOut time to fade out
     */
    @JvmStatic
    fun showTitle(title: String?, subtitle: String?, fadeIn: Int, time: Int, fadeOut: Int) {
        getMinecraft().gui.apply {
            setTimes(fadeIn, time, fadeOut)
            if (title != null)
                setTitle(TextComponent(title))
            if (subtitle != null)
                setSubtitle(TextComponent(subtitle))
        }
    }

    /**
     * Copies a string to the clipboard
     *
     * @param text The text to copy
     */
    @JvmStatic
    @JvmOverloads
    fun copy(text: String = "") {
        getMinecraft().keyboardHandler.clipboard = text
    }

    /**
     * Get the string currently on the clipboard
     */
    @JvmStatic
    fun paste(): String = getMinecraft().keyboardHandler.clipboard

    class CurrentGuiWrapper {
        /**
         * Gets the Java class name of the currently open gui, for example, "GuiChest"
         *
         * @return the class name of the current gui
         */
        fun getClassName(): String = get()?.javaClass?.simpleName ?: "null"

        /**
         * Gets the Minecraft gui class that is currently open
         *
         * @return the Minecraft gui
         */
        fun get(): Screen? = getMinecraft().screen

        fun set(screen: Screen?) {
            scheduleTask {
                getMinecraft().setScreen(screen)
            }
        }

        /**
         * Gets the slot under the mouse in the current gui, if one exists.
         *
         * @return the [Slot] under the mouse
         */
        fun getSlotUnderMouse(): Slot? {
            // TODO: fixme
            val screen: Screen? = get()
            return if (screen is AbstractContainerScreen<*>) {
                screen.asMixin<AbstractContainerScreenAccessor>().invokeGetHoveredSlot(getMouseX(), getMouseY())?.let(::Slot)
            } else null
        }

        /**
         * Closes the currently open gui
         */
        fun close() {
            scheduleTask { CTJS.minecraft.player?.closeContainer() }
        }
    }

    class CameraWrapper {
        fun getX(): Double = getMinecraft().gameRenderer.mainCamera.position().x

        fun getY(): Double = getMinecraft().gameRenderer.mainCamera.position().y

        fun getZ(): Double = getMinecraft().gameRenderer.mainCamera.position().z
    }
}
