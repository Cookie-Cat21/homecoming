package dev.homecoming.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Retains vanilla keyboard/narration behavior with the journal's own control treatment. */
final class JournalButton extends Button {
    enum Kind { NORMAL, PRIMARY, TAB, DANGER }
    private final JournalIcon icon;
    private final Kind kind;
    private final boolean selected;
    JournalButton(int x,int y,int width,String label,JournalIcon icon,Kind kind,boolean selected,OnPress press) {
        super(x,y,width,20,Component.literal(label),press,DEFAULT_NARRATION);
        this.icon=icon; this.kind=kind; this.selected=selected;
    }
    JournalButton hint(String text) { setTooltip(Tooltip.create(Component.literal(text))); return this; }
    @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        boolean strong=kind==Kind.PRIMARY || selected;
        int background=strong?JournalStyle.SPRUCE:JournalStyle.CONTROL;
        if(active && isHoveredOrFocused()) background=strong?JournalStyle.LEAF:JournalStyle.HOVER;
        int foreground=!active?JournalStyle.DISABLED:strong?JournalStyle.PAGE:kind==Kind.DANGER?JournalStyle.DANGER:JournalStyle.INK;
        graphics.fill(getX(),getY(),getX()+width,getY()+height,background);
        graphics.fill(getX(),getY()+height-1,getX()+width,getY()+height,selected?JournalStyle.COPPER:JournalStyle.LINE);
        if(isFocused()) graphics.renderOutline(getX()-1,getY()-1,width+2,height+2,JournalStyle.COPPER);
        var font=Minecraft.getInstance().font;
        if(kind==Kind.TAB && icon!=null) {
            int groupWidth=16+font.width(getMessage());
            int groupLeft=getX()+Math.max(7,(width-groupWidth)/2);
            icon.draw(graphics,groupLeft,getY()+5,foreground);
            graphics.drawString(font,getMessage(),groupLeft+16,getY()+6,foreground,false);
            return;
        }
        if(icon!=null) icon.draw(graphics,getX()+7,getY()+5,foreground);
        // Vanilla scrolling labels always add a black shadow. Dark ink on paper must
        // be drawn without it, otherwise adjacent pixel strokes merge together.
        int textLeft=getX()+(icon==null?7:23), textRight=getX()+width-7;
        int available=textRight-textLeft, textWidth=font.width(getMessage());
        if(textWidth<=available) graphics.drawString(font,getMessage(),textLeft+(available-textWidth)/2,getY()+6,foreground,false);
        else {
            int overflow=textWidth-available;
            double seconds=net.minecraft.Util.getMillis()/1000.0;
            int offset=(int)((Math.sin(seconds*Math.PI/Math.max(3,overflow/24.0)-Math.PI/2)+1)*overflow/2);
            graphics.enableScissor(textLeft,getY(),textRight,getY()+height);
            graphics.drawString(font,getMessage(),textLeft-offset,getY()+6,foreground,false);
            graphics.disableScissor();
        }
    }
}
