package com.example.examplemod.birthdaycake;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;

public class BirthdayCakeRenderer implements BlockEntityRenderer<BirthdayCakeBlockEntity> {
    public BirthdayCakeRenderer(BlockEntityRendererProvider.Context context){
    }

    @Override
    public void render(BirthdayCakeBlockEntity cakeEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        //BlockEntityから左右のろうそく情報を取得
        ItemStack leftCandleStack=cakeEntity.getCandleLeft();
        ItemStack rightCandleStack=cakeEntity.getCandleRight();

        int skyLight=LightTexture.sky(packedLight);
        int maxLight= LightTexture.pack(15,skyLight);

        //左のろうそくを描画
        if(!leftCandleStack.isEmpty()){
            poseStack.pushPose();
            poseStack.scale(0.8F,0.8F,0.8F);
            poseStack.translate(0.85,1.25,0.55);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    leftCandleStack,
                    ItemTransforms.TransformType.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    bufferSource,
                    0
            );
            poseStack.popPose();
        }
        //右のろうそく描画
        if(!rightCandleStack.isEmpty()){
            poseStack.pushPose();
            poseStack.scale(0.8F,0.8F,0.8F);
            poseStack.translate(0.35,1.25,0.55);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    rightCandleStack,
                    ItemTransforms.TransformType.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    bufferSource,
                    0
            );
            poseStack.popPose();
        }
        if(cakeEntity.isLitLeft()){
            addFlameParticle(Objects.requireNonNull(cakeEntity.getLevel()),0.7,1.55,0.45,cakeEntity.getBlockPos());
        }
        if(cakeEntity.isLitRight()){
            addFlameParticle(Objects.requireNonNull(cakeEntity.getLevel()),0.3,1.55,0.45,cakeEntity.getBlockPos());
        }
    }

    //パーティクル生成のためのメソッド
    private void addFlameParticle(Level level, double xOffset, double yOffset, double zOffset, BlockPos pos){
        if(level.random.nextFloat()<0.15F){
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX()+xOffset,
                    pos.getY()+yOffset,
                    pos.getZ()+zOffset,
                    0.0D,0.0D,0.0D);
        }
    }
}
