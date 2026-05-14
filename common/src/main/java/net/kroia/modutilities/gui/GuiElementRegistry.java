package net.kroia.modutilities.gui;

import net.kroia.modutilities.gui.elements.*;
import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class GuiElementRegistry {

    private static final Map<String, Supplier<? extends GuiElement>> factories = new HashMap<>();
    private static final Map<Class<? extends GuiElement>, String> typeKeys = new HashMap<>();

    static {
        register("label", Label.class, Label::new);
        register("textbox", TextBox.class, TextBox::new);
        register("checkbox", CheckBox.class, () -> new CheckBox(""));
        register("empty_button", EmptyButton.class, EmptyButton::new);
        register("button", Button.class, () -> new Button(""));
        register("close_button", CloseButton.class, () -> new CloseButton(() -> {}));
        register("horizontal_slider", HorizontalSlider.class, HorizontalSlider::new);
        register("vertical_slider", VerticalSlider.class, VerticalSlider::new);
        register("frame", Frame.class, Frame::new);
        register("plot", Plot.class, Plot::new);
        register("texture_element", TextureElement.class, TextureElement::new);
        register("tab_element", TabElement.class, TabElement::new);
        register("dropdown_menu", DropDownMenu.class, () -> new DropDownMenu(""));
        register("item_view", ItemView.class, ItemView::new);
        register("horizontal_list_view", HorizontalListView.class, HorizontalListView::new);
        register("vertical_list_view", VerticalListView.class, VerticalListView::new);
    }

    public static <T extends GuiElement> void register(String key, Class<T> clazz, Supplier<T> factory) {
        factories.put(key, factory);
        typeKeys.put(clazz, key);
    }

    public static GuiElement create(String key) {
        Supplier<? extends GuiElement> factory = factories.get(key);
        if (factory == null) return null;
        return factory.get();
    }

    public static String getTypeKey(GuiElement element) {
        if (element == null) return null;
        return typeKeys.get(element.getClass());
    }

    public static String getTypeKey(Class<? extends GuiElement> clazz) {
        return typeKeys.get(clazz);
    }

    public static boolean isRegistered(String key) {
        return factories.containsKey(key);
    }
}
