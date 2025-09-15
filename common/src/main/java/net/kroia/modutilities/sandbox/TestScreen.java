package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.ColorUtilities;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiContainerScreen;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.GuiTexture;
import net.kroia.modutilities.gui.elements.*;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;
import net.kroia.modutilities.gui.elements.base.Slider;
import net.kroia.modutilities.gui.layout.Layout;
import net.kroia.modutilities.gui.layout.LayoutGrid;
import net.kroia.modutilities.gui.layout.LayoutVertical;
import net.kroia.modutilities.networking.streaming.StreamSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.UUID;

public class TestScreen extends GuiScreen {


   /* private final TabElement tabElement;

    Plot.PlotData plotData1 = new Plot.PlotData();
    Plot.PlotData plotData2 = new Plot.PlotData();
    Plot.PlotData plotData3 = new Plot.PlotData();
    float time = 0;

    private final UUID sinusStreamID;*/
class MyElement extends GuiElement
{
    private final ListView listView;
    float time = 0;
    public MyElement()
    {
        listView = new VerticalListView();
        LayoutGrid layout = new LayoutGrid();
        layout.stretchX = true;
        layout.columns = 3;
        listView.setLayout(layout);

        for(int i = 0; i < 10; i++)
        {
            Label label = new Label("Label " + i);
            label.setHeight(20);
            listView.addChild(label);
        }

        addChild(listView);
    }
    @Override
    protected void render()
    {
        time += 0.1f;
        listView.setScrollbarThickness((int)(Math.sin(time * 2) * 10 + 10));

    }
    @Override
    protected void layoutChanged() {
        int width = getWidth();
        int height = getHeight();

        listView.setBounds(width/4, height/4, width/2, height/2);
    }

    private void onButtonFallingEdge()
    {
        ModUtilitiesMod.LOGGER.info("[UI] Button clicked!");
    }
    private void onButtonRisingEdge()
    {
        ModUtilitiesMod.LOGGER.info("[UI] Button released!");
    }
    private void onButtonHeld()
    {
        ModUtilitiesMod.LOGGER.info("[UI] Button held down!");
    }
}
    //private final Button myButton;
    //private final MyElement myElement;
    //private final DropDownMenu dropDownMenu;
    //private final ItemSelectionView itemSelectionView;
    //private final ContainerView<MyContainerMenu> inventoryView;

    public TestScreen()
    //public TestScreen(MyContainerMenu pMenu, Inventory pPlayerInventory, Component pTitle)
    {
        //super(pMenu, pPlayerInventory, pTitle);
        super(Component.translatable("TEST"));

        //myButton = new Button("Click me!");
        //inventoryView = new ContainerView<>(pMenu, pPlayerInventory, Component.literal("Inventory"), new GuiTexture(ModUtilitiesMod.MOD_ID,
        //        "textures/gui/inventory_hpc.png", 256, 256));
        //inventoryView.setSize(176, 166);

      //  addElement(myButton);


        //myElement = new MyElement();
        //addElement(myElement);


       /* dropDownMenu = new DropDownMenu("Dropdown");
        dropDownMenu.addOption("Option 1");
        dropDownMenu.addOption("Option 2");
        dropDownMenu.addOption("Option 3");
        dropDownMenu.addOption("Option 4");
        dropDownMenu.addOption("Option 5");
        dropDownMenu.addOption("Option 6");

        dropDownMenu.setOnOptionSelected((index, element) -> {
            ModUtilitiesMod.LOGGER.info("[UI] Selected option: " + index + " - " + element.toString());
        });
        addElement(dropDownMenu);*/

        int y=50;
        for(int i=0; i<=GuiElement.Alignment.BOTTOM_RIGHT.ordinal(); ++i)
        {
            Label label1 = new Label("Label "+i);
            int finalI = i;
            label1.setHoverTooltipSupplier(()->"This is a tooltip for label "+ finalI);
            GuiElement.Alignment alignment = GuiElement.Alignment.values()[i];
            label1.setHoverTooltipMousePositionAlignment(alignment);
            label1.setBounds(200,y,100,15);
            y += 15;
            addElement(label1);
        }

        //itemSelectionView = new ItemSelectionView((i)->{});

        //addElement(itemSelectionView);

        /*tabElement = new TabElement();
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


        sinusStreamID = StreamSystem.startServerToClientStream(Sandbox.SandboxNetwork.SINUS_STREAM, 0.0f,
                (value)-> // Callback handler for stream data
                {
                    plotData1.yValues.add((float)value.doubleValue());
                    if(plotData1.yValues.size() > 100)
                    {
                        plotData1.yValues.remove(0);
                    }
                },
                ()-> // Callback handler for stream stopped event
                {
                    // Stream stopped handler
                    ModUtilitiesMod.LOGGER.info("[UI] SineStream stopped");
                });



        addElement(tabElement);*/
    }

    public static void open()
    {
        GuiScreen.setScreen(new TestScreen());
    }
    @Override
    public void onClose() {
        super.onClose();
       // StreamSystem.stopStream(sinusStreamID);
    }

    @Override
    protected void updateLayout(Gui gui) {
        //tabElement.setBounds(0,0,getWidth(),getHeight());

        //myButton.setBounds(10, 10, 100, 20);
//
        //myElement.setBounds(10, 10, 300, 100);

        //dropDownMenu.setBounds(10, 10, 150, 20);
        //itemSelectionView.setBounds(10, 10, getWidth()/2, 100);
    }

    /*@Override
    public void renderBackground(GuiGraphics guiGraphics) {

        tabElement.setSelectedTitleHeight((int)((Math.sin(time*30)+1)*5 + 10));
        super.renderBackground(guiGraphics);

        //plotData1.yValues.clear();
        plotData2.yValues.clear();
        plotData3.yValues.clear();
        for(int i=0; i<100; i++)
        {
            //plotData1.yValues.add(i, ((float)Math.sin(time+i/10.0)));
            plotData2.yValues.add(i, ((float)Math.sin(time+i/10.0 + Math.PI*2/3)));
            plotData3.yValues.add(i, ((float)Math.sin(time+i/10.0 + Math.PI*4/3)));
        }
        time+= 0.01f;

    }*/

    public void onSinusSignalArrived(Double value)
    {

    }
}
