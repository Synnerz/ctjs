package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.MCBlockPos
import com.chattriggers.ctjs.MCParticle
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.entity.Particle
import com.chattriggers.ctjs.api.entity.PlayerMP
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.world.block.Block
import com.chattriggers.ctjs.api.world.block.BlockPos
import com.chattriggers.ctjs.api.world.block.BlockType
import com.chattriggers.ctjs.internal.mixins.ClientChunkManagerAccessor
import com.chattriggers.ctjs.internal.mixins.ClientChunkMapAccessor
import com.chattriggers.ctjs.internal.mixins.ClientWorldAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import com.chattriggers.ctjs.internal.utils.toIdentifier
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ColorParticleOption
import net.minecraft.core.particles.DustColorTransitionOptions
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.SculkChargeParticleOptions
import net.minecraft.core.particles.ShriekParticleOption
import net.minecraft.core.particles.VibrationParticleOption
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.BlockPositionSource
import kotlin.math.roundToInt

object World {
    @JvmStatic
    fun toMC() = Minecraft.getInstance().level

    @JvmField
    val spawn = SpawnWrapper()

    @JvmField
    val particle = ParticleWrapper()

    @JvmField
    val border = BorderWrapper()

    /**
     * Gets Minecraft's [ClientWorld] object
     *
     * @return The Minecraft [ClientWorld] object
     */
    @Deprecated("Use toMC", ReplaceWith("toMC()"))
    @JvmStatic
    fun getWorld(): ClientLevel? = toMC()

    @JvmStatic
    fun isLoaded(): Boolean = toMC() != null

    @JvmStatic
    fun isRaining(): Boolean = toMC()?.isRaining ?: false

    @JvmStatic
    fun getRainingStrength(): Float = toMC()?.getRainLevel(Renderer.partialTicks) ?: -1f

    @JvmStatic
    fun getTime(): Long = toMC()?.dayTime ?: -1L

    @JvmStatic
    fun getDifficulty(): Settings.Difficulty? = toMC()?.difficulty?.let(Settings.Difficulty::fromMC)

    @JvmStatic
    // FIXME
    fun getMoonPhase(): Int = -1 // toMC()?.moonPhase?.index ?: -1

    /**
     * Gets the [Block] at a location in the world.
     *
     * @param x the x position
     * @param y the y position
     * @param z the z position
     * @return the [Block] at the location
     */
    @JvmStatic
    fun getBlockAt(x: Number, y: Number, z: Number) = getBlockAt(BlockPos(x, y, z))

    /**
     * Gets the [Block] at a location in the world.
     *
     * @param pos The block position
     * @return the [Block] at the location
     */
    @JvmStatic
    fun getBlockAt(pos: BlockPos): Block {
        return Block(BlockType(getBlockStateAt(pos).block), pos)
    }

    /**
     * Gets the [BlockState] at a location in the world.
     *
     * @param pos The block position
     * @return the [BlockState] at the location
     */
    @JvmStatic
    fun getBlockStateAt(pos: BlockPos): BlockState {
        return toMC()!!.getBlockState(pos.toMC())
    }

    /**
     * Gets the skylight level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the skylight level at the location
     */
    @JvmStatic
    fun getSkyLightLevel(x: Int, y: Int, z: Int): Int = getSkyLightLevel(BlockPos(x, y, z))

    /**
     * Gets the skylight level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param pos The block position
     * @return the skylight level at the location
     */
    @JvmStatic
    fun getSkyLightLevel(pos: BlockPos): Int {
        // TODO: i don't think this is right
        return toMC()?.getMaxLocalRawBrightness(pos.toMC(), LightLayer.SKY.ordinal) ?: 0
    }

    /**
     * Gets the block light level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the block light level at the location
     */
    @JvmStatic
    fun getBlockLightLevel(x: Int, y: Int, z: Int): Int = getBlockLightLevel(BlockPos(x, y, z))

    /**
     * Gets the block light level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param pos The block position
     * @return the block light level at the location
     */
    @JvmStatic
    fun getBlockLightLevel(pos: BlockPos): Int {
        return toMC()?.getMaxLocalRawBrightness(pos.toMC()) ?: 0
    }

    /**
     * Gets all of the players in the world, and returns their wrapped versions.
     *
     * @return the players
     */
    @JvmStatic
    fun getAllPlayers(): List<PlayerMP> = toMC()?.players()?.map(::PlayerMP) ?: listOf()

    /**
     * Gets a player by their username, must be in the currently loaded chunks!
     *
     * @param name the username
     * @return the player with said username, or null if they don't exist.
     */
    @JvmStatic
    fun getPlayerByName(name: String) = getAllPlayers().firstOrNull { it.getName() == name }

    @JvmStatic
    fun hasPlayer(name: String) = getPlayerByName(name) != null

    @JvmStatic
    fun getChunk(x: Int, y: Int, z: Int) = Chunk(toMC()!!.getChunkAt(MCBlockPos(x, y, z)))

    @JvmStatic
    fun getAllEntities() = toMC()?.asMixin<ClientWorldAccessor>()?.loadedEntityStorage()?.entityGetter?.all?.map(Entity::fromMC) ?: listOf()

    /**
     * Gets every entity loaded in the world of a certain class
     *
     * @param clazz the class to filter for (Use `Java.type().class` to get this)
     * @return the entity list
     */
    @JvmStatic
    fun getAllEntitiesOfType(clazz: Class<*>): List<Entity> {
        return getAllEntities().filter {
            clazz.isInstance(it.toMC())
        }
    }

