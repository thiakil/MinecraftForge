package net.minecraftforge.debug;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;
import org.apache.logging.log4j.LogManager;

@Mod(modid = ObjectHolderTest.MODID, name = "ObjectHolderTests", version = "1.0", acceptableRemoteVersions = "*")
public class ObjectHolderTest
{
    public static final String MODID = "objectholdertest";
    final static boolean TEST_VANILLA_REPLACEMENT_BLOCKS = true;//whether to register and test a vanilla override changing the value in net.minecraft.init.Blocks

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        //verifies @ObjectHolder with custom id
        assert VanillaObjectHolder.uiButtonClick != null;
        //verifies modded items work
        assert ForgeObjectHolder.forge_potion != null;
        //verifies minecraft:air is now resolvable
        assert VanillaObjectHolder.air != null;
        //verifies unexpected name should not have defaulted to AIR.
        assert VanillaObjectHolder.nonExistentBlock == null;
        //verifies custom registries
        assert CustomRegistryObjectHolder.custom_entry != null;
        //verifies interfaces are supported
        assert CustomRegistryObjectHolder.custom_entry_by_interface != null;

        if (TEST_VANILLA_REPLACEMENT_BLOCKS)
        {
            if (Blocks.STONE instanceof VanillaOverride)
                LogManager.getLogger(MODID).info("Vanilla replacement succeeded");
            else
                throw new RuntimeException("Vanilla replacement objectholder failed");
        }
    }

    protected static class PotionForge extends Potion
    {
        protected PotionForge(ResourceLocation location, boolean badEffect, int potionColor)
        {
            super(badEffect, potionColor);
            setPotionName("potion." + location.getResourcePath());
            setRegistryName(location);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class Registration
    {
        @SubscribeEvent
        public static void newRegistry(RegistryEvent.NewRegistry event)
        {
            new RegistryBuilder<ICustomRegistryEntry>()
                    .setType(ICustomRegistryEntry.class)
                    .setName(new ResourceLocation("object_holder_test_custom_registry"))
                    .setIDRange(0, 255)
                    .create();
        }

        @SubscribeEvent
        public static void registerPotions(RegistryEvent.Register<Potion> event)
        {
            event.getRegistry().register(
                new ObjectHolderTest.PotionForge(new ResourceLocation(ObjectHolderTest.MODID, "forge_potion"), false, 0xff00ff) // test automatic id distribution
            );
        }
        
        @SubscribeEvent
        public static void registerVanillaOverride(RegistryEvent.Register<Block> event)
        {
            if (TEST_VANILLA_REPLACEMENT_BLOCKS)
            {
                event.getRegistry().register(new VanillaOverride().setRegistryName(Blocks.STONE.getRegistryName()));
            }
        }

        @SubscribeEvent
        public static void registerInterfaceRegistryForge(RegistryEvent.Register<ICustomRegistryEntry> event)
        {
            event.getRegistry().register(
                new CustomRegistryEntry().setRegistryName(new ResourceLocation(MODID, "custom_entry_by_interface"))
            );

            event.getRegistry().register(
                new CustomRegistryEntry().setRegistryName(new ResourceLocation(MODID, "custom_entry"))
            );
        }
    }
    interface ICustomRegistryEntry extends IForgeRegistryEntry<ICustomRegistryEntry>{}


    static class CustomRegistryEntry implements ICustomRegistryEntry
    {
        private ResourceLocation name;

        @Override
        public ICustomRegistryEntry setRegistryName(ResourceLocation name)
        {
            this.name = name;
            return this;
        }

        @Override
        public ResourceLocation getRegistryName()
        {
            return name;
        }

        @Override
        public Class<ICustomRegistryEntry> getRegistryType()
        {
            return ICustomRegistryEntry.class;
        }
    }

    @GameRegistry.ObjectHolder("minecraft")
    static class VanillaObjectHolder
    {
        //Tests importing vanilla objects that need the @ObjectHolder annotation to get a valid ResourceLocation
        @GameRegistry.ObjectHolder("ui.button.click")
        public static final SoundEvent uiButtonClick = null;
        public static final Block air = null;
        public static final Block nonExistentBlock = null;
    }

    @GameRegistry.ObjectHolder(ObjectHolderTest.MODID)
    static class ForgeObjectHolder
    {
        //Tests using subclasses for injections
        public static final ObjectHolderTest.PotionForge forge_potion = null;
    }

    @GameRegistry.ObjectHolder(ObjectHolderTest.MODID)
    static class CustomRegistryObjectHolder
    {
        //Tests whether custom registries can be used
        public static final ICustomRegistryEntry custom_entry = null;

        //Tests whether interfaces can be used
        public static final ICustomRegistryEntry custom_entry_by_interface = null;
    }
    
    static class VanillaOverride extends BlockStone{
    }
}
