package strangeguyperson.life;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.LevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LIFESMP implements ModInitializer {
	public static final String MOD_ID = "life";

	public static final AttachmentType<Integer> LIVES = AttachmentRegistry.create(
			id("lives"),
			builder -> builder
					.initializer(() -> 3)
					.persistent(Codec.INT)
					.copyOnDeath()
	);

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerLivingEntityEvents.AFTER_DEATH.register((livingEntity, damageSource) -> {
			if (livingEntity instanceof ServerPlayer serverPlayer) {
				Integer lives = serverPlayer.getAttached(LIFESMP.LIVES);
				int currentLives = (lives != null) ? lives : 3;
				int newLives = currentLives - 1;
				if (newLives <= 0){
					LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(serverPlayer.level(), EntitySpawnReason.TRIGGERED);
					if(lightningBolt != null){
						lightningBolt.setPos(serverPlayer.position());
						lightningBolt.setVisualOnly(true);
						serverPlayer.level().addFreshEntity(lightningBolt);
					}
					serverPlayer.setGameMode(GameType.SPECTATOR);
					Component eliminatedTitle = Component.literal("You ran out of lives! ").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.RED).withBold(true));
					Component globalTitle = serverPlayer.getDisplayName();
					Component globalSubtitle = Component.literal("ran out of lives! ").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.WHITE));
					for (ServerPlayer onlinePlayer : serverPlayer.level().getServer().getPlayerList().getPlayers()) {
						onlinePlayer.connection.send(new ClientboundSoundPacket(
								BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.LIGHTNING_BOLT_THUNDER),
								SoundSource.PLAYERS, onlinePlayer.getX(), onlinePlayer.getY(), onlinePlayer.getZ(), 9999.0f, 1.0f, serverPlayer.level().getRandom().nextLong()
						));
						onlinePlayer.connection.send(new ClientboundSoundPacket(
								BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.LIGHTNING_BOLT_IMPACT),
								SoundSource.PLAYERS, onlinePlayer.getX(), onlinePlayer.getY(), onlinePlayer.getZ(), 9999.0f, 1.0f, serverPlayer.level().getRandom().nextLong()
						));
						if (onlinePlayer == serverPlayer) {
							onlinePlayer.connection.send(new ClientboundSetTitleTextPacket(eliminatedTitle));
						} else {
							onlinePlayer.connection.send(new ClientboundSetTitleTextPacket(globalTitle));
							onlinePlayer.connection.send(new ClientboundSetSubtitleTextPacket(globalSubtitle));
						}
					}
				}
				serverPlayer.setAttached(LIFESMP.LIVES, Math.max(newLives,0));
			}
		});
		LOGGER.info("LIFESMP initializing.. ");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}