    @JvmStatic
    fun getAllBlockEntities(): List<BlockEntity> {
        val chunks = toMC()!!
            .chunkSource.asMixin<ClientChunkManagerAccessor>()
            .storage.asMixin<ClientChunkMapAccessor>()
            .chunks ?: return emptyList()

        val blockEntities = mutableListOf<BlockEntity>()

        for (i in 0 until chunks.length()) {
            blockEntities += Chunk(chunks.getPlain(i) ?: continue).getAllBlockEntities()
        }

        return blockEntities
    }

    @JvmStatic
    fun getAllBlockEntitiesOfType(clazz: Class<*>): List<BlockEntity> {
        return getAllBlockEntities().filter {
            clazz.isInstance(it.toMC())
        }
    }

    /**
     * Returns the TPS of the current world.
     *
     * On modern version (1.20.3+), this is variable. On earlier versions,
     * it is always 20.
     */
    @JvmStatic
    fun getTicksPerSecond(): Int {
        val mpt = toMC()?.tickRateManager()?.millisecondsPerTick() ?: return 20
        return (1000.0 / mpt).roundToInt()
    }

    /**
     * World border object to get border parameters
     */
    class BorderWrapper {
        /**
         * Gets the border center x location.
         *
         * @return the border center x location
         */
        fun getCenterX(): Double = toMC()!!.worldBorder.centerX

        /**
         * Gets the border center z location.
         *
         * @return the border center z location
         */
        fun getCenterZ(): Double = toMC()!!.worldBorder.centerZ

        /**
         * Gets the border size.
         *
         * @return the border size
         */
        fun getSize(): Double = toMC()!!.worldBorder.size

        /**
         * Gets the border target size.
         *
         * @return the border target size
         */
        fun getTargetSize(): Double = toMC()!!.worldBorder.lerpTarget

        /**
         * Gets the border time until the target size is met.
         *
         * @return the border time until target
         */
        fun getTimeUntilTarget(): Long = toMC()!!.worldBorder.lerpTime
    }

    /**
     * World spawn object for getting spawn location.
     */
    class SpawnWrapper {
        /**
         * Gets the spawn x location.
         *
         * @return the spawn x location.
         */
        fun getX(): Int = toMC()!!.respawnData.pos().x

        /**
         * Gets the spawn y location.
         *
         * @return the spawn y location.
         */
        fun getY(): Int = toMC()!!.respawnData.pos().y

        /**
         * Gets the spawn z location.
         *
         * @return the spawn z location.
         */
        fun getZ(): Int = toMC()!!.respawnData.pos().z
    }

    class ParticleWrapper {
        /**
         * Gets an array of all the different particle names you can pass
         * to [spawnParticle]
         *
         * @return the array of name strings
         */
        fun getParticleNames(): List<String> = BuiltInRegistries.PARTICLE_TYPE.keySet().map { it.path }.toList()

        /**
         * Spawns a particle into the world with the given attributes,
         * which can be configured further with the returned [com.chattriggers.ctjs.api.entity.Particle]
         *
         * @param particle the name of the particle to spawn, see [getParticleNames]
         * @param x the x coordinate to spawn the particle at
         * @param y the y coordinate to spawn the particle at
         * @param z the z coordinate to spawn the particle at
         * @param xSpeed the motion the particle should have in the x direction
         * @param ySpeed the motion the particle should have in the y direction
         * @param zSpeed the motion the particle should have in the z direction
         * @return the newly spawned particle for further configuration
         */
        fun spawnParticle(
            particle: String,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double,
        ): Particle? {
            val particleType = BuiltInRegistries.PARTICLE_TYPE.get(particle.toIdentifier()).get().value()

            requireNotNull(particleType) {
                "Invalid particle parameter"
            }

            // TODO: does this work?
            val effect = if (particleType is ParticleType<*>) {
                particleType
            } else {
                val blockPos = BlockPos(x, y, z)
                val blockState = getBlockStateAt(blockPos)

                when (particleType) {
                    ParticleTypes.BLOCK -> BlockParticleOption(ParticleTypes.BLOCK, blockState)
                    ParticleTypes.BLOCK_MARKER -> BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockState)
                    ParticleTypes.DUST -> DustParticleOptions.REDSTONE
                    ParticleTypes.DUST_COLOR_TRANSITION -> DustColorTransitionOptions.SCULK_PARTICLE_COLOR
                    ParticleTypes.DUST_PILLAR -> BlockParticleOption(ParticleTypes.DUST_PILLAR, blockState)
                    ParticleTypes.ENTITY_EFFECT -> ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 1f, 0f, 0f)
                    ParticleTypes.FALLING_DUST -> BlockParticleOption(ParticleTypes.FALLING_DUST, blockState)
                    ParticleTypes.ITEM -> ItemParticleOption(ParticleTypes.ITEM, ItemStack(Items.STONE, 1))
                    ParticleTypes.SCULK_CHARGE -> SculkChargeParticleOptions(0f)
                    ParticleTypes.SHRIEK -> ShriekParticleOption(0)
                    ParticleTypes.VIBRATION -> VibrationParticleOption(BlockPositionSource(blockPos.toMC()), 0)

                    else -> throw IllegalStateException("Particle not accounted for: $particle")
                }
            }

            val fx = Client.getMinecraft().particleManager.addParticle(
                effect,
                x,
                y,
                z,
                xSpeed,
                ySpeed,
                zSpeed
            )

            return fx?.let(::Particle)
        }

        fun spawnParticle(particle: MCParticle): Particle {
            Client.getMinecraft().particleManager.addParticle(particle)
            return Particle(particle)
        }
    }
}
