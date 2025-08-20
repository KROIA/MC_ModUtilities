package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.elements.*;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TestScreen extends GuiScreen {


    private final TabElement tabElement;

    Plot.PlotData plotData1 = new Plot.PlotData();
    Plot.PlotData plotData2 = new Plot.PlotData();
    Plot.PlotData plotData3 = new Plot.PlotData();
    float time = 0;

    public TestScreen()
    {
        super(Component.translatable("TEST"));


        tabElement = new TabElement();
        tabElement.setEnableBackground(false);
        tabElement.setEnableOutline(false);

        Frame frame1 = new Frame();
        //Frame frame2 = new Frame();
        Frame frame3 = new Frame();

        Button tab1Button = new Button("Tab 1");
        //Button tab2Button = new Button("Tab 2");
        Plot plot = new Plot();
        plot.addPlotData(plotData1);
        plot.addPlotData(plotData2);
        plot.addPlotData(plotData3);
        plot.setYAxisValueConversion("%.2f");
        plot.setYRange(-1, 1);
        plotData1.color = 0xFFFF0000; // Blue
        plotData2.color = 0xFF0000FF; // Red
        plotData3.color = 0xFF00FF00; // Green

        Button tab3Button = new Button("Tab 3");

        tab1Button.setBounds(0, 0, 100, 20);
       // tab2Button.setBounds(20, 20, 100, 20);
        tab3Button.setBounds(40, 40, 100, 20);
        frame1.addChild(tab1Button);
        frame3.addChild(tab3Button);

        TextureElement icon = new TextureElement("minecraft", "textures/item/diamond.png", 16, 16);
        icon.setSize(17, 20);

        icon.setHoverTooltipSupplier(()->"This is a tooltip for the icon");
        icon.setHoverTooltipMousePositionAlignment(GuiElement.Alignment.LEFT);
        tabElement.addTab(icon, frame1);
        tabElement.addTab("Tab 2", plot);
        tabElement.setTitleElementHoverTooltipSupplier((index)->
        {
            return "This is tab " + (index+1) + " tooltip";
        });
        tabElement.setTitleElementHoverTooltipMouseAlignment(GuiElement.Alignment.TOP);
        tabElement.addTab("Tab 3", frame3);

        tab3Button.setOnFallingEdge(() -> {
            tabElement.reorderTab(1,2);
        });




        addElement(tabElement);
    }
    @Override
    protected void updateLayout(Gui gui) {
        tabElement.setBounds(0,0,getWidth(),getHeight());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        tabElement.setSelectedTitleHeight((int)((Math.sin(time*30)+1)*5 + 10));
        super.renderBackground(guiGraphics);

        plotData1.yValues.clear();
        plotData2.yValues.clear();
        plotData3.yValues.clear();
        for(int i=0; i<100; i++)
        {
            plotData1.yValues.add(i, ((float)Math.sin(time+i/10.0)));
            plotData2.yValues.add(i, ((float)Math.sin(time+i/10.0 + Math.PI*2/3)));
            plotData3.yValues.add(i, ((float)Math.sin(time+i/10.0 + Math.PI*4/3)));
        }
        time+= 0.01f;

    }
}
