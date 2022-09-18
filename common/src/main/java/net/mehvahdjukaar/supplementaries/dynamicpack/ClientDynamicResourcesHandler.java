package net.mehvahdjukaar.supplementaries.dynamicpack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mehvahdjukaar.moonlight.api.events.AfterLanguageLoadEvent;
import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight.api.resources.RPUtils;
import net.mehvahdjukaar.moonlight.api.resources.ResType;
import net.mehvahdjukaar.moonlight.api.resources.StaticResource;
import net.mehvahdjukaar.moonlight.api.resources.assets.LangBuilder;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynClientResourcesProvider;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicTexturePack;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.Respriter;
import net.mehvahdjukaar.moonlight.api.resources.textures.SpriteUtils;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.mehvahdjukaar.moonlight.api.set.BlockType;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.client.WallLanternTexturesRegistry;
import net.mehvahdjukaar.supplementaries.configs.ClientConfigs;
import net.mehvahdjukaar.supplementaries.configs.RegistryConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;


public class ClientDynamicResourcesHandler extends DynClientResourcesProvider {

    public static final ClientDynamicResourcesHandler INSTANCE = new ClientDynamicResourcesHandler();

    public ClientDynamicResourcesHandler() {
        super(new DynamicTexturePack(Supplementaries.res("generated_pack")));
        this.dynamicPack.generateDebugResources = PlatformHelper.isDev() || RegistryConfigs.DEBUG_RESOURCES.get();
    }

    @Override
    public Logger getLogger() {
        return Supplementaries.LOGGER;
    }

    @Override
    public boolean dependsOnLoadedPacks() {
        return RegistryConfigs.PACK_DEPENDANT_ASSETS.get();
    }

    @Override
    public void generateStaticAssetsOnStartup(ResourceManager manager) {
        if (RegistryConfigs.ROPE_ARROW_ENABLED.get()) {
            this.dynamicPack.addNamespaces("minecraft");
        }

        this.dynamicPack.addItemModel(new ResourceLocation("crossbow_arrow"), JsonParser.parseString(
                """ 
                        {
                            "parent": "item/crossbow",
                            "textures": {
                                "layer0": "item/crossbow_arrow_base",
                                "layer1": "item/crossbow_arrow_tip"
                            }
                        }
                        """));
    }

    public void addHangingSignLoaderModel(StaticResource resource, String woodTextPath, String logTexture) {
        String string = new String(resource.data, StandardCharsets.UTF_8);

        string = string.replace("wood_type", woodTextPath);
        string = string.replace("log_texture", logTexture);

        //adds modified under my namespace
        ResourceLocation newRes = Supplementaries.res("hanging_signs/" + woodTextPath + "_loader");
        dynamicPack.addBytes(newRes, string.getBytes(), ResType.BLOCK_MODELS);
    }


    //-------------resource pack dependant textures-------------

