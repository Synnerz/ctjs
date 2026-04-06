package com.chattriggers.ctjs.api.client

import com.chattriggers.ctjs.api.triggers.RegularTrigger
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.internal.BoundKeyUpdater
import com.chattriggers.ctjs.internal.mixins.GameOptionsAccessor
import com.chattriggers.ctjs.internal.mixins.KeyBindingAccessor
import com.chattriggers.ctjs.internal.utils.Initializer
import com.chattriggers.ctjs.internal.utils.asMixin
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import org.apache.commons.lang3.ArrayUtils
import java.util.concurrent.CopyOnWriteArrayList

class KeyBind {
    private val keyBinding: KeyMapping
    private var onKeyPress: RegularTrigger? = null
    private var onKeyRelease: RegularTrigger? = null
    private var onKeyDown: RegularTrigger? = null

    private var down: Boolean = false

    /**
     * Creates a new keybind, editable in the user's controls.
     *
     * @param description what the keybind does
     * @param keyCode the keycode which the keybind will respond to, see Keyboard below. Ex. Keyboard.KEY_A
     * @param category the keybind category the keybind will be in
     * @see [org.lwjgl.input.Keyboard](http://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html)
     */
    @JvmOverloads
    constructor(description: String, keyCode: Int, category: String = "ChatTriggers") {
        val possibleDuplicate = Client.getMinecraft().options.keyMappings.find {
            I18n.get(it.translatedKeyMessage.string) == I18n.get(description) &&
                // TODO: check if this is right
                I18n.get(it.category.id.toLanguageKey("key.category")) == I18n.get(category)
        }

        if (possibleDuplicate != null) {
            require(possibleDuplicate in customKeyBindings) {
                "KeyBind already exists! To get a KeyBind from an existing Minecraft KeyBinding, " +
                    "use the other KeyBind constructor or Client.getKeyBindFromKey."
            }
            keyBinding = possibleDuplicate
        } else {
            val categoryList = KeyBindingAccessor.Category.getCategoryList()

            if (!categoryList.stream().anyMatch { it.id.path.equals(category) }) {
                uniqueCategories[category] = 0
            }
            val keyCategory = KeyMapping.Category.register(Identifier.parse(category))
            uniqueCategories[category] = uniqueCategories[category]!! + 1
            keyBinding = KeyMapping(description, keyCode, keyCategory)

            // We need to update the bound key for the KeyBind we just made to the previous binding,
            // just in case it existed last time the game was opened. This will only matter for the first
            // time launching the game, as subsequent CT loads will cause possibleDuplicate to be found.
            Client.getMinecraft().options.asMixin<BoundKeyUpdater>().ctjs_updateBoundKey(keyBinding)
//            KeyMapping.updateKeysByCode()

            addKeyBinding(keyBinding)
            customKeyBindings.add(keyBinding)
        }

        keyBinds.add(this)
    }

    constructor(keyBinding: KeyMapping) {
        this.keyBinding = keyBinding
        keyBinds.add(this)
    }

    fun registerKeyPress(method: Any) = apply {
        onKeyPress = RegularTrigger(method, TriggerType.OTHER)
    }

    fun registerKeyRelease(method: Any) = apply {
        onKeyRelease = RegularTrigger(method, TriggerType.OTHER)
    }

    fun registerKeyDown(method: Any) = apply {
        onKeyDown = RegularTrigger(method, TriggerType.OTHER)
    }

    fun unregisterKeyPress() = apply {
        onKeyPress?.unregister()
        onKeyPress = null
    }

    fun unregisterKeyRelease() = apply {
        onKeyRelease?.unregister()
        onKeyRelease = null
    }

    fun unregisterKeyDown() = apply {
        onKeyDown?.unregister()
        onKeyDown = null
    }

