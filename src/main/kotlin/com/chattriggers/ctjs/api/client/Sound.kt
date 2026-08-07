package com.chattriggers.ctjs.api.client

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.internal.mixins.AbstractSoundInstanceAccessor
import com.chattriggers.ctjs.internal.mixins.sound.SoundAccessor
import com.chattriggers.ctjs.internal.mixins.sound.SoundManagerAccessor
import com.chattriggers.ctjs.internal.mixins.sound.SoundEngineAccessor
import com.chattriggers.ctjs.MCAttenuationType
import com.chattriggers.ctjs.MCSound
import com.chattriggers.ctjs.internal.utils.asMixin
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.Sound.Type
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.server.packs.resources.ResourceMetadata
import net.minecraft.server.packs.metadata.MetadataSectionType
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.IoSupplier
import net.minecraft.server.packs.resources.Resource
import net.minecraft.world.phys.Vec3
import net.minecraft.util.RandomSource
import org.mozilla.javascript.NativeObject
import java.io.File
import java.io.InputStream
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

/**
 * Instances a new Sound with certain properties. These properties
 * should be passed through as a normal JavaScript object.
 *
 * REQUIRED:
 * - source (String) - a namespaced-identifier (e.g. `minecraft:music_disc.cat`) for a Minecraft sound, or a filename
 *                     relative to ChatTriggers assets directory
 *
 * OPTIONAL:
 * - stream (boolean) - whether to stream this sound rather than preload it (should be true for large files), defaults to false
 *
 * CONFIGURABLE (can be set in config object, or changed later):
 * - category (SoundCategory) - which category this sound should be a part of, see [setCategory].
 * - volume (float) - volume of the sound, see [setVolume]
 * - pitch (float) - pitch of the sound, see [setPitch]
 * - x, y, z (float) - location of the sound, see [setPosition]. Defaults to the players position.
 * - attenuationType (AttenuationType) - fade out type of the sound, see [setAttenuationType]
 * - attenuation (int) - The attenuation distance, see [setAttenuation]
 * - loop (boolean) - whether to loop this sound over and over, defaults to false
 * - loopDelay (int) - Ticks to delay between looping this sound
 *
 * @param config the JavaScript config object
 */
class Sound(private val config: NativeObject) {
    private lateinit var identifier: ResourceLocation
    private lateinit var soundImpl: SoundImpl
    private lateinit var sound: MCSound
    private var isCustom = false

    private var isPaused = false

    private val source = config["source"]?.toString() ?: throw IllegalArgumentException("Sound source is null.")

    // Before bootstrap, we need to store the values ourselves. Afterward, however, we should
    // derive the values from the actual sound object. This switches implementations at the
    // end of bootstrap()
    private var soundData: SoundData = InitialSoundData(config)

    private fun bootstrap() {
        if (::sound.isInitialized)
            return

        CTJS.sounds.add(this)

        val soundManagerAccessor = Client.getMinecraft().soundManager.asMixin<SoundManagerAccessor>()
        val soundFile = File(CTJS.assetsDir, source)
        if (soundFile.exists()) {
            isCustom = true
            identifier = makeIdentifier(source)
            val resource = Resource(CTResourcePack, soundFile::inputStream, ResourceMetadata::EMPTY)
            soundManagerAccessor.soundCache[identifier.withPrefix("sounds/").withSuffix(".ogg")] =
                resource
        } else {
            identifier = ResourceLocation.parse(source)
        }

        soundImpl = SoundImpl(SoundEvent.createVariableRangeEvent(identifier), soundData.category.toMC(), soundData.attenuationType.toMC())
        sound = MCSound(
            identifier,
            { 1f },
            { 1f },
            1,
            Type.FILE,
            soundData.stream,
            false,
            soundData.attenuation,
        )

        if (isCustom) {
            soundManagerAccessor.registry[identifier] = WeighedSoundEvents(identifier, null).apply {
                addSound(sound)
            }
        }

        val initialData = soundData
        soundData = BootstrappedSoundData(sound, soundImpl)

        // Apply all initial values as the user may have changed them
        soundData.loop = initialData.loop
        soundData.loopDelay = initialData.loopDelay
        soundData.x = initialData.x
        soundData.y = initialData.y
        soundData.z = initialData.z
        soundData.attenuation = initialData.attenuation
        soundData.category = initialData.category
        soundData.attenuationType = initialData.attenuationType
        soundData.volume = initialData.volume
        soundData.pitch = initialData.pitch
    }

