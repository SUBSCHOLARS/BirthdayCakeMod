package com.example.examplemod.birthdaycake;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.CakeBlock.getOutputSignal;

public class BirthdayCake extends BaseEntityBlock implements EntityBlock{
    public static final IntegerProperty BITES = BlockStateProperties.BITES;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    protected static final VoxelShape[] SHAPE_BY_BITE = new VoxelShape[]
            {
                Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D),
                Block.box(3.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D),
                Block.box(5.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D),
                Block.box(7.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D),
                Block.box(9.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D),
                Block.box(11.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D),
                Block.box(13.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D)
            };
    public static final IntegerProperty CANDLES=IntegerProperty.create("candles",0,2);

    public BirthdayCake(){
       super(BlockBehaviour.Properties.of(Material.CAKE)
               .lightLevel(state->state.getValue(LIT) ? 15 : 0));
       //デフォルトの状態の設定
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(BITES,0)
                .setValue(CANDLES,0)
                .setValue(LIT,false));
    }

    public @NotNull VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE_BY_BITE[pState.getValue(BITES)];
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if(itemstack.getItem() instanceof NumberCandleItem){
            int candleCount=pState.getValue(CANDLES);

            //BlockEntityを取得
            BlockEntity blockEntity=pLevel.getBlockEntity(pPos);
            if(blockEntity instanceof BirthdayCakeBlockEntity cakeBlockEntity){
                if(candleCount<2){
                    //1本目（左側）
                    if(candleCount==0){
                        cakeBlockEntity.setCandleLeft(itemstack);
                        pLevel.setBlock(pPos,pState.setValue(CANDLES,1),3);
                    }
                    //2本目（右側）
                    else{
                        cakeBlockEntity.setCandleRight(itemstack);
                        pLevel.setBlock(pPos,pState.setValue(CANDLES,2),3);
                    }
                    //アイテム消費と音の演出
                    if(!pPlayer.isCreative()){
                        itemstack.shrink(1);
                    }
                    pLevel.playSound(null,pPos,SoundEvents.CAKE_ADD_CANDLE,SoundSource.BLOCKS,1.0F,1.0F);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        //手に持っているアイテムが火打石と打鉄であった場合
        if(itemstack.is(Items.FLINT_AND_STEEL)){
            if(!pLevel.isClientSide){
                BlockEntity blockEntity=pLevel.getBlockEntity(pPos);
                if(blockEntity instanceof BirthdayCakeBlockEntity cakeEntity){
                    if(!cakeEntity.isLitLeft() || !cakeEntity.isLitRight()){
                        pLevel.playSound(null, pPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,1.0F,1.0F);
                        cakeEntity.setLit(true,true);
                        pLevel.setBlock(pPos,pState.setValue(LIT,true),3);
                        itemstack.hurtAndBreak(1,pPlayer,(player) -> player.broadcastBreakEvent(pHand));
                        return InteractionResult.SUCCESS;
                    }
                }
            }else{
                pPlayer.displayClientMessage(new TextComponent("誕生日おめでとう！！"),true);
            }
            return InteractionResult.PASS;
        }

        if (pLevel.isClientSide) {
            if (eat(pLevel, pPos, pState, pPlayer).consumesAction()) {
                return InteractionResult.SUCCESS;
            }

            if (itemstack.isEmpty()) {
                return InteractionResult.CONSUME;
            }
        }

        return eat(pLevel, pPos, pState, pPlayer);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType,ExampleMod.BIRTHDAY_CAKE_ENTITY,BirthdayCakeBlockEntity::tick);
    }

    protected static InteractionResult eat(LevelAccessor pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        if (!pPlayer.canEat(false)) {
            return InteractionResult.PASS;
        } else {
            pPlayer.awardStat(Stats.EAT_CAKE_SLICE);
            pPlayer.getFoodData().eat(2, 0.1F);
            int i = pState.getValue(BITES);
            pLevel.gameEvent(pPlayer, GameEvent.EAT, pPos);
            if (i < 5) {
                pLevel.setBlock(pPos, pState.setValue(BITES, Integer.valueOf(i + 1)), 3);
            } else {
                pLevel.removeBlock(pPos, false);
                pLevel.gameEvent(pPlayer, GameEvent.BLOCK_DESTROY, pPos);
            }

            return InteractionResult.SUCCESS;
        }
    }
    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        return pFacing == Direction.DOWN && !pState.canSurvive(pLevel, pCurrentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return pLevel.getBlockState(pPos.below()).getMaterial().isSolid();
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(BITES, CANDLES, LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BirthdayCakeBlockEntity(pPos,pState);
    }
}
