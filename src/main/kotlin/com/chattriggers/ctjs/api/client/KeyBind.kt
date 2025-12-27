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
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.resource.language.I18n
import net.minecraft.util.Identifier
import org.apache.commons.lang3.ArrayUtils
import java.util.concurrent.CopyOnWriteArrayList

class KeyBind {
    private val keyBinding: KeyBinding
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
        val possibleDuplicate = Client.getMinecraft().options.allKeys.find {
            I18n.translate(it.boundKeyTranslationKey) == I18n.translate(description) &&
                // TODO: check if this is right
                I18n.translate(it.category.id.toTranslationKey("key.category")) == I18n.translate(category)
        }

        if (possibleDuplicate != null) {
            require(possibleDuplicate in customKeyBindings) {
                "KeyBind already exists! To get a KeyBind from an existing Minecraft KeyBinding, " +
                    "use the other KeyBind constructor or Client.getKeyBindFromKey."
            }
            keyBinding = possibleDuplicate
        } else {
            val categoryList = KeyBindingAccessor.Category.getCategoryList()

            val keyCategory = findExistingCategory(categoryList, category)
                ?: createCategory(category).also { newCategory ->
                    categoryList.add(newCategory)
                }

            uniqueCategories[category] = (uniqueCategories[category] ?: 0) + 1

            keyBinding = KeyBinding(description, keyCode, keyCategory)

            // We need to update the bound key for the KeyBind we just made to the previous binding,
            // just in case it existed last time the game was opened. This will only matter for the first
            // time launching the game, as subsequent CT loads will cause possibleDuplicate to be found.
            Client.getMinecraft().options.asMixin<BoundKeyUpdater>().ctjs_updateBoundKey(keyBinding)
            KeyBinding.updateKeysByCode()

            addKeyBinding(keyBinding)
            customKeyBindings.add(keyBinding)
        }

        keyBinds.add(this)
    }

    constructor(keyBinding: KeyBinding) {
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
                while (keyBinding.wasPressed()) {
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
            while (keyBinding.wasPressed()) {
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
    fun isKeyDown(): Boolean = keyBinding.isPressed

    /**
     * Returns true on the initial key press. For continuous querying use [isKeyDown].
     *
     * @return whether the key has just been pressed
     */
    fun isPressed(): Boolean = keyBinding.asMixin<KeyBindingAccessor>().timesPressed > 0

    /**
     * Gets the description of the key.
     *
     * @return the description
     */
    fun getDescription(): String = keyBinding.boundKeyTranslationKey

    /**
     * Gets the key code of the key.
     *
     * @return the integer key code
     */
    fun getKeyCode(): Int = keyBinding.asMixin<KeyBindingAccessor>().boundKey.code

    /**
     * Gets the category of the key.
     *
     * @return the category
     */
    fun getCategory(): String = keyBinding.category.id.toTranslationKey("key.category")

    /**
     * Sets the state of the key.
     *
     * @param pressed True to press, False to release
     */
    fun setState(pressed: Boolean) =
        KeyBinding.setKeyPressed(keyBinding.asMixin<KeyBindingAccessor>().boundKey, pressed)

    override fun toString() = "KeyBind{" +
        "description=${getDescription()}, " +
        "keyCode=${getKeyCode()}, " +
        "category=${getCategory()}" +
        "}"

    companion object : Initializer {
        private val customKeyBindings = mutableSetOf<KeyBinding>()
        private val uniqueCategories = mutableMapOf<String, Int>()
        private val keyBinds = CopyOnWriteArrayList<KeyBind>()

        private val categoryCache = mutableMapOf<String, KeyBinding.Category>()

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

        internal fun getCategoryName(category: KeyBinding.Category): String {
            return category.id.path
        }

        private fun findExistingCategory(
            categoryList: List<KeyBinding.Category>,
            categoryName: String
        ): KeyBinding.Category? {
            categoryCache[categoryName]?.let { cached ->
                if (categoryList.any { it.id == cached.id }) {
                    return cached
                }
            }

            return categoryList.find {
                it.id.path.equals(categoryName, ignoreCase = true) ||
                        it.id.toString().equals(categoryName, ignoreCase = true)
            }?.also {
                categoryCache[categoryName] = it
            }
        }

        private fun createCategory(categoryName: String): KeyBinding.Category {
            val identifier = Identifier.of("chattriggers", categoryName.lowercase().replace(" ", "_"))
            val category = KeyBinding.Category.create(identifier)
            categoryCache[categoryName] = category
            return category
        }

        private fun removeKeyBinding(keyBinding: KeyBinding) {
            Client.getMinecraft().options.asMixin<GameOptionsAccessor>().setAllKeys(
                ArrayUtils.removeElement(
                    Client.getMinecraft().options.allKeys,
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
                    categoryCache.remove(categoryName)
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

        private fun addKeyBinding(keyBinding: KeyBinding): KeyBinding {
            Client.getMinecraft().options.asMixin<GameOptionsAccessor>().setAllKeys(
                ArrayUtils.add(
                    Client.getMinecraft().options.allKeys,
                    keyBinding
                )
            )

            val categoryList = KeyBindingAccessor.Category.getCategoryList()
            if (!categoryList.any { it.id == keyBinding.category.id }) {
                categoryList.add(keyBinding.category)
            }

            return keyBinding
        }
    }
}