    fun destroy() {
        stop()
        if (isCustom) {
            val soundManagerAccessor = Client.getMinecraft().soundManager.asMixin<SoundManagerAccessor>()
            soundManagerAccessor.registry.remove(identifier)
            soundManagerAccessor.soundCache.remove(identifier)
        }
    }

    /**
     * Gets the category of this sound, making it respect the Player's sound volume sliders.
     *
     * @return the category
     */
    fun getCategory() = soundData.category

    /**
     * Sets the category of this sound, making it respect the Player's sound volume sliders.
     *
     * @param category the category
     */
    fun setCategory(category: Category) = apply {
        soundData.category = category
    }

    /**
     * Gets this sound's volume.
     *
     * @return A float value (0.0f - 1.0f).
     */
    fun getVolume() = soundData.volume

    /**
     * Sets this sound's volume.
     *
     * @param volume A float value (0.0f - 1.0f).
     */
    fun setVolume(volume: Float) = apply {
        soundData.volume = volume
    }

    fun getX() = soundData.x

    fun getY() = soundData.y

    fun getZ() = soundData.z

    fun setX(x: Double) = apply {
        soundData.x = x
    }

    fun setY(y: Double) = apply {
        soundData.y = y
    }

    fun setZ(z: Double) = apply {
        soundData.z = z
    }

    fun getPosition() = Vec3(getX(), getY(), getZ())

    fun setPosition(x: Double, y: Double, z: Double) = apply {
        soundData.x = x
        soundData.y = y
        soundData.z = z
    }

    /**
     * Gets this sound's pitch.
     *
     * @return A float value (0.5f - 2.0f).
     */
    fun getPitch() = soundData.pitch

    /**
     * Sets this sound's pitch.
     *
     * @param pitch A float value (0.5f - 2.0f).
     */
    fun setPitch(pitch: Float) = apply {
        soundData.pitch = pitch
    }

    /**
     * Gets the attenuation type (fade out over space) of the sound
     *
     * @return The type of Attenuation
     */
    fun getAttenuationType() = soundData.attenuationType

    /**
     * Sets the attenuation type (fade out over space) of the sound
     *
     * @param attenuationType The type of Attenuation
     */
    fun setAttenuationType(attenuationType: AttenuationType) = apply {
        soundData.attenuationType = attenuationType
    }

    /**
     * Gets the attenuation distance of the sound
     */
    fun getAttenuation() = soundData.attenuation

    /**
     * Sets the attenuation distance of the sound
     */
    fun setAttenuation(attenuation: Int) = apply {
        soundData.attenuation = attenuation
    }

    /**
     * Gets whether the sound should repeat after finishing
     */
    fun getLoop() = soundData.loop

    /**
     * Sets whether the sound should repeat after finishing
     */
    fun setLoop(loop: Boolean) = apply {
        soundData.loop = loop
    }

    /**
     * Gets the tick delay after finishing before looping again (if getLoop() is true)
     */
    fun getLoopDelay() = soundData.loopDelay

    /**
     * Sets the tick delay after finishing before looping again (if getLoop() is true)
     */
    fun setLoopDelay(loopDelay: Int) = apply {
        soundData.loopDelay = loopDelay
    }

    /**
     * Plays/resumes the sound. This requires the world to be loaded
     */
    @JvmOverloads
    fun play(delay: Int = 0) {
        // TODO: Figure out how to work without a world
        require (World.isLoaded()) { "Can not play a custom sound outside the world" }

        bootstrap()

        // soundSystem.play() does a lot of setup and, most importantly, creates a new
        // source for the sound. If we have previously paused, we avoid all that setup
        // and instead directly invoke the play method from OpenAL via Source.play
        if (!isPaused) {
            soundSystem.playDelayed(soundImpl, delay)
        } else {
            Client.scheduleTask(delay) {
                isPaused = false
                soundSystem.asMixin<SoundEngineAccessor>().instanceToChannel[soundImpl]?.execute {
                    it.unpause()
                }
            }
        }
    }

