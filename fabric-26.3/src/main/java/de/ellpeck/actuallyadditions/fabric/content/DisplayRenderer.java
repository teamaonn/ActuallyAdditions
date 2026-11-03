package de.ellpeck.actuallyadditions.fabric.content;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
public final class DisplayRenderer implements BlockEntityRenderer<WorkEntity,DisplayRenderer.State> {
 public static final class State extends BlockEntityRenderState {final ItemStackRenderState item=new ItemStackRenderState();float rotation;}
 private final ItemModelResolver resolver;public DisplayRenderer(BlockEntityRendererProvider.Context context){resolver=context.itemModelResolver();}
 public State createRenderState(){return new State();}
 public void extractRenderState(WorkEntity e,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderer.super.extractRenderState(e,state,partial,camera,overlay);boolean display=e.kind()==WorkKind.DISPLAY||e.kind()==WorkKind.EMPOWERER||e.kind()==WorkKind.BATTERY_BOX;resolver.updateForTopItem(state.item,display?e.getItem(0):ItemStack.EMPTY,ItemDisplayContext.GROUND,e.getLevel(),null,(int)e.getBlockPos().asLong());state.rotation=e.getLevel()==null?0:(e.getLevel().getGameTime()+partial)*2;}
 public void submit(State s,PoseStack pose,SubmitNodeCollector nodes,CameraRenderState camera){if(s.item.isEmpty())return;pose.pushPose();pose.translate(.5F,1.15F,.5F);pose.rotateDegrees(Axis.YP,s.rotation);pose.scale(.8F,.8F,.8F);s.item.submit(pose,nodes,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
}