    @Override
    public void regenerateDynamicAssets(ResourceManager manager) {


        RPUtils.addCrossbowModel(manager, this.dynamicPack, e -> {
            e.add(new ItemOverride(new ResourceLocation("item/crossbow_rope_arrow"),
                    List.of(new ItemOverride.Predicate(new ResourceLocation("charged"), 1f),
                            new ItemOverride.Predicate(Supplementaries.res("rope_arrow"), 1f))));
        });

        //need this here for reasons I forgot
        WallLanternTexturesRegistry.reloadTextures(manager);

        //models are dynamic too as packs can change them

        //------hanging signs------
        {
            StaticResource hsBlockState = StaticResource.getOrLog(manager,
                    ResType.BLOCKSTATES.getPath(Supplementaries.res("hanging_sign_oak")));
            StaticResource hsModel = StaticResource.getOrLog(manager,
                    ResType.BLOCK_MODELS.getPath(Supplementaries.res("hanging_signs/hanging_sign_oak")));
            StaticResource hsLoader = StaticResource.getOrLog(manager,
                    ResType.BLOCK_MODELS.getPath(Supplementaries.res("hanging_signs/loader_template")));
            StaticResource hsItemModel = StaticResource.getOrLog(manager,
                    ResType.ITEM_MODELS.getPath(Supplementaries.res("hanging_sign_oak")));

            ModRegistry.HANGING_SIGNS.forEach((wood, sign) -> {
                //if(wood.isVanilla())return;

                String id = Utils.getID(sign).getPath();


                try {
                    dynamicPack.addSimilarJsonResource(hsBlockState, "hanging_sign_oak", id);
                } catch (Exception ex) {
                    getLogger().error("Failed to generate Hanging Sign blockstate definition for {} : {}", sign, ex);
                }

                try {
                    dynamicPack.addSimilarJsonResource(hsModel, "hanging_sign_oak", id);
                } catch (Exception ex) {
                    getLogger().error("Failed to generate Hanging Sign block model for {} : {}", sign, ex);
                }

                try {
                    dynamicPack.addSimilarJsonResource(hsItemModel, "hanging_sign_oak", id);
                } catch (Exception ex) {
                    getLogger().error("Failed to generate Hanging Sign item model for {} : {}", sign, ex);
                }

                try {
                    ResourceLocation logTexture;
                    try {
                        logTexture = RPUtils.findFirstBlockTextureLocation(manager, wood.log, s -> !s.contains("top"));
                    } catch (Exception e1) {
                        logTexture = RPUtils.findFirstBlockTextureLocation(manager, wood.planks, s -> true);
                        getLogger().error("Could not properly generate Hanging Sign model for {}. Falling back to planks texture : {}", sign, e1);
                    }
                    addHangingSignLoaderModel(Objects.requireNonNull(hsLoader), id, logTexture.toString());
                } catch (Exception ex) {
                    getLogger().error("Failed to generate Hanging Sign loader model for {} : {}", sign, ex);
                }
            });

        }

        //textures


        //------sing posts-----
        {
            StaticResource spItemModel = StaticResource.getOrLog(manager,
                    ResType.ITEM_MODELS.getPath(Supplementaries.res("sign_post_oak")));

            ModRegistry.SIGN_POST_ITEMS.forEach((wood, sign) -> {
                //if (wood.isVanilla()) return;
                String id = Utils.getID(sign).getPath();
                //langBuilder.addEntry(sign, wood.getVariantReadableName("sign_post"));

                try {
                    dynamicPack.addSimilarJsonResource(spItemModel, "sign_post_oak", id);
                } catch (Exception ex) {
                    getLogger().error("Failed to generate Sign Post item model for {} : {}", sign, ex);
                }
            });
        }


        //hanging signs block textures
        try (TextureImage template = TextureImage.open(manager,
                Supplementaries.res("blocks/hanging_signs/hanging_sign_oak"));
             TextureImage mask = TextureImage.open(manager,
                     Supplementaries.res("blocks/hanging_signs/board_mask"))) {


            Respriter respriter = Respriter.masked(template, mask);

            ModRegistry.HANGING_SIGNS.forEach((wood, sign) -> {
                //if (wood.isVanilla()) continue;
                ResourceLocation textureRes = Supplementaries.res("blocks/hanging_signs/" + Utils.getID(sign).getPath());
                if (alreadyHasTextureAtLocation(manager, textureRes)) return;
                try (TextureImage plankTexture = TextureImage.open(manager,
                        RPUtils.findFirstBlockTextureLocation(manager, wood.planks))) {

                    List<Palette> targetPalette = SpriteUtils.extrapolateSignBlockPalette(plankTexture);
                    TextureImage newImage = respriter.recolorWithAnimation(targetPalette, plankTexture.getMetadata());

                    dynamicPack.addAndCloseTexture(textureRes, newImage);
                } catch (Exception ex) {
                    getLogger().error("Failed to generate Hanging Sign block texture for for {} : {}", sign, ex);
                }
            });
        } catch (Exception ex) {
            getLogger().error("Could not generate any Hanging Sign block texture : ", ex);
        }

        //hanging sign item textures
        try (TextureImage boardTemplate = TextureImage.open(manager,
                Supplementaries.res("items/hanging_signs/template"));
             TextureImage boardMask = TextureImage.open(manager,
                     Supplementaries.res("items/hanging_signs/board_mask"));
             TextureImage signMask = TextureImage.open(manager,
                     Supplementaries.res("items/hanging_signs/sign_board_mask"))) {

            Respriter respriter = Respriter.masked(boardTemplate, boardMask);

            ModRegistry.HANGING_SIGNS.forEach((wood, sign) -> {

                //if (wood.isVanilla()) continue;
                ResourceLocation textureRes = Supplementaries.res("items/hanging_signs/" + Utils.getID(sign).getPath());
                if (alreadyHasTextureAtLocation(manager, textureRes)) return;

                TextureImage newImage = null;
                Item vanillaSign = wood.getItemOfThis("sign");
                if (vanillaSign != null) {
                    try (TextureImage vanillaSignTexture = TextureImage.open(manager,
                            RPUtils.findFirstItemTextureLocation(manager, vanillaSign))) {

                        Palette targetPalette = Palette.fromImage(vanillaSignTexture, signMask);
                        newImage = respriter.recolor(targetPalette);

                        try (TextureImage scribbles = recolorFromVanilla(manager, vanillaSignTexture,
                                Supplementaries.res("items/hanging_signs/sign_scribbles_mask"),
                                Supplementaries.res("items/hanging_signs/scribbles_template"))) {
                            newImage.applyOverlay(scribbles);
                        } catch (Exception ex) {
                            getLogger().error("Could not properly color Hanging Sign texture for {} : {}", sign, ex);
                        }

                        try (TextureImage stick = recolorFromVanilla(manager, vanillaSignTexture,
                                Supplementaries.res("items/hanging_signs/sign_stick_mask"),
                                Supplementaries.res("items/hanging_signs/stick_template"))) {
                            newImage.applyOverlay(stick);
                        } catch (Exception ex) {
                            getLogger().error("Could not properly color Hanging Sign item texture for {} : {}", sign, ex);
                        }

                    } catch (Exception ex) {
                        //getLogger().error("Could not find sign texture for wood type {}. Using plank texture : {}", wood, ex);
                    }
                }
                //if it failed use plank one
                if (newImage == null) {
                    try (TextureImage plankPalette = TextureImage.open(manager,
                            RPUtils.findFirstBlockTextureLocation(manager, wood.planks))) {
                        Palette targetPalette = SpriteUtils.extrapolateWoodItemPalette(plankPalette);
                        newImage = respriter.recolor(targetPalette);
                    } catch (Exception ex) {
                        getLogger().error("Failed to generate Hanging Sign item texture for for {} : {}", sign, ex);
                    }
                }
                if (newImage != null) {
                    dynamicPack.addAndCloseTexture(textureRes, newImage);
                }
            });
        } catch (Exception ex) {
            getLogger().error("Could not generate any Hanging Sign item texture : ", ex);
        }

        //sign posts item textures
        try (TextureImage template = TextureImage.open(manager,
                Supplementaries.res("items/sign_posts/template"))) {

            Respriter respriter = Respriter.of(template);

            ModRegistry.SIGN_POST_ITEMS.forEach((wood, sign) -> {
                //if (wood.isVanilla()) continue;

                ResourceLocation textureRes = Supplementaries.res("items/sign_posts/" + Utils.getID(sign).getPath());

                if (alreadyHasTextureAtLocation(manager, textureRes)) return;

                TextureImage newImage = null;
                Item signItem = wood.getItemOfThis("sign");
                if (signItem != null) {
                    try (TextureImage vanillaSign = TextureImage.open(manager,
                            RPUtils.findFirstItemTextureLocation(manager, signItem));
                         TextureImage signMask = TextureImage.open(manager,
                                 Supplementaries.res("items/hanging_signs/sign_board_mask"))) {

                        List<Palette> targetPalette = Palette.fromAnimatedImage(vanillaSign, signMask);
                        newImage = respriter.recolor(targetPalette);

                        try (TextureImage scribbles = recolorFromVanilla(manager, vanillaSign,
                                Supplementaries.res("items/hanging_signs/sign_scribbles_mask"),
                                Supplementaries.res("items/sign_posts/scribbles_template"))) {
                            newImage.applyOverlay(scribbles);
                        } catch (Exception ex) {
                            getLogger().error("Could not properly color Sign Post item texture for {} : {}", sign, ex);
                        }

                    } catch (Exception ex) {
                        //getLogger().error("Could not find sign texture for wood type {}. Using plank texture : {}", wood, ex);
                    }
                }
                //if it failed use plank one
                if (newImage == null) {
                    try (TextureImage plankPalette = TextureImage.open(manager,
                            RPUtils.findFirstBlockTextureLocation(manager, wood.planks))) {
                        Palette targetPalette = SpriteUtils.extrapolateWoodItemPalette(plankPalette);
                        newImage = respriter.recolor(targetPalette);

                    } catch (Exception ex) {
                        getLogger().error("Failed to generate Sign Post item texture for for {} : {}", sign, ex);
                    }
                }
                if (newImage != null) {
                    dynamicPack.addAndCloseTexture(textureRes, newImage);
                }
            });
        } catch (Exception ex) {
            getLogger().error("Could not generate any Sign Post item texture : ", ex);
        }

        //sign posts block textures
        try (TextureImage template = TextureImage.open(manager,
                Supplementaries.res("entity/sign_posts/sign_post_oak"))) {

            Respriter respriter = Respriter.of(template);

            ModRegistry.SIGN_POST_ITEMS.forEach((wood, sign) -> {
                //if (wood.isVanilla()) continue;
                var textureRes = Supplementaries.res("entity/sign_posts/" + Utils.getID(sign).getPath());
                if (alreadyHasTextureAtLocation(manager, textureRes)) return;

                try (TextureImage plankTexture = TextureImage.open(manager,
                        RPUtils.findFirstBlockTextureLocation(manager, wood.planks))) {
                    Palette palette = Palette.fromImage(plankTexture);
                    TextureImage newImage = respriter.recolor(palette);

                    dynamicPack.addAndCloseTexture(textureRes, newImage);
                } catch (Exception ex) {
                    getLogger().error("Failed to generate Sign Post block texture for for {} : {}", sign, ex);
                }
            });
        } catch (Exception ex) {
            getLogger().error("Could not generate any Sign Post block texture : ", ex);
        }

    }


