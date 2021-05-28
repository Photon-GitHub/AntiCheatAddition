package de.photon.aacadditionpro;

import de.photon.aacadditionpro.user.User;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Statistic;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A simple dummy {@link User} to prevent null cases and allow for testing.
 */
public class DummyUser extends User
{
    protected DummyUser()
    {
        super(new Player()
        {
            @NotNull
            @Override
            public String getDisplayName()
            {
                return "DummyDisplayName";
            }

            @Override
            public void setDisplayName(@Nullable String name)
            {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public String getPlayerListName()
            {
                return "DummyListName";
            }

            @Override
            public void setPlayerListName(@Nullable String name)
            {
                throw new UnsupportedOperationException();
            }

            @Nullable
            @Override
            public String getPlayerListHeader()
            {
                return "DummyListHeader";
            }

            @Override
            public void setPlayerListHeader(@Nullable String header)
            {
                throw new UnsupportedOperationException();
            }

            @Nullable
            @Override
            public String getPlayerListFooter()
            {
                return "DummyListFooter";
            }

            @Override
            public void setPlayerListFooter(@Nullable String footer)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setPlayerListHeaderFooter(@Nullable String header, @Nullable String footer)
            {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public Location getCompassTarget()
            {
                return new Location(null, 0, 0, 0);
            }

            @Override
            public void setCompassTarget(@NotNull Location loc)
            {
                throw new UnsupportedOperationException();
            }

            @Nullable
            @Override
            public InetSocketAddress getAddress()
            {
                return null;
            }

            @Override
            public void sendRawMessage(@NotNull String message)
            {
                // Nothing.
            }

            @Override
            public void kickPlayer(@Nullable String message)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void chat(@NotNull String msg)
            {
                // Nothing.
            }

            @Override
            public boolean performCommand(@NotNull String command)
            {
                return false;
            }

            @Override
            public boolean isOnGround()
            {
                return false;
            }

            @Override
            public boolean isSneaking()
            {
                return false;
            }

            @Override
            public void setSneaking(boolean sneak)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isSprinting()
            {
                return false;
            }

            @Override
            public void setSprinting(boolean sprinting)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void saveData()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void loadData()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isSleepingIgnored()
            {
                return false;
            }

            @Override
            public void setSleepingIgnored(boolean isSleeping)
            {
                throw new UnsupportedOperationException();
            }

            @Nullable
            @Override
            public Location getBedSpawnLocation()
            {
                return new Location(null, 10, 10, 10);
            }

            @Override
            public void setBedSpawnLocation(@Nullable Location location)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setBedSpawnLocation(@Nullable Location location, boolean force)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void playNote(@NotNull Location loc, byte instrument, byte note)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void playNote(@NotNull Location loc, @NotNull Instrument instrument, @NotNull Note note)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void playSound(@NotNull Location location, @NotNull String sound, float volume, float pitch)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void playSound(@NotNull Location location, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void playSound(@NotNull Location location, @NotNull String sound, @NotNull SoundCategory category, float volume, float pitch)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void stopSound(@NotNull Sound sound)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void stopSound(@NotNull String sound)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void stopSound(@NotNull Sound sound, @Nullable SoundCategory category)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void stopSound(@NotNull String sound, @Nullable SoundCategory category)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void playEffect(@NotNull Location loc, @NotNull Effect effect, int data)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> void playEffect(@NotNull Location loc, @NotNull Effect effect, @Nullable T data)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void sendBlockChange(@NotNull Location loc, @NotNull Material material, byte data)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void sendBlockChange(@NotNull Location loc, @NotNull BlockData block)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void sendBlockDamage(@NotNull Location loc, float progress)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean sendChunkChange(@NotNull Location loc, int sx, int sy, int sz, @NotNull byte[] data)
            {
                return false;
            }

            @Override
            public void sendSignChange(@NotNull Location loc, @Nullable String[] lines) throws IllegalArgumentException
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void sendSignChange(@NotNull Location loc, @Nullable String[] lines, @NotNull DyeColor dyeColor) throws IllegalArgumentException
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void sendMap(@NotNull MapView map)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void updateInventory()
            {
                // Nothing
            }

            @Override
            public void setPlayerTime(long time, boolean relative)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getPlayerTime()
            {
                return 0;
            }

            @Override
            public long getPlayerTimeOffset()
            {
                return 0;
            }

            @Override
            public boolean isPlayerTimeRelative()
            {
                return false;
            }

            @Override
            public void resetPlayerTime()
            {

            }

            @Nullable
            @Override
            public WeatherType getPlayerWeather()
            {
                return null;
            }

            @Override
            public void setPlayerWeather(@NotNull WeatherType type)
            {

            }

            @Override
            public void resetPlayerWeather()
            {

            }

            @Override
            public void giveExp(int amount)
            {

            }

            @Override
            public void giveExpLevels(int amount)
            {

            }

            @Override
            public float getExp()
            {
                return 0;
            }

            @Override
            public void setExp(float exp)
            {

            }

            @Override
            public int getLevel()
            {
                return 0;
            }

            @Override
            public void setLevel(int level)
            {

            }

            @Override
            public int getTotalExperience()
            {
                return 0;
            }

            @Override
            public void setTotalExperience(int exp)
            {

            }

            @Override
            public void sendExperienceChange(float progress)
            {

            }

            @Override
            public void sendExperienceChange(float progress, int level)
            {

            }

            @Override
            public boolean getAllowFlight()
            {
                return false;
            }

            @Override
            public void setAllowFlight(boolean flight)
            {

            }

            @Override
            public void hidePlayer(@NotNull Player player)
            {

            }

            @Override
            public void hidePlayer(@NotNull Plugin plugin, @NotNull Player player)
            {

            }

            @Override
            public void showPlayer(@NotNull Player player)
            {

            }

            @Override
            public void showPlayer(@NotNull Plugin plugin, @NotNull Player player)
            {

            }

            @Override
            public boolean canSee(@NotNull Player player)
            {
                return false;
            }

            @Override
            public boolean isFlying()
            {
                return false;
            }

            @Override
            public void setFlying(boolean value)
            {

            }

            @Override
            public float getFlySpeed()
            {
                return 0;
            }

            @Override
            public void setFlySpeed(float value) throws IllegalArgumentException
            {

            }

            @Override
            public float getWalkSpeed()
            {
                return 0;
            }

            @Override
            public void setWalkSpeed(float value) throws IllegalArgumentException
            {

            }

            @Override
            public void setTexturePack(@NotNull String url)
            {

            }

            @Override
            public void setResourcePack(@NotNull String url)
            {

            }

            @Override
            public void setResourcePack(@NotNull String url, @NotNull byte[] hash)
            {

            }

            @NotNull
            @Override
            public Scoreboard getScoreboard()
            {
                return null;
            }

            @Override
            public void setScoreboard(@NotNull Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException
            {

            }

            @Override
            public boolean isHealthScaled()
            {
                return false;
            }

            @Override
            public void setHealthScaled(boolean scale)
            {

            }

            @Override
            public double getHealthScale()
            {
                return 0;
            }

            @Override
            public void setHealthScale(double scale) throws IllegalArgumentException
            {

            }

            @Nullable
            @Override
            public Entity getSpectatorTarget()
            {
                return null;
            }

            @Override
            public void setSpectatorTarget(@Nullable Entity entity)
            {

            }

            @Override
            public void sendTitle(@Nullable String title, @Nullable String subtitle)
            {

            }

            @Override
            public void sendTitle(@Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut)
            {

            }

            @Override
            public void resetTitle()
            {

            }

            @Override
            public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count)
            {

            }

            @Override
            public void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count)
            {

            }

            @Override
            public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, @Nullable T data)
            {

            }

            @Override
            public <T> void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, @Nullable T data)
            {

            }

            @Override
            public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ)
            {

            }

            @Override
            public void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ)
            {

            }

            @Override
            public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, @Nullable T data)
            {

            }

