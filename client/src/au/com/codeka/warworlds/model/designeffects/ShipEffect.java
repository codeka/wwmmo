package au.com.codeka.warworlds.model.designeffects;

import org.w3c.dom.Element;

import au.com.codeka.common.design.Design;

public class ShipEffect extends Design.Effect {
    public ShipEffect(Element effectElement) {
        super(effectElement.getAttribute("kind"));
    }
}
