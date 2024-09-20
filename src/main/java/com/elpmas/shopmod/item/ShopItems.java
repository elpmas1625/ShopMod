package com.elpmas.shopmod.item;

import com.elpmas.shopmod.ShopMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ShopItems {

    // レジストリ作成
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ShopMod.MOD_ID);

    // レジストリにアイテムを登録
    public static final RegistryObject<Item> ELPMAS_ITEM = ITEMS.register("elpmas_item", () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus){
        // レジストリをイベントバスに登録
        ITEMS.register(eventBus);
    }

}
