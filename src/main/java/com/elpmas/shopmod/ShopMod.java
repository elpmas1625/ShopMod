package com.elpmas.shopmod;

// TODO: TODO練習
import com.elpmas.shopmod.item.ShopItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils; // ロギングのためのインポート
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import com.mojang.blaze3d.vertex.PoseStack; // PoseStackのインポート
import java.util.Optional;
import net.minecraftforge.client.event.RenderGuiEvent;  // これを追加
import net.minecraftforge.eventbus.api.Event;

import static com.robertx22.mine_and_slash.mmorpg.registers.common.SlashItemTags.REGULAR_GEMS;


@Mod(ShopMod.MOD_ID)
public class ShopMod {
    public static final String MOD_ID = "shopmod";
    private static final Logger LOGGER = LogUtils.getLogger();


    public ShopMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // アイテムレジストリをイベントバスに登録
        ShopItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        // money
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
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

            MoneyEvents.money = MoneyEvents.money -10;
            LOGGER.info("Player {} money: {}円", player.getName().getString(), MoneyEvents.money);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class MoneyEvents {

        private static int money = 1000;

        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            Player player = event.getEntity();
            // プレイヤーがログインした時の処理（必要に応じてカスタマイズ）
            LOGGER.info("Player {} has joined with money: {}", player.getName().getString(), money);
        }

        @SubscribeEvent
        public static void onDrawScreen(ScreenEvent.Render.Post event) {
            if (event.getScreen() instanceof InventoryScreen) {
                Minecraft mc = Minecraft.getInstance();
                GuiGraphics guiGraphics = event.getGuiGraphics();
                int width = event.getScreen().width;
                int height = event.getScreen().height;

                String text = "所持金：" + money;
                int textWidth = mc.font.width(text);
                int x = 10;
                int y = height / 2 - 50;

                guiGraphics.drawString(mc.font, text, x, y, 0xFFFFFF);
            }
        }
    }
}