    internal fun onTick() {
        if (isPressed() && !down) {
            if (keyBinding in customKeyBindings) {
                while (keyBinding.isDown()) {
                    // consume the key press if not built-in keybinding
                }
            }

            onKeyPress?.trigger(arrayOf())
            down = true
        }

        if (isKeyDown()) {
            onKeyDown?.trigger(arrayOf())
            down = true
        }

        if (down && !isKeyDown()) {
            while (keyBinding.isDown()) {
                // consume the rest of the key presses
            }

            onKeyRelease?.trigger(arrayOf())
            down = false
        }
    }

    /**
     * Returns true if the key is pressed (used for continuous querying).
     *
     * @return whether the key is pressed
     */
    fun isKeyDown(): Boolean = keyBinding.isDown

    /**
     * Returns true on the initial key press. For continuous querying use [isKeyDown].
     *
     * @return whether the key has just been pressed
     */
    fun isPressed(): Boolean = keyBinding.asMixin<KeyBindingAccessor>().clickCount > 0

    /**
     * Gets the description of the key.
     *
     * @return the description
     */
    fun getDescription(): String = keyBinding.translatedKeyMessage.string

    /**
     * Gets the key code of the key.
     *
     * @return the integer key code
     */
    fun getKeyCode(): Int = keyBinding.asMixin<KeyBindingAccessor>().key.value

    /**
     * Gets the category of the key.
     *
     * @return the category
     */
    fun getCategory(): String = keyBinding.category.id.toLanguageKey("key.category")

    /**
     * Sets the state of the key.
     *
     * @param pressed True to press, False to release
     */
    fun setState(pressed: Boolean) =
        KeyMapping.set(keyBinding.asMixin<KeyBindingAccessor>().key, pressed)

    override fun toString() = "KeyBind{" +
        "description=${getDescription()}, " +
        "keyCode=${getKeyCode()}, " +
        "category=${getCategory()}" +
        "}"

    companion object : Initializer {
        private val customKeyBindings = mutableSetOf<KeyMapping>()
        private val uniqueCategories = mutableMapOf<String, Int>()
        private val keyBinds = CopyOnWriteArrayList<KeyBind>()

        internal fun getKeyBinds() = keyBinds

        override fun init() {
            ClientTickEvents.START_CLIENT_TICK.register {
                if (!World.isLoaded())
                    return@register

                keyBinds.forEach {
                    // This used to cause crashes on legacy sometimes. If it starts crashing again,
                    // we'll add the empty try-catch block back
                    it.onTick()
                }
            }
        }

        internal fun clearKeyBinds() {
            keyBinds.toList().forEach(::removeKeyBind)
            customKeyBindings.clear()
            keyBinds.clear()
        }

        internal fun getCategoryName(category: KeyMapping.Category): String {
            return category.id.path
        }

        private fun removeKeyBinding(keyBinding: KeyMapping) {
            Client.getMinecraft().options.asMixin<GameOptionsAccessor>().setKeyMappings(
                ArrayUtils.removeElement(
                    Client.getMinecraft().options.keyMappings,
                    keyBinding
                )
            )
            val category = keyBinding.category
            val categoryName = getCategoryName(category)

            if (categoryName in uniqueCategories) {
                uniqueCategories[categoryName] = uniqueCategories[categoryName]!! - 1

                if (uniqueCategories[categoryName] == 0) {
                    uniqueCategories.remove(categoryName)
                    KeyBindingAccessor.Category.getCategoryList().removeIf { it.id.equals(category.id) }
                }
            }
        }

        private fun removeKeyBind(keyBind: KeyBind) {
            val keyBinding = keyBind.keyBinding
            if (keyBinding !in customKeyBindings) return

            removeKeyBinding(keyBinding)
            customKeyBindings.remove(keyBinding)
            keyBinds.remove(keyBind)
        }

        private fun addKeyBinding(keyBinding: KeyMapping): KeyMapping {
            Client.getMinecraft().options.asMixin<GameOptionsAccessor>().setKeyMappings(
                ArrayUtils.add(
                    Client.getMinecraft().options.keyMappings,
                    keyBinding
                )
            )

            KeyBindingAccessor.Category.getCategoryList().add(keyBinding.category)

            return keyBinding
        }
    }
}