    @Deprecated(forRemoval = true)
    private static <B extends Block, T extends BlockType> void addCandleHolderStuff(ResourceManager manager, DynamicTexturePack pack) {


        LangBuilder builder = new LangBuilder();
        //remove
        ModRegistry.CANDLE_HOLDERS.forEach((w, v) -> {
            if (w != null)
                builder.addEntry(v.get(), LangBuilder.getReadableName(w.getSerializedName() + "_candle_holder"));
        });
        pack.addLang(Supplementaries.res("en-us"), builder);




        //hanging sign item textures
        try (TextureImage base = TextureImage.open(manager,
                Supplementaries.res("items/candle_holders/base"));
             TextureImage candleMask = TextureImage.open(manager,
                     Supplementaries.res("items/candle_holders/candle"));
             TextureImage mask = TextureImage.open(manager,
                     Supplementaries.res("items/candle_holders/base_m"))) {

            Respriter respriter = Respriter.masked(base, mask);

            ModRegistry.CANDLE_HOLDERS.forEach((wood, sign) -> {
                if (wood == null) return;
                ResourceLocation textureRes = Supplementaries.res("items/candle_holders/" +
                        wood.getSerializedName());


                TextureImage newImage = null;
                Item vanillaSign = Registry.ITEM.get(new ResourceLocation(wood.getSerializedName() + "_candle"));

                try (TextureImage vanillaSignTexture = TextureImage.open(manager,
                        RPUtils.findFirstItemTextureLocation(manager, vanillaSign))) {

                    Palette targetPalette = Palette.fromImage(vanillaSignTexture, candleMask);
                    newImage = respriter.recolor(targetPalette);
                } catch (Exception ex) {
                    //getLogger().error("Could not find sign texture for wood type {}. Using plank texture : {}", wood, ex);
                }

                if (newImage != null) {
                    pack.addAndCloseTexture(textureRes, newImage);
                }
            });
        } catch (Exception ex) {
            //getLogger().error("Could not generate any Hanging Sign item texture : ", ex);
        }


        //finds one entry. used so we can grab the oak equivalent
        var oakBlock = ModRegistry.CANDLE_HOLDERS.get(DyeColor.WHITE).get();

        ItemLike oi = oakBlock;


        ResourceLocation baseId = Utils.getID(oakBlock);


        BiFunction<String, DyeColor, String> modifier = (s, c) ->
                s.replace("candle_holders/", "&")
                        .replace("blocks/candle_holder", "%")
                        .replace("candle_holder_white", "$")
                        .replace("white_candle", c.getSerializedName() + "_candle")
                        .replace("&", "candle_holders/")
                        .replace("$", "candle_holder_" + c.getSerializedName())
                        .replace("%", "blocks/candle_holder")
                        .replace("white_ceiling_", c.getSerializedName() + "_ceiling_")
                        .replace("white_floor_", c.getSerializedName() + "_floor_")
                        .replace("base", c.getSerializedName())
                        .replace("caaa","candle_holder_"+c.getSerializedName())
                        .replace("white_wall_", c.getSerializedName() + "_wall_");


        Set<String> modelsLoc = new HashSet<>();

        Item oakItem = ModRegistry.CANDLE_HOLDERS.get(null).get().asItem();;


        //item model
        try {
            //we cant use this since it might override partent too. Custom textured items need a custom model added manually with addBlockResources
            // modelModifier.replaceItemType(baseBlockName);


            StaticResource oakItemModel = StaticResource.getOrFail(manager,
                    ResType.ITEM_MODELS.getPath(Supplementaries.res("caaa")));

            JsonObject json = RPUtils.deserializeJson(oakItemModel.getInputStream());
            //adds models referenced from here. not recursive
            modelsLoc.addAll(RPUtils.findAllResourcesInJsonRecursive(json, s -> s.equals("model") || s.equals("parent")));

            if (json.has("parent")) {
                String parent = json.get("parent").getAsString();
                if (parent.contains("item/generated")) {
                }
            }

            ModRegistry.CANDLE_HOLDERS.forEach((w, b) -> {
                try {
                    pack.addSimilarJsonResource(oakItemModel, (s) -> modifier.apply(s, w));
                } catch (Exception e) {
                }
            });
        } catch (Exception e) {
        }


        //blockstate
        try {
            StaticResource oakBlockstate = StaticResource.getOrFail(manager, ResType.BLOCKSTATES.getPath(baseId));

            //models
            JsonElement json = RPUtils.deserializeJson(oakBlockstate.getInputStream());

            modelsLoc.addAll(RPUtils.findAllResourcesInJsonRecursive(json, s -> s.equals("model")));
            List<StaticResource> oakModels = new ArrayList<>();

            for (var m : modelsLoc) {
                //remove the ones from mc namespace
                ResourceLocation modelRes = new ResourceLocation(m);
                if (!modelRes.getNamespace().equals("minecraft")) {
                    StaticResource model = StaticResource.getOrLog(manager, ResType.MODELS.getPath(m));
                    if (model != null) oakModels.add(model);
                }
            }

            ModRegistry.CANDLE_HOLDERS.forEach((w, b) -> {
                if (w == null || w == DyeColor.WHITE) return;
                ResourceLocation id = Utils.getID(b);
                try {
                    //creates blockstate
                    pack.addSimilarJsonResource(oakBlockstate, (s) -> modifier.apply(s, w));

                    //creates block model
                    for (StaticResource model : oakModels) {
                        try {
                            pack.addSimilarJsonResource(model, (s) -> modifier.apply(s, w));
                        } catch (Exception exception) {
                        }
                    }
                } catch (Exception e) {
                }
            });
        } catch (Exception e) {
        }

    }


    /**
     * helper method.
     * recolors the template image with the color grabbed from the given image restrained to its mask, if possible
     */
    @Nullable
    public static TextureImage recolorFromVanilla(ResourceManager manager, TextureImage vanillaTexture, ResourceLocation vanillaMask,
                                                  ResourceLocation templateTexture) {
        try (TextureImage scribbleMask = TextureImage.open(manager, vanillaMask);
             TextureImage template = TextureImage.open(manager, templateTexture)) {
            Respriter respriter = Respriter.of(template);
            Palette palette = Palette.fromImage(vanillaTexture, scribbleMask);
            return respriter.recolor(palette);
        } catch (Exception ignored) {
        }
        return null;
    }

    //TODO: invert scribble color if sign is darker than them

    @Override
    public void addDynamicTranslations(AfterLanguageLoadEvent lang) {
        ModRegistry.HANGING_SIGNS.forEach((type, block) ->
                LangBuilder.addDynamicEntry(lang, "block.supplementaries.hanging_sign", type, block));
        ModRegistry.SIGN_POST_ITEMS.forEach((type, item) ->
                LangBuilder.addDynamicEntry(lang, "item.supplementaries.sign_post", type, item));
    }

}
