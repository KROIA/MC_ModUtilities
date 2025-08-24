# Button

## Features
* Detects mouse clicks
* The trigger button can be specified
* Callbacks can be triggered
  * on the falling edge of the mouse button.
  * on the rising edge of the mouse button.
  * when the button is pressed and held.
* The colors for the different states can be changes:
* The button can be made clickable or not (disabeling without disabeling the rendering)


---
### Usage
``` Java
public class MyElement extends GuiElement {
    private final Button myButton;
    public MyElement()
    {
        myButton = new Button("Click me!");
        myButton.setOnFallingEdge(this::onButtonFallingEdge);
        myButton.setOnRisingEdge(this::onButtonRisingEdge);
        myButton.setOnDown(this::onButtonHeld);
        addChild(myButton); // add it to the screen
    }

    @Override
    protected void layoutChanged() {
        myButton.setBounds(10, 10, 100, 20); // set the location and size of the button
    }

    private void onButtonFallingEdge()
    {
        LOGGER.info("[UI] Button clicked!");
    }
    private void onButtonRisingEdge()
    {
        LOGGER.info("[UI] Button released!");
    }
    private void onButtonHeld()
    {
        LOGGER.info("[UI] Button held down!");
    }
}
```

``` 
Console: 
[UI] Button clicked!
[UI] Button held down!
[UI] Button held down!
[UI] Button held down!
[UI] Button released!
``` 

---
### Customisation
#### Colors
- `setIdleColor(color)` Sets the color used to render the background when the button is not pressed and the mouse is not hovering over it.
- `setHoverColor(color)` Sets the color used to render the background when the button is not pressed and the mouse is hovering over it.
- `setPressedColor(color)` Sets the color used to render the background when the button is pressed.

#### Behavior
- `setClickable(boolean)` Enables/Disables the clickability of the button.
- `setTriggerButton(button)` Specifies for which mouse button it will sense mouse presses.