    /**
     * Pauses the sound, to be resumed later. This requires the world to be loaded
     */
    fun pause() {
        // TODO: Figure out how to work without a world
        require (World.isLoaded()) { "Can not pause a custom sound outside the world" }

        bootstrap()

        Client.scheduleTask {
            isPaused = true
            soundSystem.asMixin<SoundEngineAccessor>().instanceToChannel[soundImpl]?.execute {
                it.pause()
            }
        }
    }

    /**
     * Completely stops the sound. This requires the world to be loaded
     */
    fun stop() {
        // TODO: Figure out how to work without a world
        require (World.isLoaded()) { "Can not stop a custom sound outside the world" }

        bootstrap()
        soundSystem.stop(soundImpl)
        isPaused = false
    }

    /**
     * Immediately restarts the sound. This requires the world to be loaded
     */
    fun rewind() {
        stop()
        play()
    }

    private fun makeIdentifier(source: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(
            CTJS.MOD_ID,
            Path(source).nameWithoutExtension.lowercase().filter { it in validIdentChars } + "_${counter++}",
        )
    }

    private interface SoundData {
        var loop: Boolean
        var loopDelay: Int
        var stream: Boolean
        var volume: Float
        var pitch: Float
        var x: Double
        var y: Double
        var z: Double
        var attenuation: Int
        var category: Category
        var attenuationType: AttenuationType
    }

    private class InitialSoundData(config: NativeObject) : SoundData {
        override var loop = config.getOrDefault("loop", false) as Boolean
        override var loopDelay = (config.getOrDefault("loopDelay", 0) as Number).toInt()
        override var stream = config.getOrDefault("stream", false) as Boolean
        override var volume = (config.getOrDefault("volume", 1f) as Number).toFloat()
        override var pitch = (config.getOrDefault("pitch", 1f) as Number).toFloat()
        override var x = (config.getOrDefault("x", Player.getX()) as Number).toDouble()
        override var y = (config.getOrDefault("y", Player.getY()) as Number).toDouble()
        override var z = (config.getOrDefault("z", Player.getZ()) as Number).toDouble()
        override var attenuation = (config.getOrDefault("attenuation", 16) as Number).toInt()
        override var category = config["category"]?.let(Category::from) ?: Category.MASTER
        override var attenuationType = config["attenuationType"]?.let(AttenuationType::from) ?: AttenuationType.LINEAR
    }

    private class BootstrappedSoundData(
        private val sound: MCSound,
        private val impl: SoundImpl,
    ) : SoundData {
        private val mixedSound: SoundAccessor = sound.asMixin()
        private val mixedImpl: AbstractSoundInstanceAccessor = impl.asMixin()

        override var volume by impl::volume
        override var pitch by impl::pitch

        override var loop: Boolean
            get() = impl.isLooping
            set(value) {
                mixedImpl.setLooping(value)
            }

        override var loopDelay: Int
            get() = impl.delay
            set(value) {
                mixedImpl.setDelay(value)
            }

        override var stream: Boolean
            get() = error("stream should not be accessed after bootstrap")
            set(_) = error("stream should not be accessed after bootstrap")

        override var x: Double
            get() = impl.x
            set(value) {
                impl.setPosition(value, y, z)
            }

        override var y: Double
            get() = impl.y
            set(value) {
                impl.setPosition(x, value, z)
            }

        override var z: Double
            get() = impl.z
            set(value) {
                impl.setPosition(x, y, value)
            }

        override var attenuation: Int
            get() = sound.attenuationDistance
            set(value) {
                mixedSound.setAttenuationDistance(value)
            }

        override var category: Category
            get() = Category.fromMC(impl.categoryOverride)
            set(value) {
                impl.categoryOverride = value.toMC()
            }

        override var attenuationType: AttenuationType
            get() = AttenuationType.fromMC(impl.attenuation)
            set(value) {
                impl.setAttenuationType(value.toMC())
            }
    }

