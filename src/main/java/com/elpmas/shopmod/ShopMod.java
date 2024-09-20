package com.elpmas.shopmod;

// TODO: TODO練習
import com.elpmas.shopmod.item.ShopItems;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashItemTags;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.robertx22.mine_and_slash.mmorpg.registers.common.SlashItemTags.REGULAR_GEMS;


@Mod(ShopMod.MOD_ID)
public class ShopMod {
    public static final String MOD_ID = "shopmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("shopmod", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static Map<UUID, Long> playerMoneyMap = new HashMap<>();
    private static final String MONEY_FILE = "player_moneys.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    public ShopMod() {
        MinecraftForge.EVENT_BUS.register(this);
        INSTANCE.registerMessage(0, MoneyUpdatePacket.class, MoneyUpdatePacket::encode, MoneyUpdatePacket::decode, MoneyUpdatePacket::handle);

        configPath = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
        configPath.toFile().mkdirs();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // アイテムレジストリをイベントバスに登録
        ShopItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        // money
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        loadPlayerMoney();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        savePlayerMoney();
    }

    private void loadPlayerMoney() {
        File file = configPath.resolve(MONEY_FILE).toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<UUID, Long>>(){}.getType();
                playerMoneyMap = GSON.fromJson(reader, type);
                LOGGER.info("Loaded player money data from {}", file.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to load player money data", e);
            }
        } else {
            LOGGER.info("No existing player money data found at {}", file.getAbsolutePath());
        }
    }

    private void savePlayerMoney() {
        File file = configPath.resolve(MONEY_FILE).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(playerMoneyMap, writer);
            LOGGER.info("Saved player money data to {}", file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save player money data", e);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        LOGGER.info("Player logged in: {}", event.getEntity().getName().getString());
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            long money = playerMoneyMap.getOrDefault(player.getUUID(), 0L);
            LOGGER.info("Sending money update to player: {}", money);
            INSTANCE.sendTo(new MoneyUpdatePacket(money), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    public static void setPlayerMoney(Player player, long amount) {
        playerMoneyMap.put(player.getUUID(), amount);
        if (player instanceof ServerPlayer) {
            INSTANCE.sendTo(new MoneyUpdatePacket(amount), ((ServerPlayer) player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    public static long getPlayerMoney(Player player) {
        return playerMoneyMap.getOrDefault(player.getUUID(), 0L);
    }

    public static class MoneyUpdatePacket {
        private final long money;

        public MoneyUpdatePacket(long money) {
            this.money = money;
        }

        public static void encode(MoneyUpdatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeLong(packet.money);
        }

        public static MoneyUpdatePacket decode(FriendlyByteBuf buffer) {
            return new MoneyUpdatePacket(buffer.readLong());
        }

        public static void handle(MoneyUpdatePacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                Player player = ctx.get().getSender();
                if (player != null) {
                    playerMoneyMap.put(player.getUUID(), packet.money);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            ItemStack oakLog = new ItemStack(Items.OAK_LOG, 1);
            if (!player.getInventory().add(oakLog)) {
                player.drop(oakLog, false);
            }
            ItemStack moditem = new ItemStack(ShopItems.ELPMAS_ITEM.get(), 1);
            if (!player.getInventory().add(moditem)) {
                player.drop(moditem, false);
            }

            // MinecraftのRandomSourceを使ってランダムなアイテムを取得する
            Optional<Item> randomGem = ForgeRegistries.ITEMS.tags().getTag(REGULAR_GEMS)
                    .getRandomElement(RandomSource.create());

            if (randomGem.isPresent()) {
                ItemStack mmorpg_ring = new ItemStack(randomGem.get(), 1);
                if (!player.getInventory().add(mmorpg_ring)) {
                    player.drop(mmorpg_ring, false);
                }
            }

            LOGGER.info(player.getStringUUID());
            LOGGER.info(String.valueOf(player.getUUID()));
            LOGGER.info("Player {} money: {}円", player.getName().getString(), getPlayerMoney(player));
            setPlayerMoney(player, 1000L);
        }
    }
}