            @Override
            public <T> void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, @Nullable T data)
            {

            }

            @Override
            public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, double extra)
            {

            }

            @Override
            public void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra)
            {

            }

            @Override
            public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable T data)
            {

            }

            @Override
            public <T> void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable T data)
            {

            }

            @NotNull
            @Override
            public AdvancementProgress getAdvancementProgress(@NotNull Advancement advancement)
            {
                return null;
            }

            @Override
            public int getClientViewDistance()
            {
                return 0;
            }

            @Override
            public int getPing()
            {
                return 2;
            }

            @NotNull
            @Override
            public String getLocale()
            {
                return null;
            }

            @Override
            public void updateCommands()
            {

            }

            @Override
            public void openBook(@NotNull ItemStack book)
            {

            }

            @NotNull
            @Override
            public Spigot spigot()
            {
                return null;
            }

            @Override
            public boolean isOnline()
            {
                return false;
            }

            @Override
            public boolean isBanned()
            {
                return false;
            }

            @Override
            public boolean isWhitelisted()
            {
                return false;
            }

            @Override
            public void setWhitelisted(boolean value)
            {

            }

            @Nullable
            @Override
            public Player getPlayer()
            {
                return null;
            }

            @Override
            public long getFirstPlayed()
            {
                return 0;
            }

            @Override
            public long getLastPlayed()
            {
                return 0;
            }

            @Override
            public boolean hasPlayedBefore()
            {
                return false;
            }

            @Override
            public void incrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException
            {

            }

            @Override
            public void decrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException
            {

            }

            @Override
            public void incrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException
            {

            }

            @Override
            public void decrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException
            {

            }

            @Override
            public void setStatistic(@NotNull Statistic statistic, int newValue) throws IllegalArgumentException
            {

            }

            @Override
            public int getStatistic(@NotNull Statistic statistic) throws IllegalArgumentException
            {
                return 0;
            }

            @Override
            public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException
            {

            }

            @Override
            public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException
            {

            }

            @Override
            public int getStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException
            {
                return 0;
            }

            @Override
            public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException
            {

            }

            @Override
            public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException
            {

            }

            @Override
            public void setStatistic(@NotNull Statistic statistic, @NotNull Material material, int newValue) throws IllegalArgumentException
            {

            }

            @Override
            public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException
            {

            }

            @Override
            public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException
            {

            }

            @Override
            public int getStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException
            {
                return 0;
            }

            @Override
            public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) throws IllegalArgumentException
            {

            }

            @Override
            public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount)
            {

            }

            @Override
            public void setStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int newValue)
            {

            }

            @NotNull
            @Override
            public Map<String, Object> serialize()
            {
                return null;
            }

            @Override
            public boolean isConversing()
            {
                return false;
            }

            @Override
            public void acceptConversationInput(@NotNull String input)
            {

            }

            @Override
            public boolean beginConversation(@NotNull Conversation conversation)
            {
                return false;
            }

            @Override
            public void abandonConversation(@NotNull Conversation conversation)
            {

            }

            @Override
            public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details)
            {

            }

            @Override
            public void sendRawMessage(@Nullable UUID sender, @NotNull String message)
            {

            }

            @NotNull
            @Override
            public String getName()
            {
                return null;
            }

            @NotNull
            @Override
            public PlayerInventory getInventory()
            {
                return null;
            }

            @NotNull
            @Override
            public Inventory getEnderChest()
            {
                return null;
            }

            @NotNull
            @Override
            public MainHand getMainHand()
            {
                return null;
            }

            @Override
            public boolean setWindowProperty(@NotNull InventoryView.Property prop, int value)
            {
                return false;
            }

            @NotNull
            @Override
            public InventoryView getOpenInventory()
            {
                return null;
            }

            @Nullable
            @Override
            public InventoryView openInventory(@NotNull Inventory inventory)
            {
                return null;
            }

            @Nullable
            @Override
            public InventoryView openWorkbench(@Nullable Location location, boolean force)
            {
                return null;
            }

            @Nullable
            @Override
            public InventoryView openEnchanting(@Nullable Location location, boolean force)
            {
                return null;
            }

            @Override
            public void openInventory(@NotNull InventoryView inventory)
            {

            }

            @Nullable
            @Override
            public InventoryView openMerchant(@NotNull Villager trader, boolean force)
            {
                return null;
            }

            @Nullable
            @Override
            public InventoryView openMerchant(@NotNull Merchant merchant, boolean force)
            {
                return null;
            }

            @Override
            public void closeInventory()
            {

            }

            @NotNull
            @Override
            public ItemStack getItemInHand()
            {
                return null;
            }

            @Override
            public void setItemInHand(@Nullable ItemStack item)
            {

            }

            @NotNull
            @Override
            public ItemStack getItemOnCursor()
            {
                return null;
            }

            @Override
            public void setItemOnCursor(@Nullable ItemStack item)
            {

            }

            @Override
            public boolean hasCooldown(@NotNull Material material)
            {
                return false;
            }

            @Override
            public int getCooldown(@NotNull Material material)
            {
                return 0;
            }

            @Override
            public void setCooldown(@NotNull Material material, int ticks)
            {

            }

            @Override
            public int getSleepTicks()
            {
                return 0;
            }

            @Override
            public boolean sleep(@NotNull Location location, boolean force)
            {
                return false;
            }

            @Override
            public void wakeup(boolean setSpawnLocation)
            {

            }

            @NotNull
            @Override
            public Location getBedLocation()
            {
                return null;
            }

            @NotNull
            @Override
            public GameMode getGameMode()
            {
                return null;
            }

            @Override
            public void setGameMode(@NotNull GameMode mode)
            {

            }

            @Override
            public boolean isBlocking()
            {
                return false;
            }

            @Override
            public boolean isHandRaised()
            {
                return false;
            }

            @Override
            public int getExpToLevel()
            {
                return 0;
            }

            @Override
            public float getAttackCooldown()
            {
                return 0;
            }

            @Override
            public boolean discoverRecipe(@NotNull NamespacedKey recipe)
            {
                return false;
            }

            @Override
            public int discoverRecipes(@NotNull Collection<NamespacedKey> recipes)
            {
                return 0;
            }

            @Override
            public boolean undiscoverRecipe(@NotNull NamespacedKey recipe)
            {
                return false;
            }

            @Override
            public int undiscoverRecipes(@NotNull Collection<NamespacedKey> recipes)
            {
                return 0;
            }

            @Override
            public boolean hasDiscoveredRecipe(@NotNull NamespacedKey recipe)
            {
                return false;
            }

            @NotNull
            @Override
            public Set<NamespacedKey> getDiscoveredRecipes()
            {
                return null;
            }

            @Nullable
            @Override
            public Entity getShoulderEntityLeft()
            {
                return null;
            }

            @Override
            public void setShoulderEntityLeft(@Nullable Entity entity)
            {

            }

            @Nullable
            @Override
            public Entity getShoulderEntityRight()
            {
                return null;
            }

            @Override
            public void setShoulderEntityRight(@Nullable Entity entity)
            {

            }

            @Override
            public boolean dropItem(boolean dropAll)
            {
                return false;
            }

            @Override
            public float getExhaustion()
            {
                return 0;
            }

            @Override
            public void setExhaustion(float value)
            {

            }

            @Override
            public float getSaturation()
            {
                return 0;
            }

            @Override
            public void setSaturation(float value)
            {

            }

            @Override
            public int getFoodLevel()
            {
                return 0;
            }

            @Override
            public void setFoodLevel(int value)
            {

            }

            @Override
            public int getSaturatedRegenRate()
            {
                return 0;
            }

            @Override
            public void setSaturatedRegenRate(int ticks)
            {

            }

            @Override
            public int getUnsaturatedRegenRate()
            {
                return 0;
            }

            @Override
            public void setUnsaturatedRegenRate(int ticks)
            {

            }

            @Override
            public int getStarvationRate()
            {
                return 0;
            }

            @Override
            public void setStarvationRate(int ticks)
            {

            }

            @Override
            public double getEyeHeight()
            {
                return 0;
            }

            @Override
            public double getEyeHeight(boolean ignorePose)
            {
                return 0;
            }

            @NotNull
            @Override
            public Location getEyeLocation()
            {
                return null;
            }

            @NotNull
            @Override
            public List<Block> getLineOfSight(@Nullable Set<Material> transparent, int maxDistance)
            {
                return null;
            }

            @NotNull
            @Override
            public Block getTargetBlock(@Nullable Set<Material> transparent, int maxDistance)
            {
                return null;
            }

            @NotNull
            @Override
            public List<Block> getLastTwoTargetBlocks(@Nullable Set<Material> transparent, int maxDistance)
            {
                return null;
            }

            @Nullable
            @Override
            public Block getTargetBlockExact(int maxDistance)
            {
                return null;
            }

            @Nullable
            @Override
            public Block getTargetBlockExact(int maxDistance, @NotNull FluidCollisionMode fluidCollisionMode)
            {
                return null;
            }

            @Nullable
            @Override
            public RayTraceResult rayTraceBlocks(double maxDistance)
            {
                return null;
            }

            @Nullable
            @Override
            public RayTraceResult rayTraceBlocks(double maxDistance, @NotNull FluidCollisionMode fluidCollisionMode)
            {
                return null;
            }

            @Override
            public int getRemainingAir()
            {
                return 0;
            }

            @Override
            public void setRemainingAir(int ticks)
            {

            }

            @Override
            public int getMaximumAir()
            {
                return 0;
            }

            @Override
            public void setMaximumAir(int ticks)
            {

            }

            @Override
            public int getArrowCooldown()
            {
                return 0;
            }

            @Override
            public void setArrowCooldown(int ticks)
            {

            }

            @Override
            public int getArrowsInBody()
            {
                return 0;
            }

            @Override
            public void setArrowsInBody(int count)
            {

            }

            @Override
            public int getMaximumNoDamageTicks()
            {
                return 0;
            }

            @Override
            public void setMaximumNoDamageTicks(int ticks)
            {

            }

            @Override
            public double getLastDamage()
            {
                return 0;
            }

            @Override
            public void setLastDamage(double damage)
            {

            }

            @Override
            public int getNoDamageTicks()
            {
                return 0;
            }

            @Override
            public void setNoDamageTicks(int ticks)
            {

            }

            @Nullable
            @Override
            public Player getKiller()
            {
                return null;
            }

            @Override
            public boolean addPotionEffect(@NotNull PotionEffect effect)
            {
                return false;
            }

            @Override
            public boolean addPotionEffect(@NotNull PotionEffect effect, boolean force)
            {
                return false;
            }

            @Override
            public boolean addPotionEffects(@NotNull Collection<PotionEffect> effects)
            {
                return false;
            }

            @Override
            public boolean hasPotionEffect(@NotNull PotionEffectType type)
            {
                return false;
            }

            @Nullable
            @Override
            public PotionEffect getPotionEffect(@NotNull PotionEffectType type)
            {
                return null;
            }

            @Override
            public void removePotionEffect(@NotNull PotionEffectType type)
            {

            }

            @NotNull
            @Override
            public Collection<PotionEffect> getActivePotionEffects()
            {
                return null;
            }

            @Override
            public boolean hasLineOfSight(@NotNull Entity other)
            {
                return false;
            }

            @Override
            public boolean getRemoveWhenFarAway()
            {
                return false;
            }

            @Override
            public void setRemoveWhenFarAway(boolean remove)
            {

            }

            @Nullable
            @Override
            public EntityEquipment getEquipment()
            {
                return null;
            }

            @Override
            public boolean getCanPickupItems()
            {
                return false;
            }

            @Override
            public void setCanPickupItems(boolean pickup)
            {

            }

            @Override
            public boolean isLeashed()
            {
                return false;
            }

            @NotNull
            @Override
            public Entity getLeashHolder() throws IllegalStateException
            {
                return null;
            }

            @Override
            public boolean setLeashHolder(@Nullable Entity holder)
            {
                return false;
            }

            @Override
            public boolean isGliding()
            {
                return false;
            }

            @Override
            public void setGliding(boolean gliding)
            {

            }

            @Override
            public boolean isSwimming()
            {
                return false;
            }

            @Override
            public void setSwimming(boolean swimming)
            {

            }

            @Override
            public boolean isRiptiding()
            {
                return false;
            }

            @Override
            public boolean isSleeping()
            {
                return false;
            }

            @Override
            public void setAI(boolean ai)
            {

            }

            @Override
            public boolean hasAI()
            {
                return false;
            }

            @Override
            public void attack(@NotNull Entity target)
            {

            }

            @Override
            public void swingMainHand()
            {

            }

            @Override
            public void swingOffHand()
            {

            }

            @Override
            public boolean isCollidable()
            {
                return false;
            }

            @Override
            public void setCollidable(boolean collidable)
            {

            }

            @NotNull
            @Override
            public Set<UUID> getCollidableExemptions()
            {
                return null;
            }

            @Nullable
            @Override
            public <T> T getMemory(@NotNull MemoryKey<T> memoryKey)
            {
                return null;
            }

            @Override
            public <T> void setMemory(@NotNull MemoryKey<T> memoryKey, @Nullable T memoryValue)
            {

            }

            @NotNull
            @Override
            public EntityCategory getCategory()
            {
                return null;
            }

            @Override
            public boolean isInvisible()
            {
                return false;
            }

            @Override
            public void setInvisible(boolean invisible)
            {

            }

            @Nullable
            @Override
            public AttributeInstance getAttribute(@NotNull Attribute attribute)
            {
                return null;
            }

            @Override
            public void damage(double amount)
            {

            }

            @Override
            public void damage(double amount, @Nullable Entity source)
            {

            }

            @Override
            public double getHealth()
            {
                return 0;
            }

            @Override
            public void setHealth(double health)
            {

            }

            @Override
            public double getAbsorptionAmount()
            {
                return 0;
            }

            @Override
            public void setAbsorptionAmount(double amount)
            {

            }

            @Override
            public double getMaxHealth()
            {
                return 0;
            }

            @Override
            public void setMaxHealth(double health)
            {

            }

            @Override
            public void resetMaxHealth()
            {

            }

            @NotNull
            @Override
            public Location getLocation()
            {
                return null;
            }

            @Nullable
            @Override
            public Location getLocation(@Nullable Location loc)
            {
                return null;
            }

            @NotNull
            @Override
            public Vector getVelocity()
            {
                return null;
            }

            @Override
            public void setVelocity(@NotNull Vector velocity)
            {

            }

            @Override
            public double getHeight()
            {
                return 0;
            }

            @Override
            public double getWidth()
            {
                return 0;
            }

            @NotNull
            @Override
            public BoundingBox getBoundingBox()
            {
                return null;
            }

            @Override
            public boolean isInWater()
            {
                return false;
            }

            @NotNull
            @Override
            public World getWorld()
            {
                return null;
            }

            @Override
            public void setRotation(float yaw, float pitch)
            {

            }

            @Override
            public boolean teleport(@NotNull Location location)
            {
                return false;
            }

            @Override
            public boolean teleport(@NotNull Location location, @NotNull PlayerTeleportEvent.TeleportCause cause)
            {
                return false;
            }

            @Override
            public boolean teleport(@NotNull Entity destination)
            {
                return false;
            }

            @Override
            public boolean teleport(@NotNull Entity destination, @NotNull PlayerTeleportEvent.TeleportCause cause)
            {
                return false;
            }

            @NotNull
            @Override
            public List<Entity> getNearbyEntities(double x, double y, double z)
            {
                return null;
            }

            @Override
            public int getEntityId()
            {
                return 0;
            }

            @Override
            public int getFireTicks()
            {
                return 0;
            }

            @Override
            public void setFireTicks(int ticks)
            {

            }

            @Override
            public int getMaxFireTicks()
            {
                return 0;
            }

            @Override
            public void remove()
            {

            }

            @Override
            public boolean isDead()
            {
                return false;
            }

            @Override
            public boolean isValid()
            {
                return false;
            }

            @NotNull
            @Override
            public Server getServer()
            {
                return null;
            }

            @Override
            public boolean isPersistent()
            {
                return false;
            }

            @Override
            public void setPersistent(boolean persistent)
            {

            }

            @Nullable
            @Override
            public Entity getPassenger()
            {
                return null;
            }

            @Override
            public boolean setPassenger(@NotNull Entity passenger)
            {
                return false;
            }

            @NotNull
            @Override
            public List<Entity> getPassengers()
            {
                return null;
            }

            @Override
            public boolean addPassenger(@NotNull Entity passenger)
            {
                return false;
            }

            @Override
            public boolean removePassenger(@NotNull Entity passenger)
            {
                return false;
            }

            @Override
            public boolean isEmpty()
            {
                return false;
            }

            @Override
            public boolean eject()
            {
                return false;
            }

            @Override
            public float getFallDistance()
            {
                return 0;
            }

            @Override
            public void setFallDistance(float distance)
            {

            }

            @Nullable
            @Override
            public EntityDamageEvent getLastDamageCause()
            {
                return null;
            }

            @Override
            public void setLastDamageCause(@Nullable EntityDamageEvent event)
            {

            }

            @NotNull
            @Override
            public UUID getUniqueId()
            {
                return null;
            }

            @Override
            public int getTicksLived()
            {
                return 0;
            }

            @Override
            public void setTicksLived(int value)
            {

            }

            @Override
            public void playEffect(@NotNull EntityEffect type)
            {

            }

            @NotNull
            @Override
            public EntityType getType()
            {
                return null;
            }

            @Override
            public boolean isInsideVehicle()
            {
                return false;
            }

            @Override
            public boolean leaveVehicle()
            {
                return false;
            }

            @Nullable
            @Override
            public Entity getVehicle()
            {
                return null;
            }

            @Override
            public boolean isCustomNameVisible()
            {
                return false;
            }

            @Override
            public void setCustomNameVisible(boolean flag)
            {

            }

            @Override
            public boolean isGlowing()
            {
                return false;
            }

            @Override
            public void setGlowing(boolean flag)
            {

            }

            @Override
            public boolean isInvulnerable()
            {
                return false;
            }

            @Override
            public void setInvulnerable(boolean flag)
            {

            }

            @Override
            public boolean isSilent()
            {
                return false;
            }

            @Override
            public void setSilent(boolean flag)
            {

            }

            @Override
            public boolean hasGravity()
            {
                return false;
            }

            @Override
            public void setGravity(boolean gravity)
            {

            }

            @Override
            public int getPortalCooldown()
            {
                return 0;
            }

            @Override
            public void setPortalCooldown(int cooldown)
            {

            }

            @NotNull
            @Override
            public Set<String> getScoreboardTags()
            {
                return null;
            }

            @Override
            public boolean addScoreboardTag(@NotNull String tag)
            {
                return false;
            }

            @Override
            public boolean removeScoreboardTag(@NotNull String tag)
            {
                return false;
            }

            @NotNull
            @Override
            public PistonMoveReaction getPistonMoveReaction()
            {
                return null;
            }

            @NotNull
            @Override
            public BlockFace getFacing()
            {
                return null;
            }

            @NotNull
            @Override
            public Pose getPose()
            {
                return null;
            }

            @Nullable
            @Override
            public String getCustomName()
            {
                return null;
            }

            @Override
            public void setCustomName(@Nullable String name)
            {

            }

            @Override
            public void sendMessage(@NotNull String message)
            {

            }

            @Override
            public void sendMessage(@NotNull String[] messages)
            {

            }

            @Override
            public void sendMessage(@Nullable UUID sender, @NotNull String message)
            {

            }

            @Override
            public void sendMessage(@Nullable UUID sender, @NotNull String[] messages)
            {

            }

            @Override
            public void setMetadata(@NotNull String metadataKey, @NotNull MetadataValue newMetadataValue)
            {

            }

            @NotNull
            @Override
            public List<MetadataValue> getMetadata(@NotNull String metadataKey)
            {
                return null;
            }

            @Override
            public boolean hasMetadata(@NotNull String metadataKey)
            {
                return false;
            }

            @Override
            public void removeMetadata(@NotNull String metadataKey, @NotNull Plugin owningPlugin)
            {

            }

            @Override
            public boolean isPermissionSet(@NotNull String name)
            {
                return false;
            }

            @Override
            public boolean isPermissionSet(@NotNull Permission perm)
            {
                return false;
            }

            @Override
            public boolean hasPermission(@NotNull String name)
            {
                return false;
            }

            @Override
            public boolean hasPermission(@NotNull Permission perm)
            {
                return false;
            }

            @NotNull
            @Override
            public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value)
            {
                return null;
            }

            @NotNull
            @Override
            public PermissionAttachment addAttachment(@NotNull Plugin plugin)
            {
                return null;
            }

            @Nullable
            @Override
            public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks)
            {
                return null;
            }

            @Nullable
            @Override
            public PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks)
            {
                return null;
            }

            @Override
            public void removeAttachment(@NotNull PermissionAttachment attachment)
            {

            }

            @Override
            public void recalculatePermissions()
            {

            }

            @NotNull
            @Override
            public Set<PermissionAttachmentInfo> getEffectivePermissions()
            {
                return null;
            }

            @Override
            public boolean isOp()
            {
                return false;
            }

            @Override
            public void setOp(boolean value)
            {

            }

            @NotNull
            @Override
            public PersistentDataContainer getPersistentDataContainer()
            {
                return null;
            }

            @Override
            public void sendPluginMessage(@NotNull Plugin source, @NotNull String channel, @NotNull byte[] message)
            {

            }

            @NotNull
            @Override
            public Set<String> getListeningPluginChannels()
            {
                return null;
            }

            @NotNull
            @Override
            public <T extends Projectile> T launchProjectile(@NotNull Class<? extends T> projectile)
            {
                return null;
            }

            @NotNull
            @Override
            public <T extends Projectile> T launchProjectile(@NotNull Class<? extends T> projectile, @Nullable Vector velocity)
            {
                return null;
            }
        });
    }
}