    private class SoundImpl(
        soundEvent: SoundEvent,
        soundCategory: SoundSource,
        attenuationType: MCAttenuationType,
    ) : AbstractTickableSoundInstance(soundEvent, soundCategory, RandomSource.create()) {
        var categoryOverride: SoundSource = super.source

        init {
            this.attenuation = attenuationType
        }

        override fun tick() {
            if (!World.isLoaded())
                stop()
        }

        override fun getSource(): SoundSource {
            return categoryOverride
        }

        fun setPosition(x: Double, y: Double, z: Double) {
            this.x = x
            this.y = y
            this.z = z
        }

        fun setAttenuationType(attenuationType: MCAttenuationType) {
            this.attenuation = attenuationType
        }

        fun setVolume(volume: Float) {
            this.volume = volume.coerceIn(0f, 1f)
        }

        fun setPitch(pitch: Float) {
            this.pitch = pitch.coerceIn(0.5f, 2f)
        }
    }

    enum class Category(override val mcValue: SoundSource) : CTWrapper<SoundSource> {
        MASTER(SoundSource.MASTER),
        MUSIC(SoundSource.MUSIC),
        RECORDS(SoundSource.RECORDS),
        WEATHER(SoundSource.WEATHER),
        BLOCKS(SoundSource.BLOCKS),
        HOSTILE(SoundSource.HOSTILE),
        NEUTRAL(SoundSource.NEUTRAL),
        PLAYERS(SoundSource.PLAYERS),
        AMBIENT(SoundSource.AMBIENT),
        VOICE(SoundSource.VOICE);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: SoundSource) = entries.first { it.mcValue == mcValue }

            @JvmStatic
            fun from(value: Any) = when (value) {
                is CharSequence -> valueOf(value.toString())
                is SoundSource -> fromMC(value)
                is Category -> value
                else -> throw IllegalArgumentException("Cannot create Sound.Category from $value")
            }
        }
    }

    enum class AttenuationType(override val mcValue: MCAttenuationType) : CTWrapper<MCAttenuationType> {
        NONE(MCAttenuationType.NONE),
        LINEAR(MCAttenuationType.LINEAR);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: MCAttenuationType) = entries.first { it.mcValue == mcValue }

            @JvmStatic
            fun from(value: Any) = when (value) {
                is CharSequence -> valueOf(value.toString())
                is MCAttenuationType -> fromMC(value)
                is AttenuationType -> value
                else -> throw IllegalArgumentException("Cannot create Sound.Category from $value")
            }
        }
    }

    private object CTResourcePack : PackResources {
        override fun packId() = CTJS.MOD_ID

        override fun close() {
            throw UnsupportedOperationException()
        }

        override fun getRootResource(vararg segments: String?): IoSupplier<InputStream>? {
            throw UnsupportedOperationException()
        }

        override fun getResource(type: PackType, id: ResourceLocation): IoSupplier<InputStream>? {
            throw UnsupportedOperationException()
        }

        override fun listResources(
            type: PackType,
            namespace: String,
            prefix: String,
            consumer: PackResources.ResourceOutput
        ) {
            throw UnsupportedOperationException()
        }

        override fun getNamespaces(type: PackType): MutableSet<String> {
            throw UnsupportedOperationException()
        }

        override fun <T : Any?> getMetadataSection(metaReader: MetadataSectionType<T>?): T? {
            throw UnsupportedOperationException()
        }

        override fun location(): PackLocationInfo {
            throw NotImplementedError()
        }
    }

    private companion object {
        private val soundSystem by lazy {
            Client.getMinecraft().soundManager.asMixin<SoundManagerAccessor>().soundEngine
        }

        private val validIdentChars = setOf(
            *('a'..'z').toList().toTypedArray(),
            *('0'..'9').toList().toTypedArray(),
            '_', '.', '-', '/',
        )
        private var counter = 0
    }
}
